package com.zixo.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.LastMessageInfo
import com.zixo.app.domain.model.MessageContentType
import com.zixo.app.domain.model.MessageModel
import com.zixo.app.domain.model.MessageReaction
import com.zixo.app.domain.model.MessageActionResult
import com.zixo.app.domain.model.ParticipantRole
import com.zixo.app.domain.model.ThreadParticipant
import com.zixo.app.domain.model.ThreadType
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [ChatRepository].
 *
 * Manages chat threads and messages using Firestore with continuous
 * snapshot listeners for real-time sync. Uses Firebase Realtime Database
 * for presence and typing indicators, and Firebase Storage for media uploads.
 *
 * Zero-trust enforcement: All send operations verify mutual contact status
 * through [ContactRepository.checkCommunicationGate] before writing.
 * Non-contact messages are blocked at this boundary.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth,
    private val contactRepository: ContactRepository
) : ChatRepository {

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    private val threadsCollection get() = firestore.collection("threads")

    private fun messagesCollection(threadId: String) =
        threadsCollection.document(threadId).collection("messages")

    private fun presenceRef(uid: String) =
        realtimeDb.getReference("presence").child(uid)

    private fun typingRef(threadId: String, uid: String) =
        realtimeDb.getReference("typing").child(threadId).child(uid)

    // ── Observe Threads ───────────────────────────────────────────────────────

    override fun observeThreads(): Flow<List<ChatThreadModel>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = threadsCollection
            .whereArrayContains("participantUids", myUid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing threads")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val threads = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToChatThreadModel(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map thread document: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(threads)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    override fun observeThread(threadId: String): Flow<ChatThreadModel?> = callbackFlow {
        val subscription = threadsCollection.document(threadId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing thread: %s", threadId)
                    trySend(null)
                    return@addSnapshotListener
                }

                trySend(
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            mapToChatThreadModel(snapshot)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to map thread: %s", threadId)
                            null
                        }
                    } else {
                        null
                    }
                )
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Observe Messages ──────────────────────────────────────────────────────

    override fun observeMessages(threadId: String): Flow<List<MessageModel>> = callbackFlow {
        val subscription = messagesCollection(threadId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing messages in thread: %s", threadId)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToMessageModel(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map message document: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Send Text Message ─────────────────────────────────────────────────────

    override fun sendTextMessage(
        threadId: String,
        content: String,
        replyToMessageId: String?
    ): Flow<Result<MessageModel>> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(Result.failure(IllegalStateException("Not authenticated")))
            return@flow
        }

        try {
            val threadDoc = threadsCollection.document(threadId).get().await()
            if (!threadDoc.exists()) {
                emit(Result.failure(IllegalStateException("Thread not found")))
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>)
                ?: emptyList()

            for (uid in participantUids) {
                if (uid == myUid) continue
                val gate = contactRepository.checkCommunicationGate(uid)
                if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                    emit(Result.failure(
                        SecurityException("Communication blocked for participant: $uid")
                    ))
                    return@flow
                }
            }

            val myProfile = firestore.collection("users").document(myUid).get().await()
            val now = System.currentTimeMillis()
            val messageId = UUID.randomUUID().toString()

            val messageData = hashMapOf(
                "id" to messageId,
                "threadId" to threadId,
                "senderUid" to myUid,
                "senderDisplayName" to (myProfile.getString("displayName") ?: ""),
                "senderAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "content" to content,
                "timestamp" to now,
                "type" to MessageContentType.TEXT.name,
                "mediaUrl" to null,
                "mediaThumbnailUrl" to null,
                "mediaFileSize" to 0L,
                "mediaMimeType" to "",
                "isRead" to false,
                "readByUids" to listOf(myUid),
                "deliveredToUids" to listOf(myUid),
                "replyToMessageId" to replyToMessageId,
                "replyToPreview" to null,
                "replyToSenderName" to null,
                "forwardedFromUid" to null,
                "forwardedFromName" to null,
                "isForwarded" to false,
                "reactions" to emptyList<Map<String, Any>>(),
                "isDeletedForMe" to false,
                "isDeletedForEveryone" to false,
                "isEdited" to false,
                "editedAt" to null,
                "ephemeralExpiresAt" to null,
                "caption" to null
            )

            val batch = firestore.batch()
            batch.set(messagesCollection(threadId).document(messageId), messageData)

            val lastMessage = LastMessageInfo(
                senderUid = myUid,
                senderDisplayName = myProfile.getString("displayName") ?: "",
                content = content,
                type = MessageContentType.TEXT,
                timestamp = now,
                isRead = false
            )
            batch.update(
                threadsCollection.document(threadId),
                mapOf(
                    "lastMessage" to lastMessage.toFirestoreMap(),
                    "lastMessageTimestamp" to now
                )
            )
            batch.commit().await()

            val message = MessageModel(
                id = messageId,
                threadId = threadId,
                senderUid = myUid,
                senderDisplayName = myProfile.getString("displayName") ?: "",
                senderAvatarUrl = myProfile.getString("photoUrl") ?: "",
                content = content,
                timestamp = now,
                type = MessageContentType.TEXT,
                replyToMessageId = replyToMessageId
            )

            emit(Result.success(message))
        } catch (e: Exception) {
            Timber.e(e, "Failed to send text message")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Send Media Message ────────────────────────────────────────────────────

    override fun sendMediaMessage(
        threadId: String,
        localFilePath: String,
        mimeType: String,
        caption: String?,
        replyToMessageId: String?
    ): Flow<Result<MessageModel>> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(Result.failure(IllegalStateException("Not authenticated")))
            return@flow
        }

        try {
            val threadDoc = threadsCollection.document(threadId).get().await()
            if (!threadDoc.exists()) {
                emit(Result.failure(IllegalStateException("Thread not found")))
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>)
                ?: emptyList()

            for (uid in participantUids) {
                if (uid == myUid) continue
                val gate = contactRepository.checkCommunicationGate(uid)
                if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                    emit(Result.failure(
                        SecurityException("Communication blocked for participant: $uid")
                    ))
                    return@flow
                }
            }

            val contentType = when {
                mimeType.startsWith("image/") -> MessageContentType.IMAGE
                mimeType.startsWith("video/") -> MessageContentType.VIDEO
                mimeType.startsWith("audio/voice") || mimeType.startsWith("audio/ogg") ->
                    MessageContentType.AUDIO_VOICE
                mimeType.startsWith("audio/") -> MessageContentType.AUDIO_FILE
                else -> MessageContentType.FILE
            }

            val messageId = UUID.randomUUID().toString()
            val storagePath = "chat_media/$threadId/$messageId/${UUID.randomUUID()}"
            val fileUri = Uri.parse(localFilePath)

            val storageRef = storage.reference.child(storagePath)
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val myProfile = firestore.collection("users").document(myUid).get().await()
            val now = System.currentTimeMillis()

            val messageData = hashMapOf(
                "id" to messageId,
                "threadId" to threadId,
                "senderUid" to myUid,
                "senderDisplayName" to (myProfile.getString("displayName") ?: ""),
                "senderAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "content" to "",
                "timestamp" to now,
                "type" to contentType.name,
                "mediaUrl" to downloadUrl,
                "mediaThumbnailUrl" to null,
                "mediaFileSize" to 0L,
                "mediaMimeType" to mimeType,
                "isRead" to false,
                "readByUids" to listOf(myUid),
                "deliveredToUids" to listOf(myUid),
                "replyToMessageId" to replyToMessageId,
                "replyToPreview" to null,
                "replyToSenderName" to null,
                "forwardedFromUid" to null,
                "forwardedFromName" to null,
                "isForwarded" to false,
                "reactions" to emptyList<Map<String, Any>>(),
                "isDeletedForMe" to false,
                "isDeletedForEveryone" to false,
                "isEdited" to false,
                "editedAt" to null,
                "ephemeralExpiresAt" to null,
                "caption" to caption
            )

            val batch = firestore.batch()
            batch.set(messagesCollection(threadId).document(messageId), messageData)

            val contentPreview = caption ?: mimeType
            val lastMessage = LastMessageInfo(
                senderUid = myUid,
                senderDisplayName = myProfile.getString("displayName") ?: "",
                content = contentPreview,
                type = contentType,
                timestamp = now,
                isRead = false
            )
            batch.update(
                threadsCollection.document(threadId),
                mapOf(
                    "lastMessage" to lastMessage.toFirestoreMap(),
                    "lastMessageTimestamp" to now
                )
            )
            batch.commit().await()

            val message = MessageModel(
                id = messageId,
                threadId = threadId,
                senderUid = myUid,
                senderDisplayName = myProfile.getString("displayName") ?: "",
                senderAvatarUrl = myProfile.getString("photoUrl") ?: "",
                timestamp = now,
                type = contentType,
                mediaUrl = downloadUrl,
                mediaMimeType = mimeType,
                caption = caption,
                replyToMessageId = replyToMessageId
            )

            emit(Result.success(message))
        } catch (e: Exception) {
            Timber.e(e, "Failed to send media message")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Forward Message ───────────────────────────────────────────────────────

    override fun forwardMessage(
        messageId: String,
        targetThreadIds: List<String>
    ): Flow<Result<Unit>> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(Result.failure(IllegalStateException("Not authenticated")))
            return@flow
        }

        try {
            var sourceMessage: MessageModel? = null
            for (threadId in threadsCollection.get().await().documents) {
                val msgDoc = messagesCollection(threadId.id).document(messageId).get().await()
                if (msgDoc.exists()) {
                    sourceMessage = mapToMessageModel(msgDoc)
                    break
                }
            }

            if (sourceMessage == null) {
                emit(Result.failure(IllegalStateException("Source message not found")))
                return@flow
            }

            for (targetThreadId in targetThreadIds) {
                val threadDoc = threadsCollection.document(targetThreadId).get().await()
                if (!threadDoc.exists()) continue

                @Suppress("UNCHECKED_CAST")
                val participantUids = (threadDoc.get("participantUids") as? List<String>)
                    ?: emptyList()

                var blocked = false
                for (uid in participantUids) {
                    if (uid == myUid) continue
                    val gate = contactRepository.checkCommunicationGate(uid)
                    if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                        blocked = true
                        break
                    }
                }
                if (blocked) continue

                val myProfile = firestore.collection("users").document(myUid).get().await()
                val now = System.currentTimeMillis()
                val newMessageId = UUID.randomUUID().toString()

                val messageData = hashMapOf(
                    "id" to newMessageId,
                    "threadId" to targetThreadId,
                    "senderUid" to myUid,
                    "senderDisplayName" to (myProfile.getString("displayName") ?: ""),
                    "senderAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                    "content" to sourceMessage.content,
                    "timestamp" to now,
                    "type" to sourceMessage.type.name,
                    "mediaUrl" to sourceMessage.mediaUrl,
                    "forwardedFromUid" to sourceMessage.senderUid,
                    "forwardedFromName" to sourceMessage.senderDisplayName,
                    "isForwarded" to true,
                    "isRead" to false,
                    "readByUids" to listOf(myUid),
                    "deliveredToUids" to listOf(myUid),
                    "reactions" to emptyList<Map<String, Any>>(),
                    "isDeletedForMe" to false,
                    "isDeletedForEveryone" to false,
                    "isEdited" to false,
                    "caption" to sourceMessage.caption
                )

                messagesCollection(targetThreadId).document(newMessageId)
                    .set(messageData).await()
            }

            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to forward message")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Delete Message ────────────────────────────────────────────────────────

    override suspend fun deleteMessageForMe(threadId: String, messageId: String) {
        try {
            val myUid = currentUid ?: return
            messagesCollection(threadId).document(messageId)
                .update("isDeletedForMe", true, "deletedForMeUid", myUid)
                .await()
            Timber.d("Message deleted for me: %s", messageId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete message for me: %s", messageId)
        }
    }

    override suspend fun deleteMessageForEveryone(threadId: String, messageId: String) {
        try {
            messagesCollection(threadId).document(messageId)
                .update(
                    mapOf(
                        "content" to "This message was deleted",
                        "mediaUrl" to null,
                        "mediaThumbnailUrl" to null,
                        "caption" to null,
                        "isDeletedForEveryone" to true,
                        "type" to MessageContentType.DELETED_PLACEHOLDER.name
                    )
                ).await()
            Timber.d("Message deleted for everyone: %s", messageId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete message for everyone: %s", messageId)
        }
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    override suspend fun addReaction(
        threadId: String,
        messageId: String,
        emoji: String,
        isThreeD: Boolean
    ) {
        try {
            val myUid = currentUid ?: return
            val messageDoc = messagesCollection(threadId).document(messageId).get().await()
            if (!messageDoc.exists()) return

            @Suppress("UNCHECKED_CAST")
            val existingReactions = (messageDoc.get("reactions") as? List<Map<String, Any>>)
                ?: emptyList()

            val filtered = existingReactions.filterNot { it["uid"] == myUid }
            val newReaction = mapOf(
                "uid" to myUid,
                "emoji" to emoji,
                "customStickerId" to null,
                "timestamp" to System.currentTimeMillis(),
                "isThreeD" to isThreeD
            )

            val updatedReactions = filtered + newReaction
            messagesCollection(threadId).document(messageId)
                .update("reactions", updatedReactions)
                .await()

            Timber.d("Reaction added: %s to message: %s", emoji, messageId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add reaction")
        }
    }

    // ── Mark as Read ──────────────────────────────────────────────────────────

    override suspend fun markThreadAsRead(threadId: String) {
        try {
            val myUid = currentUid ?: return
            threadsCollection.document(threadId)
                .update("unreadCount.$myUid", 0)
                .await()
            Timber.d("Thread marked as read: %s", threadId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark thread as read: %s", threadId)
        }
    }

    // ── Create Thread ─────────────────────────────────────────────────────────

    override fun createSingleThread(contactUid: String): Flow<Result<ChatThreadModel>> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(Result.failure(IllegalStateException("Not authenticated")))
            return@flow
        }

        try {
            val gate = contactRepository.checkCommunicationGate(contactUid)
            if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                emit(Result.failure(
                    SecurityException("Cannot create thread: not a mutual contact")
                ))
                return@flow
            }

            val existingThread = threadsCollection
                .whereArrayContains("participantUids", myUid)
                .get().await()

            for (doc in existingThread.documents) {
                @Suppress("UNCHECKED_CAST")
                val uids = (doc.get("participantUids") as? List<String>) ?: emptyList()
                val typeStr = doc.getString("type") ?: ""
                if (uids.contains(contactUid) && typeStr == ThreadType.SINGLE.name) {
                    val thread = mapToChatThreadModel(doc)
                    emit(Result.success(thread))
                    return@flow
                }
            }

            val myProfile = firestore.collection("users").document(myUid).get().await()
            val contactProfile = firestore.collection("users")
                .document(contactUid).get().await()
            val now = System.currentTimeMillis()
            val threadId = UUID.randomUUID().toString()

            val myParticipant = ThreadParticipant(
                uid = myUid,
                displayName = myProfile.getString("displayName") ?: "",
                avatarUrl = myProfile.getString("photoUrl") ?: "",
                zixoNumber = myProfile.getString("zixoNumber") ?: "",
                role = ParticipantRole.MEMBER,
                joinedAt = now
            )

            val contactParticipant = ThreadParticipant(
                uid = contactUid,
                displayName = contactProfile.getString("displayName") ?: "",
                avatarUrl = contactProfile.getString("photoUrl") ?: "",
                zixoNumber = contactProfile.getString("zixoNumber") ?: "",
                role = ParticipantRole.MEMBER,
                joinedAt = now
            )

            val threadData = hashMapOf(
                "id" to threadId,
                "type" to ThreadType.SINGLE.name,
                "participantUids" to listOf(myUid, contactUid),
                "participantProfiles" to mapOf(
                    myUid to myParticipant.toFirestoreMap(),
                    contactUid to contactParticipant.toFirestoreMap()
                ),
                "groupName" to null,
                "groupAvatarUrl" to null,
                "groupDescription" to null,
                "groupAdminUids" to emptyList<String>(),
                "createdByUid" to myUid,
                "createdAt" to now,
                "lastMessage" to null,
                "lastMessageTimestamp" to now,
                "unreadCount" to mapOf(myUid to 0, contactUid to 0),
                "isPinned" to false,
                "isMuted" to false,
                "isArchived" to false,
                "ephemeralTimerSeconds" to 0,
                "wallpaperUrl" to null
            )

            threadsCollection.document(threadId).set(threadData).await()

            val thread = ChatThreadModel(
                id = threadId,
                type = ThreadType.SINGLE,
                participantUids = setOf(myUid, contactUid),
                participantProfiles = mapOf(
                    myUid to myParticipant,
                    contactUid to contactParticipant
                ),
                createdByUid = myUid,
                createdAt = now
            )

            emit(Result.success(thread))
        } catch (e: Exception) {
            Timber.e(e, "Failed to create single thread")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun createGroupThread(
        groupName: String,
        participantUids: List<String>
    ): Flow<Result<ChatThreadModel>> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(Result.failure(IllegalStateException("Not authenticated")))
            return@flow
        }

        try {
            val verifiedUids = mutableListOf(myUid)
            for (uid in participantUids) {
                if (uid == myUid) continue
                val gate = contactRepository.checkCommunicationGate(uid)
                if (gate is CommunicationGate.Allowed) {
                    verifiedUids.add(uid)
                }
            }

            val now = System.currentTimeMillis()
            val threadId = UUID.randomUUID().toString()

            val participantProfiles = mutableMapOf<String, Map<String, Any?>>()
            val domainParticipants = mutableMapOf<String, ThreadParticipant>()

            for (uid in verifiedUids) {
                val profile = firestore.collection("users").document(uid).get().await()
                val participant = ThreadParticipant(
                    uid = uid,
                    displayName = profile.getString("displayName") ?: "",
                    avatarUrl = profile.getString("photoUrl") ?: "",
                    zixoNumber = profile.getString("zixoNumber") ?: "",
                    role = if (uid == myUid) ParticipantRole.ADMIN else ParticipantRole.MEMBER,
                    joinedAt = now
                )
                participantProfiles[uid] = participant.toFirestoreMap()
                domainParticipants[uid] = participant
            }

            val threadData = hashMapOf(
                "id" to threadId,
                "type" to ThreadType.GROUP.name,
                "participantUids" to verifiedUids,
                "participantProfiles" to participantProfiles,
                "groupName" to groupName,
                "groupAvatarUrl" to null,
                "groupDescription" to null,
                "groupAdminUids" to listOf(myUid),
                "createdByUid" to myUid,
                "createdAt" to now,
                "lastMessage" to null,
                "lastMessageTimestamp" to now,
                "unreadCount" to verifiedUids.associateWith { 0 },
                "isPinned" to false,
                "isMuted" to false,
                "isArchived" to false,
                "ephemeralTimerSeconds" to 0,
                "wallpaperUrl" to null
            )

            threadsCollection.document(threadId).set(threadData).await()

            val thread = ChatThreadModel(
                id = threadId,
                type = ThreadType.GROUP,
                participantUids = verifiedUids.toSet(),
                participantProfiles = domainParticipants,
                groupName = groupName,
                groupAdminUids = setOf(myUid),
                createdByUid = myUid,
                createdAt = now
            )

            emit(Result.success(thread))
        } catch (e: Exception) {
            Timber.e(e, "Failed to create group thread")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Thread Settings ───────────────────────────────────────────────────────

    override suspend fun setThreadPinned(threadId: String, isPinned: Boolean) {
        try {
            threadsCollection.document(threadId).update("isPinned", isPinned).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to set thread pinned: %s", threadId)
        }
    }

    override suspend fun setThreadMuted(threadId: String, isMuted: Boolean) {
        try {
            threadsCollection.document(threadId).update("isMuted", isMuted).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to set thread muted: %s", threadId)
        }
    }

    override suspend fun setThreadArchived(threadId: String, isArchived: Boolean) {
        try {
            threadsCollection.document(threadId).update("isArchived", isArchived).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to set thread archived: %s", threadId)
        }
    }

    override suspend fun setThreadEphemeralTimer(threadId: String, timerSeconds: Int) {
        try {
            threadsCollection.document(threadId)
                .update("ephemeralTimerSeconds", timerSeconds).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to set ephemeral timer: %s", threadId)
        }
    }

    // ── Action Result Handler ─────────────────────────────────────────────────

    override suspend fun handleActionResult(action: MessageActionResult) {
        try {
            when (action) {
                is MessageActionResult.DeleteForMe -> {
                    val threadId = findThreadForMessage(action.messageId) ?: return
                    deleteMessageForMe(threadId, action.messageId)
                }
                is MessageActionResult.DeleteForEveryone -> {
                    val threadId = findThreadForMessage(action.messageId) ?: return
                    deleteMessageForEveryone(threadId, action.messageId)
                }
                is MessageActionResult.React -> {
                    val threadId = findThreadForMessage(action.messageId) ?: return
                    addReaction(threadId, action.messageId, action.emoji, action.isThreeD)
                }
                is MessageActionResult.Forward ->
                    forwardMessage(action.messageId, action.targetThreadIds)
                else -> Timber.d("Unhandled action result: %s", action::class.simpleName)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle action result")
        }
    }

    private suspend fun findThreadForMessage(messageId: String): String? {
        return try {
            val myUid = currentUid ?: return null
            val threadSnapshot = threadsCollection
                .whereArrayContains("participantUids", myUid)
                .get().await()

            for (threadDoc in threadSnapshot.documents) {
                val msgDoc = messagesCollection(threadDoc.id)
                    .document(messageId).get().await()
                if (msgDoc.exists()) {
                    return threadDoc.id
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to find thread for message: %s", messageId)
            null
        }
    }

    // ── Mapping Helpers ───────────────────────────────────────────────────────

    private fun mapToChatThreadModel(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): ChatThreadModel {
        @Suppress("UNCHECKED_CAST")
        val participantUids = (doc.get("participantUids") as? List<String>)
            ?.toSet() ?: emptySet()

        @Suppress("UNCHECKED_CAST")
        val profilesRaw = doc.get("participantProfiles") as? Map<String, Map<String, Any>>
            ?: emptyMap()

        val participantProfiles = profilesRaw.mapValues { (_, data) ->
            ThreadParticipant(
                uid = data["uid"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                avatarUrl = data["avatarUrl"] as? String ?: "",
                zixoNumber = data["zixoNumber"] as? String ?: "",
                role = try {
                    ParticipantRole.valueOf(data["role"] as? String ?: "MEMBER")
                } catch (_: Exception) {
                    ParticipantRole.MEMBER
                },
                joinedAt = data["joinedAt"] as? Long ?: 0L,
                isOnline = data["isOnline"] as? Boolean ?: false
            )
        }

        val lastMessageRaw = doc.get("lastMessage") as? Map<String, Any>
        val lastMessage = lastMessageRaw?.let { data ->
            LastMessageInfo(
                senderUid = data["senderUid"] as? String ?: "",
                senderDisplayName = data["senderDisplayName"] as? String ?: "",
                content = data["content"] as? String ?: "",
                type = try {
                    MessageContentType.valueOf(data["type"] as? String ?: "TEXT")
                } catch (_: Exception) {
                    MessageContentType.TEXT
                },
                timestamp = data["timestamp"] as? Long ?: 0L,
                isRead = data["isRead"] as? Boolean ?: false
            )
        }

        val typeStr = doc.getString("type") ?: "SINGLE"
        val threadType = try {
            ThreadType.valueOf(typeStr)
        } catch (_: Exception) {
            ThreadType.SINGLE
        }

        @Suppress("UNCHECKED_CAST")
        val adminUids = (doc.get("groupAdminUids") as? List<String>)
            ?.toSet() ?: emptySet()

        return ChatThreadModel(
            id = doc.id,
            type = threadType,
            participantUids = participantUids,
            participantProfiles = participantProfiles,
            groupName = doc.getString("groupName"),
            groupAvatarUrl = doc.getString("groupAvatarUrl"),
            groupDescription = doc.getString("groupDescription"),
            groupAdminUids = adminUids,
            createdByUid = doc.getString("createdByUid") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L,
            lastMessage = lastMessage,
            isPinned = doc.getBoolean("isPinned") ?: false,
            isMuted = doc.getBoolean("isMuted") ?: false,
            isArchived = doc.getBoolean("isArchived") ?: false,
            ephemeralTimerSeconds = (doc.getLong("ephemeralTimerSeconds") ?: 0L).toInt(),
            wallpaperUrl = doc.getString("wallpaperUrl")
        )
    }

    private fun mapToMessageModel(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): MessageModel {
        @Suppress("UNCHECKED_CAST")
        val reactionsRaw = doc.get("reactions") as? List<Map<String, Any>>
            ?: emptyList()

        val reactions = reactionsRaw.map { data ->
            MessageReaction(
                uid = data["uid"] as? String ?: "",
                emoji = data["emoji"] as? String ?: "",
                customStickerId = data["customStickerId"] as? String,
                timestamp = data["timestamp"] as? Long ?: 0L,
                isThreeD = data["isThreeD"] as? Boolean ?: false
            )
        }

        @Suppress("UNCHECKED_CAST")
        val readByUids = (doc.get("readByUids") as? List<String>)
            ?.toSet() ?: emptySet()

        @Suppress("UNCHECKED_CAST")
        val deliveredToUids = (doc.get("deliveredToUids") as? List<String>)
            ?.toSet() ?: emptySet()

        val typeStr = doc.getString("type") ?: "TEXT"
        val contentType = try {
            MessageContentType.valueOf(typeStr)
        } catch (_: Exception) {
            MessageContentType.TEXT
        }

        return MessageModel(
            id = doc.id,
            threadId = doc.getString("threadId") ?: "",
            senderUid = doc.getString("senderUid") ?: "",
            senderDisplayName = doc.getString("senderDisplayName") ?: "",
            senderAvatarUrl = doc.getString("senderAvatarUrl") ?: "",
            content = doc.getString("content") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L,
            type = contentType,
            mediaUrl = doc.getString("mediaUrl"),
            mediaThumbnailUrl = doc.getString("mediaThumbnailUrl"),
            mediaFileSize = doc.getLong("mediaFileSize") ?: 0L,
            mediaMimeType = doc.getString("mediaMimeType") ?: "",
            isRead = doc.getBoolean("isRead") ?: false,
            readByUids = readByUids,
            deliveredToUids = deliveredToUids,
            replyToMessageId = doc.getString("replyToMessageId"),
            replyToPreview = doc.getString("replyToPreview"),
            replyToSenderName = doc.getString("replyToSenderName"),
            forwardedFromUid = doc.getString("forwardedFromUid"),
            forwardedFromName = doc.getString("forwardedFromName"),
            isForwarded = doc.getBoolean("isForwarded") ?: false,
            reactions = reactions,
            isDeletedForMe = doc.getBoolean("isDeletedForMe") ?: false,
            isDeletedForEveryone = doc.getBoolean("isDeletedForEveryone") ?: false,
            isEdited = doc.getBoolean("isEdited") ?: false,
            editedAt = doc.getLong("editedAt"),
            ephemeralExpiresAt = doc.getLong("ephemeralExpiresAt"),
            caption = doc.getString("caption")
        )
    }

    // ── Firestore Mapping Extensions ──────────────────────────────────────────

    private fun LastMessageInfo.toFirestoreMap(): Map<String, Any?> = mapOf(
        "senderUid" to senderUid,
        "senderDisplayName" to senderDisplayName,
        "content" to content,
        "type" to type.name,
        "timestamp" to timestamp,
        "isRead" to isRead
    )

    private fun ThreadParticipant.toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "zixoNumber" to zixoNumber,
        "role" to role.name,
        "joinedAt" to joinedAt,
        "isOnline" to isOnline
    )
}
