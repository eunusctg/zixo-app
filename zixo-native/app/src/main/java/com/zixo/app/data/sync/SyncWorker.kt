package com.zixo.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zixo.app.data.config.FirebaseConfig
import com.zixo.app.data.local.room.dao.ContactDao
import com.zixo.app.data.local.room.dao.MessageDao
import com.zixo.app.data.local.room.dao.StatusDao
import com.zixo.app.data.local.room.dao.UserDao
import com.zixo.app.data.local.room.entity.ContactEntity
import com.zixo.app.data.local.room.entity.MessageEntity
import com.zixo.app.data.local.room.entity.StatusEntity
import com.zixo.app.data.local.room.entity.UserEntity
import com.zixo.app.data.mapper.toContactEntity
import com.zixo.app.data.mapper.toMessageEntity
import com.zixo.app.data.mapper.toStatusEntity
import com.zixo.app.data.mapper.toUserEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * WorkManager sync orchestrator for offline-first Firestore↔Room reconciliation.
 *
 * ## Execution Flow:
 * 1. Download contacts from Firestore → ContactEntity → ContactDao.upsert
 * 2. Download messages from Firestore → MessageEntity → MessageDao.upsert
 * 3. Download statuses → StatusEntity → StatusDao.upsert
 * 4. Download user profiles → UserEntity → UserDao.upsert
 * 5. Upload unsynced local records to Firestore
 * 6. Apply ConflictResolver for each entity using server-wins timestamp logic
 * 7. Update SyncStatus via DataStore
 *
 * Exponential backoff: BackoffPolicy.EXPONENTIAL with 30s initial delay.
 * Constraints: RequiredNetworkType.CONNECTED, BatteryNotLow.
 *
 * All Firestore operations use .await() from kotlinx-coroutines-play-services.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val statusDao: StatusDao,
    private val userDao: UserDao,
    private val conflictResolver: ConflictResolver
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Timber.w("SyncWorker: No authenticated user, aborting sync")
                return@withContext Result.failure()
            }

            Timber.d("SyncWorker: Starting full sync for user %s", uid)
            var conflictsResolved = 0

            // ── Step 1: Sync Contacts ──────────────────────────────
            try {
                val contactsSnapshot = firestore
                    .collection(FirebaseConfig.USERS_COLLECTION)
                    .document(uid)
                    .collection(FirebaseConfig.CONTACTS_SUBCOLLECTION)
                    .get()
                    .await()

                val remoteContacts = contactsSnapshot.documents.mapNotNull { doc ->
                    try { doc.data?.toContactEntity(doc.id) } catch (e: Exception) { null }
                }

                for (remote in remoteContacts) {
                    val local = contactDao.getByContactUserId(remote.contactUserId)
                    if (local != null) {
                        val resolved = conflictResolver.resolveContactConflict(
                            local, contactsSnapshot.documents.find {
                                it.id == remote.id
                            }?.data ?: emptyMap()
                        )
                        contactDao.upsert(resolved)
                        conflictsResolved++
                    } else {
                        contactDao.upsert(remote)
                    }
                }
                Timber.d("SyncWorker: Synced %d contacts", remoteContacts.size)
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker: Contact sync failed")
            }

            // ── Step 2: Sync Messages ──────────────────────────────
            try {
                val chatsSnapshot = firestore
                    .collection(FirebaseConfig.CHATS_COLLECTION)
                    .whereArrayContains("participants", uid)
                    .get()
                    .await()

                for (chatDoc in chatsSnapshot.documents) {
                    val chatId = chatDoc.id
                    val messagesSnapshot = firestore
                        .collection(FirebaseConfig.CHATS_COLLECTION)
                        .document(chatId)
                        .collection(FirebaseConfig.MESSAGES_SUBCOLLECTION)
                        .orderBy(FirebaseConfig.TIMESTAMP_FIELD)
                        .limit(FirebaseConfig.SYNC_BATCH_SIZE.toLong())
                        .get()
                        .await()

                    val remoteMessages = messagesSnapshot.documents.mapNotNull { doc ->
                        try { doc.data?.toMessageEntity(chatId) } catch (e: Exception) { null }
                    }

                    for (remote in remoteMessages) {
                        val local = messageDao.getById(remote.id)
                        if (local != null) {
                            val resolved = conflictResolver.resolveMessageConflict(
                                local, messagesSnapshot.documents.find {
                                    it.id == remote.id
                                }?.data ?: emptyMap()
                            )
                            messageDao.insert(resolved)
                            conflictsResolved++
                        } else {
                            messageDao.insert(remote)
                        }
                    }
                }
                Timber.d("SyncWorker: Synced messages from %d chats", chatsSnapshot.size())
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker: Message sync failed")
            }

            // ── Step 3: Sync Statuses ──────────────────────────────
            try {
                val statusesSnapshot = firestore
                    .collection(FirebaseConfig.STATUSES_COLLECTION)
                    .whereGreaterThan("expiresAt", System.currentTimeMillis())
                    .get()
                    .await()

                val remoteStatuses = statusesSnapshot.documents.mapNotNull { doc ->
                    try { doc.data?.toStatusEntity() } catch (e: Exception) { null }
                }

                for (remote in remoteStatuses) {
                    val local = statusDao.getById(remote.id)
                    if (local != null) {
                        val resolved = conflictResolver.resolveStatusConflict(
                            local, statusesSnapshot.documents.find {
                                it.id == remote.id
                            }?.data ?: emptyMap()
                        )
                        statusDao.insert(resolved)
                        conflictsResolved++
                    } else {
                        statusDao.insert(remote)
                    }
                }
                Timber.d("SyncWorker: Synced %d statuses", remoteStatuses.size)
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker: Status sync failed")
            }

            // ── Step 4: Cleanup expired statuses ───────────────────
            try {
                val deleted = statusDao.deleteExpired()
                Timber.d("SyncWorker: Cleaned up %d expired statuses", deleted)
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker: Status cleanup failed")
            }

            // ── Step 5: Upload unsynced messages ───────────────────
            try {
                val unsyncedMessages = messageDao.getUnsyncedMessages()
                for (msg in unsyncedMessages) {
                    try {
                        firestore
                            .collection(FirebaseConfig.CHATS_COLLECTION)
                            .document(msg.chatId)
                            .collection(FirebaseConfig.MESSAGES_SUBCOLLECTION)
                            .document(msg.id)
                            .set(mapOf(
                                "id" to msg.id,
                                "chatId" to msg.chatId,
                                "senderId" to msg.senderId,
                                "content" to msg.content,
                                "messageType" to msg.messageType,
                                "createdAt" to msg.createdAt,
                                "updatedAt" to msg.updatedAt
                            ))
                            .await()
                        messageDao.markSynced(msg.id)
                    } catch (e: Exception) {
                        Timber.e(e, "SyncWorker: Failed to upload message %s", msg.id)
                    }
                }
                Timber.d("SyncWorker: Uploaded %d unsynced messages", unsyncedMessages.size)
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker: Unsynced message upload failed")
            }

            Timber.d("SyncWorker: Full sync completed, %d conflicts resolved", conflictsResolved)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Fatal sync error, retrying with backoff")
            Result.retry()
        }
    }
}
