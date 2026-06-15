package com.zixo.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.zixo.app.data.local.room.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the users Room cache.
 * Reduces Firestore reads for profile data and enables
 * offline profile viewing with TTL-based invalidation.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getByUid(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid")
    fun observeByUid(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE zixoNumber = :zixoNumber")
    suspend fun getByZixoNumber(zixoNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE lastSyncedAt IS NULL OR lastSyncedAt < :threshold")
    suspend fun getStaleUsers(threshold: Long): List<UserEntity>

    @Query("UPDATE users SET lastSeenAt = :lastSeen, isOnline = :isOnline WHERE uid = :uid")
    suspend fun updatePresence(uid: String, lastSeen: Long, isOnline: Boolean)

    @Query("UPDATE users SET displayName = :displayName, photoUrl = :photoUrl, bio = :bio, lastSyncedAt = :syncTime WHERE uid = :uid")
    suspend fun updateProfile(uid: String, displayName: String, photoUrl: String, bio: String, syncTime: Long = System.currentTimeMillis())

    @Query("UPDATE users SET lastSyncedAt = :timestamp WHERE uid = :uid")
    suspend fun markSynced(uid: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM users WHERE lastSyncedAt < :threshold AND uid != :currentUid")
    suspend fun cleanupStale(threshold: Long, currentUid: String): Int

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getCachedUserCount(): Int

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
