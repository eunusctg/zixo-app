package com.zixo.app.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching chat messages locally.
 * Enables offline reading, full-text search, and paginated queries
 * without requiring Firestore round-trips.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chatId", "createdAt"]),
        Index(value = ["chatId", "senderId"]),
        Index(value = ["senderId"]),
        Index(value = ["syncedAt"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String,
    val content: String,
    val messageType: String,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val replyToId: String? = null,
    val forwardedFrom: String? = null,
    val reactionsJson: String? = null,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val isDeletedForMe: Boolean = false,
    val isDeletedForEveryone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val syncedAt: Long? = null
)
