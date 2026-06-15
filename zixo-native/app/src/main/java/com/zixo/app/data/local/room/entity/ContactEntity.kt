package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for contact caching.
 * Enables instant offline contact listing without Firestore round-trips.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["contactUserId"]),
        Index(value = ["contactZixoNumber"]),
        Index(value = ["isMutual"]),
        Index(value = ["isBlocked"]),
        Index(value = ["lastSyncedAt"])
    ]
)
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val contactUserId: String,
    val contactDisplayName: String,
    val contactUsername: String,
    val contactZixoNumber: String,
    val contactAvatarUrl: String,
    val contactBio: String,
    val isMutual: Boolean = false,
    val isVerifiedContact: Boolean = true,
    val isBlocked: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val mutualVerifiedAt: Long? = null,
    val lastSyncedAt: Long? = null
)
