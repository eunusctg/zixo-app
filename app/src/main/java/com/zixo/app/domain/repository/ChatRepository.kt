package com.zixo.app.domain.repository

import com.zixo.app.domain.model.CallRecord
import com.zixo.app.domain.model.Chat
import com.zixo.app.domain.model.Message
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val chats: StateFlow<List<Chat>>
    val callHistory: StateFlow<List<CallRecord>>

    // Chat operations
    suspend fun getOrCreateChat(otherUserId: String): Result<String>
    fun getMessagesFlow(chatId: String): StateFlow<List<Message>>
    suspend fun sendMessage(chatId: String, text: String, replyToId: String = ""): Result<Message>
    suspend fun sendImageMessage(chatId: String, imageBytes: ByteArray, caption: String = ""): Result<Message>
    suspend fun deleteMessageForMe(chatId: String, messageId: String): Result<Unit>
    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit>
    suspend fun addReaction(chatId: String, messageId: String, emoji: String): Result<Unit>
    suspend fun removeReaction(chatId: String, messageId: String): Result<Unit>
    suspend fun markAsRead(chatId: String): Result<Unit>
    suspend fun forwardMessage(chatId: String, message: Message): Result<Message>

    // Call operations
    suspend fun initiateCall(receiverId: String, type: String): Result<CallRecord>
    suspend fun endCall(callId: String, duration: Long): Result<Unit>
    suspend fun getLiveKitToken(roomName: String): Result<String>

    // Realtime listeners
    fun startListening()
    fun stopListening()
    fun startMessagesListening(chatId: String)
    fun stopMessagesListening(chatId: String)
}
