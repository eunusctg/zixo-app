package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zixo.app.domain.model.ChatThread
import java.time.Instant

@Entity(
    tableName = "chat_threads",
    indices = [
        Index(value = ["participantUids"], name = "index_chat_threads_participant_uids"),
        Index(value = ["lastMessageTimestamp"], name = "index_chat_threads_last_message_timestamp")
    ]
)
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val participantUids: String, // JSON-serialized list of UIDs
    val lastMessage: String?,
    val lastMessageTimestamp: Long?, // Epoch millis
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

fun ChatEntity.toDomain(): ChatThread = ChatThread(
    id = id,
    participantUids = deserializeUidList(participantUids),
    lastMessage = lastMessage,
    lastMessageTimestamp = lastMessageTimestamp?.let { Instant.ofEpochMilli(it) },
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted
)

fun ChatThread.toEntity(): ChatEntity = ChatEntity(
    id = id,
    participantUids = serializeUidList(participantUids),
    lastMessage = lastMessage,
    lastMessageTimestamp = lastMessageTimestamp?.toEpochMilli(),
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted
)

private fun serializeUidList(uids: List<String>): String {
    if (uids.isEmpty()) return "[]"
    return uids.joinToString(prefix = "[", separator = ",", suffix = "]") { uid ->
        "\"$uid\""
    }
}

private fun deserializeUidList(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return json
        .removeSurrounding("[", "]")
        .split(",")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotEmpty() }
}
