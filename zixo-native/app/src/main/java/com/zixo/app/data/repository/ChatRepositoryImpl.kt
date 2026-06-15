package com.zixo.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.LastMessageInfo
import com.zixo.app.domain.model.MessageContentType
import com.zixo.app.domain.model.MessageModel
import com.zixo.app.domain.model.MessageReaction
import com.zixo.app.domain.model.ParticipantRole
import com.zixo.app.domain.model.ThreadParticipant
import com.zixo.app.domain.model.ThreadType
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
 * snapshot listeners for real-time sync. Uses Firebase Storage for media uploads
 * and Firebase Realtime Database for presence and typing indicators.
 *
 * Zero-trust enforcement: All send operations verify mutual contact status
 * through [ContactRepository.verifyMutualContact] before executing.
 * Non-contact messages are blocked at this boundary.
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
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

    // ── Get Threads ───────────────────────────────────────────────────────────

    override fun getThreads(): Flow<List<ChatThreadModel>> = callbackFlow {
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
                        mapToChatThreadModel(doc, myUid)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map thread: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(threads)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    override fun observeThreadsRealtime(): Flow<List<ChatThreadModel>> = getThreads()

    // ── Get Messages ──────────────────────────────────────────────────────────

    override fun getMessages(threadId: String): Flow<List<MessageModel>> = callbackFlow {
        val subscription = messagesCollection(threadId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing messages for thread: %s", threadId)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToMessageModel(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map message: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    override fun observeMessagesRealtime(threadId: String): Flow<List<MessageModel>> =
        getMessages(threadId)

    // ── Send Message ──────────────────────────────────────────────────────────

    override fun sendMessage(
        threadId: String,
        message: MessageModel
    ): Flow<Result<MessageModel>> = flow {
        val myUid = currentUid
            ?: throw IllegalStateException("Not authenticated")

        try {
            // Zero-trust: verify mutual contact before sending
            val threadDoc = threadsCollection.document(threadId).get().await()
            if (!threadDoc.exists()) {
                emit(Result.failure(IllegalStateException("Thread not found")))
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>)
                ?: emptyList()

            // Verify communication gate for all other participants
            for (participantUid in participantUids) {
                if (participantUid != myUid) {
                    val gate = contactRepository.verifyMutualContact(participantUid)
                    var gateResult: CommunicationGate? = null
                    gate.collect { gateResult = it }
                    if (gateResult !is CommunicationGate.Allowed) {
                        emit(Result.failure(SecurityException(
                            "Communication denied with $participantUid"
                        )))
                        return@flow
                    }
                }
            }

            // Create the message document
            val messageData = messageToFirestoreMap(message)
            messagesCollection(threadId).document(message.id).set(messageData).await()

            // Update the thread's last message metadata
            val threadUpdates = mapOf(
                "lastMessage" to (message.content.takeIf { it.isNotBlank() } ?: "📎 Media"),
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderUid" to message.senderUid,
                "lastMessageSenderName" to message.senderDisplayName,
                "lastMessageType" to message.type.name
            )
            threadsCollection.document(threadId).update(threadUpdates).await()

            Timber.d("Message sent: %s in thread %s", message.id, threadId)
            emit(Result.success(message))
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Delete For Me ─────────────────────────────────────────────────────────

    override fun deleteForMe(messageId: String, threadId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Mark the message as deleted for the current user only
            val deletedForMeUids = messagesCollection(threadId).document(messageId)
                .get().await()
                .get("deletedForMeUids") as? List<String> ?: emptyList()

            messagesCollection(threadId).document(messageId)
                .update("deletedForMeUids", deletedForMeUids + myUid)
                .await()

            Timber.d("Message deleted for me: %s", messageId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete message for me: %s", messageId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Delete For Everyone ───────────────────────────────────────────────────

    override fun deleteForEveryone(messageId: String, threadId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Only the sender can delete for everyone
            val messageDoc = messagesCollection(threadId).document(messageId).get().await()
            val senderUid = messageDoc.getString("senderUid") ?: ""
            if (senderUid != myUid) {
                emit(Result.failure(SecurityException("Only the sender can delete for everyone")))
                return@flow
            }

            val updates = mapOf(
                "content" to "",
                "isDeletedForEveryone" to true,
                "type" to MessageContentType.DELETED_PLACEHOLDER.name,
                "mediaUrl" to null,
                "mediaThumbnailUrl" to null,
                "caption" to null
            )
            messagesCollection(threadId).document(messageId).update(updates).await()

            Timber.d("Message deleted for everyone: %s", messageId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete message for everyone: %s", messageId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Add Reaction ──────────────────────────────────────────────────────────

    override fun addReaction(
        messageId: String,
        threadId: String,
        emoji: String,
        isThreeD: Boolean
    ): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")
            val myName = firebaseAuth.currentUser?.displayName ?: ""

            val messageDoc = messagesCollection(threadId).document(messageId).get().await()
            if (!messageDoc.exists()) {
                emit(Result.failure(IllegalStateException("Message not found")))
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val existingReactions = (messageDoc.get("reactions") as? List<Map<String, Any>>)
                ?: emptyList()

            // Remove any existing reaction by this user, then add the new one
            val filteredReactions = existingReactions.filterNot { it["uid"] == myUid }
            val newReaction = mapOf(
                "uid" to myUid,
                "emoji" to emoji,
                "timestamp" to System.currentTimeMillis(),
                "isThreeD" to isThreeD
            )

            messagesCollection(threadId).document(messageId)
                .update("reactions", filteredReactions + newReaction)
                .await()

            Timber.d("Reaction added: %s to message %s", emoji, messageId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to add reaction")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Mark As Read ──────────────────────────────────────────────────────────

    override fun markAsRead(threadId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Reset unread count for the current user
            val threadUpdates = mapOf("unreadCount.$myUid" to 0)
            threadsCollection.document(threadId).update(threadUpdates).await()

            // Mark recent unread messages as read
            val unreadMessages = messagesCollection(threadId)
                .whereNotIn("readByUids", listOf(myUid))
                .limit(50)
                .get()
                .await()

            for (doc in unreadMessages.documents) {
                @Suppress("UNCHECKED_CAST")
                val readByUids = (doc.get("readByUids") as? List<String>) ?: emptyList()
                if (!readByUids.contains(myUid)) {
                    doc.reference.update("readByUids", readByUids + myUid).await()
                }
            }

            Timber.d("Thread marked as read: %s", threadId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark thread as read: %s", threadId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Create Group Thread ───────────────────────────────────────────────────

    override fun createGroupThread(
        name: String,
        participantUids: Set<String>
    ): Flow<Result<ChatThreadModel>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Verify all participants are mutual contacts
            val verifiedUids = mutableSetOf(myUid)
            for (uid in participantUids) {
                val gate = contactRepository.verifyMutualContact(uid)
                var gateResult: CommunicationGate? = null
                gate.collect { gateResult = it }
                if (gateResult is CommunicationGate.Allowed) {
                    verifiedUids.add(uid)
                } else {
                    Timber.w("Skipping non-mutual participant: %s", uid)
                }
            }

            if (verifiedUids.size < 2) {
                emit(Result.failure(IllegalStateException(
                    "At least one mutual contact is required to create a group"
                )))
                return@flow
            }

            val threadId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // Fetch participant profiles for denormalization
            val participantProfiles = mutableMapOf<String, Map<String, Any?>>()
            for (uid in verifiedUids) {
                val profileDoc = firestore.collection("users").document(uid).get().await()
                participantProfiles[uid] = mapOf(
                    "uid" to uid,
                    "displayName" to (profileDoc.getString("displayName") ?: ""),
                    "avatarUrl" to (profileDoc.getString("photoUrl") ?: ""),
                    "zixoNumber" to (profileDoc.getString("zixoNumber") ?: ""),
                    "role" to if (uid == myUid) ParticipantRole.ADMIN.name else ParticipantRole.MEMBER.name,
                    "joinedAt" to now,
                    "isOnline" to (profileDoc.getBoolean("isOnline") ?: false)
                )
            }

            val threadData = hashMapOf(
                "id" to threadId,
                "type" to ThreadType.GROUP.name,
                "participantUids" to verifiedUids.toList(),
                "participantProfiles" to participantProfiles,
                "groupName" to name,
                "groupAvatarUrl" to null,
                "groupDescription" to null,
                "groupAdminUids" to listOf(myUid),
                "createdByUid" to myUid,
                "createdAt" to now,
                "lastMessage" to null,
                "lastMessageTimestamp" to now,
                "unreadCount" to emptyMap<String, Int>(),
                "isPinned" to false,
                "isMuted" to false,
                "isArchived" to false,
                "ephemeralTimerSeconds" to 0
            )

            threadsCollection.document(threadId).set(threadData).await()

            val thread = ChatThreadModel(
                id = threadId,
                type = ThreadType.GROUP,
                participantUids = verifiedUids,
                groupName = name,
                groupAdminUids = setOf(myUid),
                createdByUid = myUid,
                createdAt = now
            )

            Timber.d("Group thread created: %s with %d participants", threadId, verifiedUids.size)
            emit(Result.success(thread))
        } catch (e: Exception) {
            Timber.e(e, "Failed to create group thread")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Get Chat Thread ──────────────────────────────────────────────────────

    override fun getChatThread(chatId: String): Flow<ChatThreadModel?> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val subscription = threadsCollection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing chat thread: %s", chatId)
                    trySend(null)
                    return@addSnapshotListener
                }

                val thread = snapshot?.let { doc ->
                    try {
                        mapToChatThreadModel(doc, myUid)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map thread: %s", doc.id)
                        null
                    }
                }

                trySend(thread)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Get Group Members ────────────────────────────────────────────────────

    override fun getGroupMembers(chatId: String): Flow<List<ThreadParticipant>> = callbackFlow {
        val subscription = threadsCollection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing group members for: %s", chatId)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                @Suppress("UNCHECKED_CAST")
                val profilesRaw = snapshot?.get("participantProfiles") as? Map<String, Map<String, Any>>
                    ?: emptyMap()

                val members = profilesRaw.map { (uid, data) ->
                    val roleStr = data["role"] as? String ?: "MEMBER"
                    ThreadParticipant(
                        uid = uid,
                        displayName = data["displayName"] as? String ?: "",
                        avatarUrl = data["avatarUrl"] as? String ?: "",
                        zixoNumber = data["zixoNumber"] as? String ?: "",
                        role = try { ParticipantRole.valueOf(roleStr) } catch (_: Exception) { ParticipantRole.MEMBER },
                        joinedAt = data["joinedAt"] as? Long ?: 0L,
                        isOnline = data["isOnline"] as? Boolean ?: false
                    )
                }

                trySend(members)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Update Group Name ────────────────────────────────────────────────────

    override fun updateGroupName(chatId: String, name: String): Flow<Result<Unit>> = flow {
        try {
            threadsCollection.document(chatId)
                .update("groupName", name)
                .await()
            Timber.d("Group name updated: %s", chatId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to update group name: %s", chatId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Update Group Description ─────────────────────────────────────────────

    override fun updateGroupDescription(chatId: String, description: String): Flow<Result<Unit>> = flow {
        try {
            threadsCollection.document(chatId)
                .update("groupDescription", description)
                .await()
            Timber.d("Group description updated: %s", chatId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to update group description: %s", chatId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Update Member Role ───────────────────────────────────────────────────

    override fun updateMemberRole(chatId: String, userId: String, role: ParticipantRole): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Verify the current user is an admin
            val threadDoc = threadsCollection.document(chatId).get().await()
            @Suppress("UNCHECKED_CAST")
            val adminUids = (threadDoc.get("groupAdminUids") as? List<String>) ?: emptyList()
            if (myUid !in adminUids) {
                emit(Result.failure(SecurityException("Only admins can change member roles")))
                return@flow
            }

            // Update the participant's role in the denormalized profiles
            val fieldPath = "participantProfiles.$userId.role"
            threadsCollection.document(chatId)
                .update(fieldPath, role.name)
                .await()

            // Update admin list if changing to/from admin role
            if (role == ParticipantRole.ADMIN && myUid !in adminUids) {
                threadsCollection.document(chatId)
                    .update("groupAdminUids", adminUids + userId)
                    .await()
            } else if (role != ParticipantRole.ADMIN && userId in adminUids) {
                threadsCollection.document(chatId)
                    .update("groupAdminUids", adminUids.filter { it != userId })
                    .await()
            }

            Timber.d("Member role updated: %s -> %s in %s", userId, role.name, chatId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to update member role")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Remove Group Member ──────────────────────────────────────────────────

    override fun removeGroupMember(chatId: String, userId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Verify admin status
            val threadDoc = threadsCollection.document(chatId).get().await()
            @Suppress("UNCHECKED_CAST")
            val adminUids = (threadDoc.get("groupAdminUids") as? List<String>) ?: emptyList()
            if (myUid !in adminUids) {
                emit(Result.failure(SecurityException("Only admins can remove members")))
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>) ?: emptyList()
            val updatedUids = participantUids.filter { it != userId }

            val updates = mapOf(
                "participantUids" to updatedUids,
                "groupAdminUids" to adminUids.filter { it != userId }
            )

            threadsCollection.document(chatId).update(updates).await()

            Timber.d("Member removed: %s from %s", userId, chatId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove group member")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Leave Group ──────────────────────────────────────────────────────────

    override fun leaveGroup(chatId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            val threadDoc = threadsCollection.document(chatId).get().await()
            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val adminUids = (threadDoc.get("groupAdminUids") as? List<String>) ?: emptyList()

            val updatedUids = participantUids.filter { it != myUid }
            val updatedAdminUids = adminUids.filter { it != myUid }

            val updates = mutableMapOf<String, Any>(
                "participantUids" to updatedUids,
                "groupAdminUids" to updatedAdminUids
            )

            threadsCollection.document(chatId).update(updates.toMap()).await()

            Timber.d("Left group: %s", chatId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to leave group: %s", chatId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Toggle Mute Chat ─────────────────────────────────────────────────────

    override fun toggleMuteChat(chatId: String, isMuted: Boolean): Flow<Result<Unit>> = flow {
        try {
            threadsCollection.document(chatId)
                .update("isMuted", isMuted)
                .await()
            Timber.d("Chat mute toggled: %s -> %b", chatId, isMuted)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle mute: %s", chatId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Mapping Helpers ───────────────────────────────────────────────────────

    private fun mapToChatThreadModel(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        myUid: String
    ): ChatThreadModel? {
        return try {
            val typeStr = doc.getString("type") ?: "SINGLE"
            val threadType = try { ThreadType.valueOf(typeStr) } catch (_: Exception) { ThreadType.SINGLE }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (doc.get("participantUids") as? List<String>)?.toSet()
                ?: emptySet()

            @Suppress("UNCHECKED_CAST")
            val profilesRaw = doc.get("participantProfiles") as? Map<String, Map<String, Any>>
                ?: emptyMap()

            val participantProfiles = profilesRaw.map { (uid, data) ->
                val roleStr = data["role"] as? String ?: "MEMBER"
                uid to ThreadParticipant(
                    uid = uid,
                    displayName = data["displayName"] as? String ?: "",
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    zixoNumber = data["zixoNumber"] as? String ?: "",
                    role = try { ParticipantRole.valueOf(roleStr) } catch (_: Exception) { ParticipantRole.MEMBER },
                    joinedAt = data["joinedAt"] as? Long ?: 0L,
                    isOnline = data["isOnline"] as? Boolean ?: false
                )
            }.toMap()

            @Suppress("UNCHECKED_CAST")
            val adminUids = (doc.get("groupAdminUids") as? List<String>)?.toSet()
                ?: emptySet()

            val lastMessage = if (doc.contains("lastMessage") && doc.getString("lastMessage") != null) {
                LastMessageInfo(
                    senderUid = doc.getString("lastMessageSenderUid") ?: "",
                    senderDisplayName = doc.getString("lastMessageSenderName") ?: "",
                    content = doc.getString("lastMessage") ?: "",
                    type = try {
                        MessageContentType.valueOf(doc.getString("lastMessageType") ?: "TEXT")
                    } catch (_: Exception) { MessageContentType.TEXT },
                    timestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                    isRead = true
                )
            } else null

            @Suppress("UNCHECKED_CAST")
            val unreadCounts = doc.get("unreadCount") as? Map<String, Long> ?: emptyMap()
            val unreadCount = (unreadCounts[myUid] ?: 0L).toInt()

            ChatThreadModel(
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
                unreadCount = unreadCount,
                isPinned = doc.getBoolean("isPinned") ?: false,
                isMuted = doc.getBoolean("isMuted") ?: false,
                isArchived = doc.getBoolean("isArchived") ?: false,
                ephemeralTimerSeconds = doc.getLong("ephemeralTimerSeconds")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map ChatThreadModel from document: %s", doc.id)
            null
        }
    }

    private fun mapToMessageModel(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): MessageModel? {
        return try {
            val typeStr = doc.getString("type") ?: "TEXT"
            val contentType = try { MessageContentType.valueOf(typeStr) }
                catch (_: Exception) { MessageContentType.TEXT }

            @Suppress("UNCHECKED_CAST")
            val readByUids = (doc.get("readByUids") as? List<String>)?.toSet() ?: emptySet()

            @Suppress("UNCHECKED_CAST")
            val deliveredToUids = (doc.get("deliveredToUids") as? List<String>)?.toSet()
                ?: emptySet()

            @Suppress("UNCHECKED_CAST")
            val reactionsRaw = doc.get("reactions") as? List<Map<String, Any>> ?: emptyList()

            val reactions = reactionsRaw.map { data ->
                MessageReaction(
                    uid = data["uid"] as? String ?: "",
                    emoji = data["emoji"] as? String ?: "",
                    timestamp = data["timestamp"] as? Long ?: 0L,
                    isThreeD = data["isThreeD"] as? Boolean ?: false
                )
            }

            @Suppress("UNCHECKED_CAST")
            val deletedForMeUids = (doc.get("deletedForMeUids") as? List<String>) ?: emptyList()

            MessageModel(
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
                isRead = readByUids.isNotEmpty(),
                readByUids = readByUids,
                deliveredToUids = deliveredToUids,
                replyToMessageId = doc.getString("replyToMessageId"),
                replyToPreview = doc.getString("replyToPreview"),
                replyToSenderName = doc.getString("replyToSenderName"),
                forwardedFromUid = doc.getString("forwardedFromUid"),
                forwardedFromName = doc.getString("forwardedFromName"),
                isForwarded = doc.getBoolean("isForwarded") ?: false,
                reactions = reactions,
                isDeletedForMe = deletedForMeUids.contains(currentUid),
                isDeletedForEveryone = doc.getBoolean("isDeletedForEveryone") ?: false,
                isEdited = doc.getBoolean("isEdited") ?: false,
                editedAt = doc.getLong("editedAt"),
                ephemeralExpiresAt = doc.getLong("ephemeralExpiresAt"),
                caption = doc.getString("caption")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map MessageModel from document: %s", doc.id)
            null
        }
    }

    private fun messageToFirestoreMap(message: MessageModel): Map<String, Any?> = mapOf(
        "id" to message.id,
        "threadId" to message.threadId,
        "senderUid" to message.senderUid,
        "senderDisplayName" to message.senderDisplayName,
        "senderAvatarUrl" to message.senderAvatarUrl,
        "content" to message.content,
        "timestamp" to message.timestamp,
        "type" to message.type.name,
        "mediaUrl" to message.mediaUrl,
        "mediaThumbnailUrl" to message.mediaThumbnailUrl,
        "mediaFileSize" to message.mediaFileSize,
        "mediaMimeType" to message.mediaMimeType,
        "readByUids" to message.readByUids.toList(),
        "deliveredToUids" to message.deliveredToUids.toList(),
        "replyToMessageId" to message.replyToMessageId,
        "replyToPreview" to message.replyToPreview,
        "replyToSenderName" to message.replyToSenderName,
        "forwardedFromUid" to message.forwardedFromUid,
        "forwardedFromName" to message.forwardedFromName,
        "isForwarded" to message.isForwarded,
        "reactions" to emptyList<Map<String, Any>>(),
        "isDeletedForEveryone" to false,
        "isEdited" to false,
        "ephemeralExpiresAt" to message.ephemeralExpiresAt,
        "caption" to message.caption
    )
}
