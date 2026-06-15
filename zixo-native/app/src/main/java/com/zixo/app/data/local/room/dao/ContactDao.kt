package com.zixo.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.zixo.app.data.local.room.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the contacts Room cache.
 * Enables instant offline contact listing, sorting, and filtering
 * without Firestore round-trips.
 */
@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    @Upsert
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM contacts WHERE userId = :uid ORDER BY isPinned DESC, contactDisplayName ASC")
    fun getAllContacts(uid: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE contactUserId = :contactUserId")
    suspend fun getByContactUserId(contactUserId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE contactZixoNumber = :zixoNumber")
    suspend fun getByZixoNumber(zixoNumber: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE isMutual = 1 ORDER BY contactDisplayName ASC")
    fun getMutualContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isBlocked = 1 ORDER BY contactDisplayName ASC")
    fun getBlockedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE contactDisplayName LIKE '%' || :query || '%' OR contactUsername LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE contactUserId = :contactUserId")
    suspend fun setBlocked(contactUserId: String, blocked: Boolean)

    @Query("UPDATE contacts SET isPinned = :pinned WHERE contactUserId = :contactUserId")
    suspend fun setPinned(contactUserId: String, pinned: Boolean)

    @Query("UPDATE contacts SET isMuted = :muted WHERE contactUserId = :contactUserId")
    suspend fun setMuted(contactUserId: String, muted: Boolean)

    @Query("UPDATE contacts SET isMutual = :isMutual, mutualVerifiedAt = :verifiedAt WHERE contactUserId = :contactUserId")
    suspend fun setMutual(contactUserId: String, isMutual: Boolean, verifiedAt: Long? = null)

    @Query("SELECT COUNT(*) FROM contacts WHERE isMutual = 1")
    fun getMutualContactCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM contacts WHERE isBlocked = 1")
    fun getBlockedContactCount(): Flow<Int>

    @Query("SELECT * FROM contacts WHERE lastSyncedAt IS NULL OR lastSyncedAt < :threshold")
    suspend fun getStaleContacts(threshold: Long): List<ContactEntity>

    @Query("UPDATE contacts SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun markSynced(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
