package com.zexo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.*
import com.google.firebase.database.FirebaseDatabase
import com.zexo.app.data.model.Chat
import com.zexo.app.data.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val rtdb: FirebaseDatabase
) {
    companion object { private const val TAG = "ChatRepository" }

    fun observeChats(uid: String): Flow<List<Chat>> = callbackFlow {
        var subscription: ListenerRegistration? = null
        try {
            val query = firestore.collection("chats")
                .whereArrayContains("participants", uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
            
            subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Chat listener error, trying fallback", error)
                    // Fallback without orderBy
                    val fallbackQuery = firestore.collection("chats")
                        .whereArrayContains("participants", uid)
                    fallbackQuery.addSnapshotListener { fallbackSnap, _ ->
                        if (fallbackSnap != null) {
                            val chats = fallbackSnap.documents.mapNotNull { doc -> parseChat(doc) }
                                .sortedByDescending { it.updatedAt ?: 0L }
                            trySend(chats)
                        }
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc -> parseChat(doc) }
                    trySend(chats)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe chats", e)
        }
        awaitClose { subscription?.remove() }
    }

    fun observeMessages(chatId: String, limit: Long = 100): Flow<List<Message>> = callbackFlow {
        var subscription: ListenerRegistration? = null
        try {
            val query = firestore.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit)
            
            subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Messages listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc -> parseMessage(doc) }
                    trySend(messages)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe messages", e)
        }
        awaitClose { subscription?.remove() }
    }

    suspend fun createOrGetChat(uid1: String, uid2: String): Result<String> {
        return try {
            // Check if chat exists
            val query = firestore.collection("chats")
                .whereArrayContains("participants", uid1)
                .get().await()
            
            for (doc in query.documents) {
                val participants = doc.get("participants") as? List<String> ?: continue
                if (participants.contains(uid2)) {
                    val isGroup = doc.getBoolean("isGroup") ?: false
                    if (!isGroup) return Result.success(doc.id)
                }
            }
            
            // Create new chat
            val chatRef = firestore.collection("chats").document()
            val chatData = mapOf(
                "participants" to listOf(uid1, uid2),
                "isGroup" to false,
                "unreadCount" to mapOf(uid1 to 0, uid2 to 0),
                "typing" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(chatData).await()
            Result.success(chatRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Create or get chat failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String, type: String = "text"): Result<String> {
        return try {
            val msgRef = firestore.collection("chats").document(chatId)
                .collection("messages").document()
            
            val msgData = mapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "text" to text,
                "type" to type,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "sent",
                "starred" to false
            )
            msgRef.set(msgData).await()
            
            // Update chat metadata
            val chatRef = firestore.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            if (chatSnap.exists()) {
                val participants = chatSnap.get("participants") as? List<String> ?: emptyList()
                val unreadUpdates = mutableMapOf<String, Any>()
                participants.forEach { uid ->
                    if (uid != senderId) {
                        val currentCount = (chatSnap.get("unreadCount.$uid") as? Long)?.toInt() ?: 0
                        unreadUpdates["unreadCount.$uid"] = currentCount + 1
                    }
                }
                chatRef.update(mapOf(
                    "lastMessage" to text,
                    "lastMessageSender" to senderId,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ) + unreadUpdates).await()
            }
            Result.success(msgRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Send message failed", e)
            Result.failure(e)
        }
    }

    suspend fun markChatRead(chatId: String, uid: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId)
                .update(mapOf("unreadCount.$uid" to 0, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setTyping(chatId: String, uid: String, isTyping: Boolean) {
        try {
            val typingRef = rtdb.getReference("typing").child(chatId).child(uid)
            if (isTyping) {
                typingRef.setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
            } else {
                typingRef.removeValue()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Set typing failed", e)
        }
    }

    fun observeTyping(chatId: String): Flow<List<String>> = callbackFlow {
        val typingRef = rtdb.getReference("typing").child(chatId)
        val listener = typingRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val uids = mutableListOf<String>()
                val now = System.currentTimeMillis()
                for (child in snapshot.children) {
                    val ts = child.getValue(Long::class.java) ?: 0L
                    if (now - ts < 5000) {
                        uids.add(child.key ?: "")
                    }
                }
                trySend(uids)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
        awaitClose { typingRef.removeEventListener(listener) }
    }

    private fun parseChat(doc: DocumentSnapshot): Chat? {
        return try {
            Chat(
                id = doc.id,
                participants = (doc.get("participants") as? List<String>) ?: emptyList(),
                isGroup = doc.getBoolean("isGroup") ?: false,
                groupName = doc.getString("groupName") ?: "",
                groupAvatar = doc.getString("groupAvatar") ?: "",
                lastMessage = doc.getString("lastMessage") ?: "",
                lastMessageSender = doc.getString("lastMessageSender") ?: "",
                lastMessageTime = doc.getLong("lastMessageTime"),
                unreadCount = (doc.get("unreadCount") as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap(),
                typing = (doc.get("typing") as? List<String>) ?: emptyList(),
                pinned = doc.getBoolean("pinned") ?: false,
                muted = (doc.get("muted") as? List<String>) ?: emptyList(),
                createdAt = doc.getLong("createdAt"),
                updatedAt = doc.getLong("updatedAt")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse chat failed", e)
            null
        }
    }

    private fun parseMessage(doc: DocumentSnapshot): Message? {
        return try {
            Message(
                id = doc.id,
                chatId = doc.getString("chatId") ?: "",
                senderId = doc.getString("senderId") ?: "",
                text = doc.getString("text") ?: "",
                type = doc.getString("type") ?: "text",
                timestamp = doc.getLong("timestamp"),
                status = doc.getString("status") ?: "sent",
                replyTo = doc.getString("replyTo") ?: "",
                starred = doc.getBoolean("starred") ?: false,
                mediaUrl = doc.getString("mediaUrl") ?: "",
                fileName = doc.getString("fileName") ?: "",
                fileSize = doc.getLong("fileSize") ?: 0L,
                duration = doc.getLong("duration") ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse message failed", e)
            null
        }
    }
}
