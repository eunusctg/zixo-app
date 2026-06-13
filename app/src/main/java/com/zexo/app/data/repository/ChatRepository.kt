package com.zexo.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zexo.app.data.model.Message
import com.zexo.app.data.model.MessageReaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun observeChats(userId: String): Flow<List<com.zexo.app.data.model.Chat>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        com.zexo.app.data.model.Chat(
                            id = doc.id,
                            participants = (doc.get("participants") as? List<String>) ?: emptyList(),
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageSender = doc.getString("lastMessageSender") ?: "",
                            lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                            unreadCount = (doc.getLong("unreadCount")?.toInt() ?: 0),
                            isGroup = doc.getBoolean("isGroup") ?: false,
                            groupName = doc.getString("groupName") ?: "",
                            groupAvatar = doc.getString("groupAvatar") ?: "",
                            typing = (doc.get("typing") as? List<String>) ?: emptyList(),
                            pinned = doc.getBoolean("pinned") ?: false,
                            muted = doc.getBoolean("muted") ?: false,
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { registration.remove() }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.id,
                            chatId = doc.getString("chatId") ?: chatId,
                            senderId = doc.getString("senderId") ?: "",
                            senderZixoNumber = doc.getString("senderZixoNumber") ?: "",
                            text = doc.getString("text") ?: "",
                            type = doc.getString("type") ?: "text",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            status = doc.getString("status") ?: "sent",
                            replyTo = doc.getString("replyTo") ?: "",
                            replyToTextPreview = doc.getString("replyToTextPreview"),
                            replyToSenderName = doc.getString("replyToSenderName"),
                            forwardedFromZixoNumber = doc.getString("forwardedFromZixoNumber"),
                            isDeletedForEveryone = doc.getBoolean("isDeletedForEveryone") ?: false,
                            deletedForUsers = (doc.get("deletedForUsers") as? List<String>) ?: emptyList(),
                            reactions = (doc.get("reactions") as? List<Map<String, String>>)?.mapNotNull { map ->
                                MessageReaction(
                                    senderZixoNumber = map["senderZixoNumber"] ?: "",
                                    reactionEmoji = map["reactionEmoji"] ?: ""
                                )
                            } ?: emptyList(),
                            starred = doc.getBoolean("starred") ?: false,
                            mediaUrl = doc.getString("mediaUrl") ?: "",
                            fileName = doc.getString("fileName") ?: "",
                            fileSize = doc.getLong("fileSize") ?: 0L,
                            duration = doc.getLong("duration") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(chatId: String, message: Message): Result<Message> {
        return try {
            val docRef = firestore.collection("chats").document(chatId)
                .collection("messages").document()
            val msgWithId = message.copy(id = docRef.id)

            val data = mutableMapOf<String, Any?>(
                "chatId" to msgWithId.chatId,
                "senderId" to msgWithId.senderId,
                "senderZixoNumber" to msgWithId.senderZixoNumber,
                "text" to msgWithId.text,
                "type" to msgWithId.type,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "sent",
                "replyTo" to msgWithId.replyTo,
                "replyToTextPreview" to msgWithId.replyToTextPreview,
                "replyToSenderName" to msgWithId.replyToSenderName,
                "forwardedFromZixoNumber" to msgWithId.forwardedFromZixoNumber,
                "isDeletedForEveryone" to false,
                "deletedForUsers" to emptyList<String>(),
                "reactions" to emptyList<Map<String, String>>(),
                "starred" to false,
                "mediaUrl" to msgWithId.mediaUrl,
                "fileName" to msgWithId.fileName,
                "fileSize" to msgWithId.fileSize,
                "duration" to msgWithId.duration
            )

            docRef.set(data).await()

            firestore.collection("chats").document(chatId).update(mapOf(
                "lastMessage" to msgWithId.text,
                "lastMessageSender" to msgWithId.senderId,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()

            Result.success(msgWithId)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send message"))
        }
    }

    suspend fun addReaction(chatId: String, messageId: String, reaction: MessageReaction): Result<Unit> {
        return try {
            val docRef = firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
            val doc = docRef.get().await()
            val existingReactions = (doc.get("reactions") as? List<Map<String, String>>) ?: emptyList()
            val updatedReactions = existingReactions + mapOf(
                "senderZixoNumber" to reaction.senderZixoNumber,
                "reactionEmoji" to reaction.reactionEmoji
            )
            docRef.update("reactions", updatedReactions).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to add reaction"))
        }
    }

    suspend fun deleteMessageForMe(chatId: String, messageId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
            val doc = docRef.get().await()
            val deletedFor = (doc.get("deletedForUsers") as? List<String>) ?: emptyList()
            if (!deletedFor.contains(userId)) {
                docRef.update("deletedForUsers", deletedFor + userId).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to delete message"))
        }
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
            docRef.update(mapOf(
                "isDeletedForEveryone" to true,
                "text" to "This message was deleted.",
                "mediaUrl" to "",
                "fileName" to "",
                "fileSize" to 0L,
                "reactions" to emptyList<Map<String, String>>()
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to delete message for everyone"))
        }
    }

    suspend fun forwardMessage(sourceChatId: String, targetChatId: String, messageId: String, forwarderZixoNumber: String, forwarderId: String): Result<Unit> {
        return try {
            val sourceDoc = firestore.collection("chats").document(sourceChatId)
                .collection("messages").document(messageId).get().await()

            val originalText = sourceDoc.getString("text") ?: ""
            val originalMediaUrl = sourceDoc.getString("mediaUrl") ?: ""
            val originalType = sourceDoc.getString("type") ?: "text"

            val forwardedMessage = Message(
                chatId = targetChatId,
                senderId = forwarderId,
                senderZixoNumber = forwarderZixoNumber,
                text = originalText,
                type = originalType,
                mediaUrl = originalMediaUrl,
                forwardedFromZixoNumber = forwarderZixoNumber,
                timestamp = System.currentTimeMillis(),
                status = "sending"
            )
            sendMessage(targetChatId, forwardedMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to forward message"))
        }
    }

    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            val snapshot = firestore.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get().await()

            val existingChat = snapshot.documents.find { doc ->
                val participants = doc.get("participants") as? List<String> ?: emptyList()
                participants.contains(otherUserId) && participants.size == 2
            }

            if (existingChat != null) {
                Result.success(existingChat.id)
            } else {
                val docRef = firestore.collection("chats").document()
                docRef.set(mapOf(
                    "participants" to listOf(currentUserId, otherUserId),
                    "isGroup" to false,
                    "unreadCount" to 0,
                    "lastMessage" to "",
                    "lastMessageSender" to "",
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "typing" to emptyList<String>(),
                    "pinned" to false,
                    "muted" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )).await()
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to create chat"))
        }
    }

    suspend fun markChatRead(chatId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId)
                .update("unreadCount", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to mark as read"))
        }
    }
}
