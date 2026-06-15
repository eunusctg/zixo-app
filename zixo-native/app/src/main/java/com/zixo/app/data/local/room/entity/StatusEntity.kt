package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for status caching.
 * Enables offline viewing and auto-expiration cleanup.
 */
@Entity(
    tableName = "statuses",
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["expiresAt"]),
        Index(value = ["isMyStatus"]),
        Index(value = ["lastSyncedAt"])
    ]
)
data class StatusEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val text: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String = "TEXT",
    val backgroundColor: String? = null,
    val fontName: String? = null,
    val visibility: String = "ALL_CONTACTS",
    val viewersJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
    val isViewed: Boolean = false,
    val isMyStatus: Boolean = false,
    val lastSyncedAt: Long? = null
)
