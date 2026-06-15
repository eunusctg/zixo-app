package com.zixo.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zixo.app.data.local.room.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the statuses Room cache.
 * Supports offline viewing, auto-expiration cleanup,
 * and unsynced record retrieval for the sync engine.
 */
@Dao
interface StatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: StatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<StatusEntity>)

    @Delete
    suspend fun delete(status: StatusEntity)

    @Query("DELETE FROM statuses WHERE id = :statusId")
    suspend fun deleteById(statusId: String)

    @Query("SELECT * FROM statuses WHERE userId = :userId ORDER BY createdAt DESC")
    fun getByUser(userId: String): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE isMyStatus = 1 ORDER BY createdAt DESC")
    fun getMyStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getActiveStatuses(currentTime: Long = System.currentTimeMillis()): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE expiresAt > :currentTime AND userId != :currentUserId ORDER BY createdAt DESC")
    fun getContactStatuses(currentUserId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<StatusEntity>>

    @Query("UPDATE statuses SET isViewed = 1 WHERE id = :statusId")
    suspend fun markViewed(statusId: String)

    @Query("DELETE FROM statuses WHERE expiresAt <= :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM statuses WHERE lastSyncedAt IS NULL")
    suspend fun getUnsyncedStatuses(): List<StatusEntity>

    @Query("UPDATE statuses SET lastSyncedAt = :timestamp WHERE id = :statusId")
    suspend fun markSynced(statusId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM statuses WHERE id = :statusId")
    suspend fun getById(statusId: String): StatusEntity?

    @Query("DELETE FROM statuses")
    suspend fun deleteAll()
}
