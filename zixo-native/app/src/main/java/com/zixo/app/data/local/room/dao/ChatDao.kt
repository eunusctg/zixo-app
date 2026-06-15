package com.zixo.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zixo.app.data.local.room.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_threads ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllThreads(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_threads WHERE id = :id LIMIT 1")
    fun getThreadById(id: String): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threads: List<ChatEntity>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateThread(thread: ChatEntity)

    @Query("DELETE FROM chat_threads WHERE id = :id")
    suspend fun deleteThread(id: String)

    @Query(
        """
        SELECT * FROM chat_threads
        WHERE lastMessage LIKE :query
           OR participantUids LIKE :query
        ORDER BY isPinned DESC, lastMessageTimestamp DESC
        """
    )
    fun searchThreads(query: String): Flow<List<ChatEntity>>
}
