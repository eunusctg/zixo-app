package com.zixo.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zixo.app.data.local.room.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the messages Room cache.
 * Provides paginated queries, full-text search, unread counts,
 * and unsynced record retrieval for the WorkManager sync engine.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForChatPaginated(chatId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchAllMessages(query: String, limit: Int = 50): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND isRead = 0 AND senderId != :currentUid")
    fun getUnreadCount(chatId: String, currentUid: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0 AND senderId != :currentUid")
    fun getTotalUnreadCount(currentUid: String): Flow<Int>

    @Query("UPDATE messages SET isRead = 1 WHERE chatId = :chatId AND senderId != :currentUid AND isRead = 0")
    suspend fun markAllRead(chatId: String, currentUid: String)

    @Query("UPDATE messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markRead(messageId: String)

    @Query("UPDATE messages SET isDelivered = 1 WHERE id = :messageId")
    suspend fun markDelivered(messageId: String)

    @Query("UPDATE messages SET isDeletedForMe = 1 WHERE id = :messageId")
    suspend fun markDeletedForMe(messageId: String)

    @Query("UPDATE messages SET isDeletedForEveryone = 1 WHERE id = :messageId")
    suspend fun markDeletedForEveryone(messageId: String)

    @Query("SELECT * FROM messages WHERE syncedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Query("UPDATE messages SET syncedAt = :timestamp WHERE id = :messageId")
    suspend fun markSynced(messageId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE chatId = :chatId AND isDeletedForMe = 1")
    suspend fun cleanupDeletedForMe(chatId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: String)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestMessage(chatId: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessageCount(chatId: String): Int
}
