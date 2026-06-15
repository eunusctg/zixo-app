package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for user profile caching.
 * Reduces Firestore reads and enables offline profile viewing.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["zixoNumber"]),
        Index(value = ["lastSyncedAt"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val displayName: String,
    val username: String,
    val zixoNumber: String,
    val photoUrl: String,
    val bio: String,
    val phoneNumber: String? = null,
    val hasPasskey: Boolean = false,
    val passkeyCredentialId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastSyncedAt: Long? = null
)
