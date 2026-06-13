package com.zixo.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.CallRecord
import com.zixo.app.domain.model.Chat
import com.zixo.app.domain.model.Message
import com.zixo.app.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepositoryImpl : ChatRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    override val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    override val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    private val messagesFlows = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val messagesListeners = mutableMapOf<String, ListenerRegistration>()

    private var chatsListener: ListenerRegistration? = null
    private var callsListener: ListenerRegistration? = null

    private val TAG = "ChatRepo"

    // ── Chat Operations ──

    override suspend fun getOrCreateChat(otherUserId: String): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            if (uid == otherUserId) return Result.failure(Exception("Cannot chat with yourself"))

            // Check if chat already exists — single field query (no composite index needed)
            val existing = firestore.collection("chats")
                .whereArrayContains("participants", uid)
                .get().await()

            for (doc in existing.documents) {
                try {
                    val participants = doc.get("participants") as? List<String> ?: continue
                    if (participants.contains(otherUserId)) {
                        return Result.success(doc.id)
                    }
                } catch (_: Exception) { continue }
            }

            // Create new chat
            val chatId = UUID.randomUUID().toString()
            val chat = Chat(
                id = chatId,
                participants = listOf(uid, otherUserId),
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("chats").document(chatId)
                .set(chat.toMap())
                .await()

            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create chat", e)
            Result.failure(e)
        }
    }

    override fun getMessagesFlow(chatId: String): StateFlow<List<Message>> {
        return messagesFlows.getOrPut(chatId) {
            MutableStateFlow(emptyList())
        }
    }

    override suspend fun sendMessage(chatId: String, text: String, replyToId: String): Result<Message> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val msgId = UUID.randomUUID().toString()
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = uid,
                text = text,
                timestamp = System.currentTimeMillis(),
                replyToId = replyToId
            )

            val batch = firestore.batch()
            batch.set(firestore.collection("chats").document(chatId)
                .collection("messages").document(msgId), message.toMap())
            batch.update(firestore.collection("chats").document(chatId), mapOf(
                "lastMessage" to text,
                "lastMessageSenderId" to uid,
                "lastMessageTimestamp" to message.timestamp
            ))
            batch.commit().await()

            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendImageMessage(chatId: String, imageBytes: ByteArray, caption: String): Result<Message> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val msgId = UUID.randomUUID().toString()
            val ref = storage.reference.child("chat_images/$chatId/$msgId.jpg")
            ref.putBytes(imageBytes).await()
            val imageUrl = ref.downloadUrl.await().toString()

            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = uid,
                text = caption,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("chats").document(chatId)
                .collection("messages").document(msgId)
                .set(message.toMap())
                .await()

            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send image message", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessageForMe(chatId: String, messageId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("deletedForUsers", FieldValue.arrayUnion(uid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message for me", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(mapOf(
                    "isDeletedForEveryone" to true,
                    "text" to "This message was deleted.",
                    "imageUrl" to ""
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message for everyone", e)
            Result.failure(e)
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, emoji: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("reactions.$uid", emoji)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add reaction", e)
            Result.failure(e)
        }
    }

    override suspend fun removeReaction(chatId: String, messageId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("reactions.$uid", FieldValue.delete())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove reaction", e)
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(chatId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("chats").document(chatId)
                .update("unreadCount.$uid", 0)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark as read", e)
            Result.failure(e)
        }
    }

    override suspend fun forwardMessage(chatId: String, message: Message): Result<Message> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val msgId = UUID.randomUUID().toString()
            val forwarded = Message(
                id = msgId,
                chatId = chatId,
                senderId = uid,
                text = message.text,
                imageUrl = message.imageUrl,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("chats").document(chatId)
                .collection("messages").document(msgId)
                .set(forwarded.toMap())
                .await()
            Result.success(forwarded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward message", e)
            Result.failure(e)
        }
    }

    // ── Call Operations ──

    override suspend fun initiateCall(receiverId: String, type: String): Result<CallRecord> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val callId = UUID.randomUUID().toString()
            val call = CallRecord(
                id = callId,
                callerId = uid,
                receiverId = receiverId,
                timestamp = System.currentTimeMillis(),
                type = type,
                status = "ringing",
                isOutgoing = true
            )
            firestore.collection("calls").document(callId)
                .set(call.toMap())
                .await()
            Result.success(call)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call", e)
            Result.failure(e)
        }
    }

    override suspend fun endCall(callId: String, duration: Long): Result<Unit> {
        return try {
            firestore.collection("calls").document(callId)
                .update(mapOf("duration" to duration, "status" to "ended"))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
            Result.failure(e)
        }
    }

    override suspend fun getLiveKitToken(roomName: String): Result<String> {
        return try {
            // LiveKit token should be fetched from a backend endpoint
            // For now, return an error indicating backend is needed
            Result.failure(Exception("LiveKit token endpoint not configured"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get LiveKit token", e)
            Result.failure(e)
        }
    }

    // ── Realtime Listeners ──

    override fun startListening() {
        startChatsListener()
        startCallsListener()
    }

    override fun stopListening() {
        chatsListener?.remove()
        chatsListener = null
        callsListener?.remove()
        callsListener = null
        messagesListeners.values.forEach { it.remove() }
        messagesListeners.clear()
    }

    override fun startMessagesListening(chatId: String) {
        if (messagesListeners.containsKey(chatId)) return

        val flow = messagesFlows.getOrPut(chatId) { MutableStateFlow(emptyList()) }

        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Messages listener error for $chatId", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                try {
                    val uid = auth.currentUser?.uid ?: ""
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try { documentToMessage(doc, uid) } catch (_: Exception) { null }
                    }
                    flow.value = messages
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process messages snapshot", e)
                }
            }

        messagesListeners[chatId] = listener
    }

    override fun stopMessagesListening(chatId: String) {
        messagesListeners.remove(chatId)?.remove()
    }

    private fun startChatsListener() {
        val uid = auth.currentUser?.uid ?: return
        // Use single-field query to avoid composite index requirement
        chatsListener = firestore.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Chats listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                try {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        try { documentToChat(doc) } catch (_: Exception) { null }
                    }.sortedByDescending { it.lastMessageTimestamp }
                    _chats.value = chats
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process chats snapshot", e)
                }
            }
    }

    private fun startCallsListener() {
        val uid = auth.currentUser?.uid ?: return

        // Strategy: Try whereArrayContains("participants") first (requires participants array field).
        // If that fails (e.g. old documents missing the field, or index not ready),
        // fall back to a simple unordered query and filter in memory.
        callsListener = firestore.collection("calls")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Calls listener error with participants query, trying fallback", error)
                    // Fallback to simple query without composite index
                    startCallsListenerFallback(uid)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                try {
                    val calls = snapshot.documents.mapNotNull { doc ->
                        try { documentToCallRecord(doc) } catch (_: Exception) { null }
                    }.sortedByDescending { it.timestamp }
                    _callHistory.value = calls
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process calls snapshot", e)
                    _callHistory.value = emptyList()
                }
            }
    }

    /**
     * Fallback calls listener — uses a simple unordered query by callerId
     * and filters by both callerId and receiverId in memory.
     * This avoids ANY composite index requirement.
     */
    private fun startCallsListenerFallback(uid: String) {
        callsListener?.remove()

        // Try querying by callerId (single field, no composite index needed)
        callsListener = firestore.collection("calls")
            .whereEqualTo("callerId", uid)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Calls fallback listener error", error)
                    // Last resort: try receiverId query
                    startCallsListenerLastResort(uid)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                try {
                    val callsAsCaller = snapshot.documents.mapNotNull { doc ->
                        try { documentToCallRecord(doc) } catch (_: Exception) { null }
                    }

                    // Also get calls where user is receiver
                    firestore.collection("calls")
                        .whereEqualTo("receiverId", uid)
                        .limit(50)
                        .get()
                        .addOnSuccessListener { receiverSnapshot ->
                            val callsAsReceiver = receiverSnapshot.documents.mapNotNull { doc ->
                                try { documentToCallRecord(doc) } catch (_: Exception) { null }
                            }
                            val allCalls = (callsAsCaller + callsAsReceiver)
                                .distinctBy { it.id }
                                .sortedByDescending { it.timestamp }
                            _callHistory.value = allCalls
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to fetch receiver calls", e)
                            _callHistory.value = callsAsCaller.sortedByDescending { it.timestamp }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process calls fallback snapshot", e)
                    _callHistory.value = emptyList()
                }
            }
    }

    /**
     * Last resort — fetch all recent calls and filter in memory.
     * This only uses the __name__ auto-index and requires no composite index.
     */
    private fun startCallsListenerLastResort(uid: String) {
        callsListener?.remove()

        scope.launch {
            try {
                val snapshot = firestore.collection("calls")
                    .limit(100)
                    .get()
                    .await()

                val calls = snapshot.documents.mapNotNull { doc ->
                    try { documentToCallRecord(doc) } catch (_: Exception) { null }
                }.filter { it.callerId == uid || it.receiverId == uid }
                    .sortedByDescending { it.timestamp }

                _callHistory.value = calls
            } catch (e: Exception) {
                Log.e(TAG, "Last resort calls fetch failed", e)
                _callHistory.value = emptyList()
            }
        }
    }

    // ── Document Mappers ──

    private fun documentToMessage(doc: com.google.firebase.firestore.DocumentSnapshot, currentUserId: String): Message {
        return Message(
            id = doc.id,
            chatId = doc.getString("chatId") ?: "",
            senderId = doc.getString("senderId") ?: "",
            text = doc.getString("text") ?: "",
            imageUrl = doc.getString("imageUrl") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L,
            isRead = doc.getBoolean("isRead") ?: false,
            replyToId = doc.getString("replyToId") ?: "",
            replyToText = doc.getString("replyToText") ?: "",
            replyToSender = doc.getString("replyToSender") ?: "",
            deletedForUsers = (doc.get("deletedForUsers") as? List<String>) ?: emptyList(),
            isDeletedForEveryone = doc.getBoolean("isDeletedForEveryone") ?: false,
            reactions = (doc.get("reactions") as? Map<String, String>) ?: emptyMap()
        )
    }

    private fun documentToChat(doc: com.google.firebase.firestore.DocumentSnapshot): Chat {
        return Chat(
            id = doc.id,
            participants = (doc.get("participants") as? List<String>) ?: emptyList(),
            participantNames = (doc.get("participantNames") as? Map<String, String>) ?: emptyMap(),
            participantAvatars = (doc.get("participantAvatars") as? Map<String, String>) ?: emptyMap(),
            lastMessage = doc.getString("lastMessage") ?: "",
            lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
            lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
            unreadCount = (doc.get("unreadCount") as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap(),
            createdAt = doc.getLong("createdAt") ?: 0L,
            isGroup = doc.getBoolean("isGroup") ?: false,
            groupName = doc.getString("groupName") ?: "",
            groupAvatar = doc.getString("groupAvatar") ?: ""
        )
    }

    private fun documentToCallRecord(doc: com.google.firebase.firestore.DocumentSnapshot): CallRecord {
        return CallRecord(
            id = doc.id,
            callerId = doc.getString("callerId") ?: "",
            callerName = doc.getString("callerName") ?: "",
            callerAvatar = doc.getString("callerAvatar") ?: "",
            receiverId = doc.getString("receiverId") ?: "",
            receiverName = doc.getString("receiverName") ?: "",
            receiverAvatar = doc.getString("receiverAvatar") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L,
            duration = doc.getLong("duration") ?: 0L,
            type = doc.getString("type") ?: "audio",
            status = doc.getString("status") ?: "missed",
            isOutgoing = doc.getBoolean("isOutgoing") ?: false
        )
    }

    // ── Model to Map converters ──

    private fun Message.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "chatId" to chatId,
        "senderId" to senderId,
        "text" to text,
        "imageUrl" to imageUrl,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "replyToId" to replyToId,
        "replyToText" to replyToText,
        "replyToSender" to replyToSender,
        "deletedForUsers" to deletedForUsers,
        "isDeletedForEveryone" to isDeletedForEveryone,
        "reactions" to reactions
    )

    private fun Chat.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "participants" to participants,
        "participantNames" to participantNames,
        "participantAvatars" to participantAvatars,
        "lastMessage" to lastMessage,
        "lastMessageSenderId" to lastMessageSenderId,
        "lastMessageTimestamp" to lastMessageTimestamp,
        "unreadCount" to unreadCount,
        "createdAt" to createdAt,
        "isGroup" to isGroup,
        "groupName" to groupName,
        "groupAvatar" to groupAvatar
    )

    private fun CallRecord.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "callerId" to callerId,
        "callerName" to callerName,
        "callerAvatar" to callerAvatar,
        "receiverId" to receiverId,
        "receiverName" to receiverName,
        "receiverAvatar" to receiverAvatar,
        "timestamp" to timestamp,
        "duration" to duration,
        "type" to type,
        "status" to status,
        "isOutgoing" to isOutgoing,
        "participants" to listOf(callerId, receiverId)
    )
}
