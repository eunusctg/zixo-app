package com.zixo.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.MyStatusState
import com.zixo.app.domain.model.StatusContentType
import com.zixo.app.domain.model.StatusGroupModel
import com.zixo.app.domain.model.StatusModel
import com.zixo.app.domain.model.StatusReaction
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [StatusRepository].
 *
 * Manages ephemeral status updates using Firestore for storage and
 * Firebase Storage for media uploads. Enforces zero-trust delivery:
 * statuses are ONLY delivered to verified mutual contacts.
 *
 * Auto-cleanup: statuses older than 24 hours are filtered out on
 * every emission. Uses continuous snapshot listeners for real-time
 * sync via [callbackFlow].
 */
@Singleton
class StatusRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth,
    private val contactRepository: ContactRepository
) : StatusRepository {

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    private val statusesCollection get() = firestore.collection("statuses")
    private val usersCollection get() = firestore.collection("users")

    private val twentyFourHoursMs: Long = 24 * 60 * 60 * 1000L

    private data class RunnableStatusData(
        val statuses: List<StatusModel>,
        val myUid: String
    )

    // ── Observe Status Feed ───────────────────────────────────────────────────

    override fun observeStatusFeed(): Flow<List<StatusGroupModel>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = statusesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing status feed")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val now = System.currentTimeMillis()
                    val cutoff = now - twentyFourHoursMs

                    val allStatuses = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            mapToStatusModel(doc)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to map status document: %s", doc.id)
                            null
                        }
                    }?.filter { status ->
                        status.senderUid != myUid &&
                            status.createdAt >= cutoff &&
                            !status.isExpired
                    } ?: emptyList()

                    trySend(RunnableStatusData(allStatuses, myUid))
                } catch (e: Exception) {
                    Timber.e(e, "Error processing status feed")
                    trySend(null)
                }
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO).map { runnableData ->
        if (runnableData == null) return@map emptyList<StatusGroupModel>()

        val mutualContacts = contactRepository.observeContacts().first()
            .filter { it.isMutual && !it.isBlocked }

        val mutualUids = mutualContacts.map { it.contactUserId }.toSet()

        val visibleStatuses = runnableData.statuses.filter { status ->
            mutualUids.contains(status.senderUid) ||
                status.viewedByUids.contains(runnableData.myUid)
        }

        groupStatusesBySender(visibleStatuses, mutualContacts)
    }.flowOn(Dispatchers.IO)

    // ── Observe My Statuses ───────────────────────────────────────────────────

    override fun observeMyStatuses(): Flow<MyStatusState> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(MyStatusState())
            close()
            return@callbackFlow
        }

        val subscription = statusesCollection
            .whereEqualTo("senderUid", myUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing my statuses")
                    trySend(MyStatusState(errorMessage = error.localizedMessage))
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val cutoff = now - twentyFourHoursMs

                val myStatuses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToStatusModel(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map my status document: %s", doc.id)
                        null
                    }
                }?.filter { it.createdAt >= cutoff && !it.isExpired }
                    ?: emptyList()

                trySend(MyStatusState(myStatuses = myStatuses))
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Post Text Status ──────────────────────────────────────────────────────

    override suspend fun postTextStatus(text: String, backgroundColor: String?) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val myProfile = usersCollection.document(myUid).get().await()
                val now = System.currentTimeMillis()
                val statusId = UUID.randomUUID().toString()

                val statusData = hashMapOf(
                    "id" to statusId,
                    "senderUid" to myUid,
                    "senderDisplayName" to (myProfile.getString("displayName") ?: ""),
                    "senderAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                    "senderZixoNumber" to (myProfile.getString("zixoNumber") ?: ""),
                    "type" to StatusContentType.TEXT.name,
                    "textContent" to text,
                    "mediaUrl" to null,
                    "mediaMimeType" to null,
                    "mediaThumbnailUrl" to null,
                    "backgroundColor" to backgroundColor,
                    "fontName" to null,
                    "shapeType" to null,
                    "emoji3dCode" to null,
                    "caption" to null,
                    "createdAt" to now,
                    "expiresAt" to now + twentyFourHoursMs,
                    "viewedByUids" to emptyList<String>(),
                    "reactions" to emptyList<Map<String, Any>>(),
                    "isExpired" to false
                )

                statusesCollection.document(statusId).set(statusData).await()
                Timber.d("Text status posted: %s", statusId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to post text status")
        }
    }

    // ── Post Image Status ─────────────────────────────────────────────────────

    override suspend fun postImageStatus(localFilePath: String, caption: String?) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val myProfile = usersCollection.document(myUid).get().await()
                val now = System.currentTimeMillis()
                val statusId = UUID.randomUUID().toString()

                val storagePath = "statuses/$myUid/$statusId/image.jpg"
                val fileUri = Uri.parse(localFilePath)
                val storageRef = storage.reference.child(storagePath)
                storageRef.putFile(fileUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val statusData = hashMapOf(
                    "id" to statusId,
                    "senderUid" to myUid,
                    "senderDisplayName" to (myProfile.getString("displayName") ?: ""),
                    "senderAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                    "senderZixoNumber" to (myProfile.getString("zixoNumber") ?: ""),
                    "type" to StatusContentType.IMAGE.name,
                    "textContent" to null,
                    "mediaUrl" to downloadUrl,
                    "mediaMimeType" to "image/jpeg",
                    "mediaThumbnailUrl" to null,
                    "backgroundColor" to null,
                    "fontName" to null,
                    "shapeType" to null,
                    "emoji3dCode" to null,
                    "caption" to caption,
                    "createdAt" to now,
                    "expiresAt" to now + twentyFourHoursMs,
                    "viewedByUids" to emptyList<String>(),
                    "reactions" to emptyList<Map<String, Any>>(),
                    "isExpired" to false
                )

                statusesCollection.document(statusId).set(statusData).await()
                Timber.d("Image status posted: %s", statusId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to post image status")
        }
    }

    // ── Post Video Status ─────────────────────────────────────────────────────

    override suspend fun postVideoStatus(localFilePath: String, caption: String?) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val myProfile = usersCollection.document(myUid).get().await()
                val now = System.currentTimeMillis()
                val statusId = UUID.randomUUID().toString()

                val storagePath = "statuses/$myUid/$statusId/video.mp4"
                val fileUri = Uri.parse(localFilePath)
                val storageRef = storage.reference.child(storagePath)
                storageRef.putFile(fileUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val statusData = hashMapOf(
                    "id" to statusId,
                    "senderUid" to myUid,
                    "senderDisplayName" to (myProfile.getString("displayName") ?: ""),
                    "senderAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                    "senderZixoNumber" to (myProfile.getString("zixoNumber") ?: ""),
                    "type" to StatusContentType.VIDEO.name,
                    "textContent" to null,
                    "mediaUrl" to downloadUrl,
                    "mediaMimeType" to "video/mp4",
                    "mediaThumbnailUrl" to null,
                    "backgroundColor" to null,
                    "fontName" to null,
                    "shapeType" to null,
                    "emoji3dCode" to null,
                    "caption" to caption,
                    "createdAt" to now,
                    "expiresAt" to now + twentyFourHoursMs,
                    "viewedByUids" to emptyList<String>(),
                    "reactions" to emptyList<Map<String, Any>>(),
                    "isExpired" to false
                )

                statusesCollection.document(statusId).set(statusData).await()
                Timber.d("Video status posted: %s", statusId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to post video status")
        }
    }

    // ── View Status ───────────────────────────────────────────────────────────

    override suspend fun viewStatus(statusId: String) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val doc = statusesCollection.document(statusId).get().await()
                if (!doc.exists()) return@withContext

                @Suppress("UNCHECKED_CAST")
                val viewedByUids = (doc.get("viewedByUids") as? List<String>)
                    ?: emptyList()

                if (!viewedByUids.contains(myUid)) {
                    val updatedUids = viewedByUids + myUid
                    statusesCollection.document(statusId)
                        .update("viewedByUids", updatedUids)
                        .await()
                    Timber.d("Status viewed: %s", statusId)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to view status: %s", statusId)
        }
    }

    // ── Add Status Reaction ───────────────────────────────────────────────────

    override suspend fun addStatusReaction(statusId: String, emoji: String) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val doc = statusesCollection.document(statusId).get().await()
                if (!doc.exists()) return@withContext

                val senderUid = doc.getString("senderUid") ?: return@withContext
                if (senderUid != myUid) {
                    val gate = contactRepository.checkCommunicationGate(senderUid)
                    if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                        Timber.w("Cannot react to status: not a mutual contact")
                        return@withContext
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val existingReactions = (doc.get("reactions") as? List<Map<String, Any>>)
                    ?: emptyList()

                val myProfile = usersCollection.document(myUid).get().await()
                val newReaction = mapOf(
                    "uid" to myUid,
                    "displayName" to (myProfile.getString("displayName") ?: ""),
                    "emoji" to emoji,
                    "timestamp" to System.currentTimeMillis()
                )

                val updatedReactions = existingReactions + newReaction
                statusesCollection.document(statusId)
                    .update("reactions", updatedReactions)
                    .await()

                Timber.d("Status reaction added: %s to %s", emoji, statusId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add status reaction")
        }
    }

    // ── Delete Status ─────────────────────────────────────────────────────────

    override suspend fun deleteStatus(statusId: String) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val doc = statusesCollection.document(statusId).get().await()
                if (!doc.exists()) return@withContext

                val senderUid = doc.getString("senderUid") ?: return@withContext
                if (senderUid != myUid) {
                    Timber.w("Cannot delete status owned by another user")
                    return@withContext
                }

                statusesCollection.document(statusId).delete().await()
                Timber.d("Status deleted: %s", statusId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete status: %s", statusId)
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private fun groupStatusesBySender(
        statuses: List<StatusModel>,
        mutualContacts: List<ContactModel>
    ): List<StatusGroupModel> {
        val contactMap = mutualContacts.associateBy { it.contactUserId }

        return statuses
            .groupBy { it.senderUid }
            .map { (senderUid, senderStatuses) ->
                val contact = contactMap[senderUid]
                val latestTimestamp = senderStatuses.maxOfOrNull { it.createdAt } ?: 0L
                val hasUnviewed = senderStatuses.any { status ->
                    !status.isExpired
                }

                StatusGroupModel(
                    senderUid = senderUid,
                    senderDisplayName = contact?.contactDisplayName
                        ?: senderStatuses.firstOrNull()?.senderDisplayName ?: "",
                    senderAvatarUrl = contact?.contactAvatarUrl
                        ?: senderStatuses.firstOrNull()?.senderAvatarUrl ?: "",
                    senderZixoNumber = contact?.contactZixoNumber
                        ?: senderStatuses.firstOrNull()?.senderZixoNumber ?: "",
                    statuses = senderStatuses.sortedByDescending { it.createdAt },
                    hasUnviewedStatuses = hasUnviewed,
                    latestTimestamp = latestTimestamp
                )
            }
            .sortedByDescending { it.latestTimestamp }
    }

    private fun mapToStatusModel(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): StatusModel {
        val typeStr = doc.getString("type") ?: "TEXT"
        val contentType = try {
            StatusContentType.valueOf(typeStr)
        } catch (_: Exception) {
            StatusContentType.TEXT
        }

        @Suppress("UNCHECKED_CAST")
        val viewedByUids = (doc.get("viewedByUids") as? List<String>)
            ?.toSet() ?: emptySet()

        @Suppress("UNCHECKED_CAST")
        val reactionsRaw = doc.get("reactions") as? List<Map<String, Any>>
            ?: emptyList()

        val reactions = reactionsRaw.map { data ->
            StatusReaction(
                uid = data["uid"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                emoji = data["emoji"] as? String ?: "",
                timestamp = data["timestamp"] as? Long ?: 0L
            )
        }

        return StatusModel(
            id = doc.id,
            senderUid = doc.getString("senderUid") ?: "",
            senderDisplayName = doc.getString("senderDisplayName") ?: "",
            senderAvatarUrl = doc.getString("senderAvatarUrl") ?: "",
            senderZixoNumber = doc.getString("senderZixoNumber") ?: "",
            type = contentType,
            textContent = doc.getString("textContent"),
            mediaUrl = doc.getString("mediaUrl"),
            mediaMimeType = doc.getString("mediaMimeType"),
            mediaThumbnailUrl = doc.getString("mediaThumbnailUrl"),
            backgroundColor = doc.getString("backgroundColor"),
            fontName = doc.getString("fontName"),
            shapeType = doc.getString("shapeType")?.let {
                try { com.zixo.app.domain.model.StatusShapeType.valueOf(it) } catch (_: Exception) { null }
            },
            emoji3dCode = doc.getString("emoji3dCode"),
            caption = doc.getString("caption"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            expiresAt = doc.getLong("expiresAt") ?: 0L,
            viewedByUids = viewedByUids,
            reactions = reactions,
            isExpired = doc.getBoolean("isExpired") ?: false
        )
    }
}
