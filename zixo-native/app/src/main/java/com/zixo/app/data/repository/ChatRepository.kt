package com.zixo.app.data.repository

import com.zixo.app.data.local.room.dao.ChatDao
import com.zixo.app.data.local.room.entity.toDomain
import com.zixo.app.data.local.room.entity.toEntity
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.ChatThread
import com.zixo.app.domain.model.Message
import com.zixo.app.domain.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val chatDao: ChatDao,
    private val firebaseAuthService: FirebaseAuthService
) {

    /**
     * Observes all chat threads from the local Room database.
     * The list is kept in sync with Firestore via [syncThreadsFromRemote].
     */
    fun getChatThreads(): Flow<List<ChatThread>> =
        chatDao.getAllThreads().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Observes messages for a specific thread directly from Firestore,
     * limited to [limit] most recent messages.
     */
    fun getMessages(threadId: String, limit: Int = 50): Flow<List<Message>> =
        firestoreService.getMessages(threadId, limit.toLong())

    /**
     * Sends a message to a chat thread. The message is first sent to Firestore
     * for delivery and multi-device sync, and the local thread cache is updated
     * on the next sync cycle.
     *
     * @param threadId The ID of the thread to send the message to.
     * @param content The text or media content of the message.
     * @param type The message type string (e.g., "TEXT", "IMAGE", "VIDEO", "AUDIO", "FILE").
     * @return A [Flow] that emits [Unit] once the message has been sent successfully.
     */
    fun sendMessage(threadId: String, content: String, type: String): Flow<Unit> = flow {
        val currentUserId = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot send message: no authenticated user")

        val messageType = runCatching { MessageType.valueOf(type) }
            .getOrDefault(MessageType.TEXT)

        val message = Message(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            senderUid = currentUserId,
            content = content,
            timestamp = Instant.now(),
            type = messageType
        )

        firestoreService.sendMessage(threadId, message).first()
        emit(Unit)
    }

    /**
     * Searches local chat threads by last message content or participant UIDs.
     *
     * @param query The search query string.
     * @return A [Flow] of matching [ChatThread] lists.
     */
    fun searchThreads(query: String): Flow<List<ChatThread>> =
        chatDao.searchThreads("%$query%").map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Performs a full sync of chat threads from Firestore into the local
     * Room database. This ensures the local cache is up to date with the
     * remote data source.
     *
     * Should be called periodically or when the user explicitly requests a refresh.
     */
    suspend fun syncThreadsFromRemote() {
        val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
        val remoteThreads = firestoreService.getChatThreads(uid).first()
        val entities = remoteThreads.map { it.toEntity() }
        chatDao.insertAll(entities)
    }
}
