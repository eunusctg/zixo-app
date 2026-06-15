package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zixo.app.domain.model.ChatThreadModel

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
    val isMuted: Boolean = false,
    val threadType: String = "SINGLE",
    val groupName: String? = null
)

fun ChatEntity.toDomain(): ChatThreadModel = ChatThreadModel(
    id = id,
    type = try { com.zixo.app.domain.model.ThreadType.valueOf(threadType) }
        catch (_: Exception) { com.zixo.app.domain.model.ThreadType.SINGLE },
    participantUids = deserializeUidList(participantUids).toSet(),
    lastMessage = if (lastMessage != null) {
        com.zixo.app.domain.model.LastMessageInfo(
            content = lastMessage,
            timestamp = lastMessageTimestamp ?: 0L
        )
    } else null,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    groupName = groupName
)

fun ChatThreadModel.toEntity(): ChatEntity = ChatEntity(
    id = id,
    participantUids = serializeUidList(participantUids.toList()),
    lastMessage = lastMessage?.content,
    lastMessageTimestamp = lastMessage?.timestamp,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    threadType = type.name,
    groupName = groupName
)

private fun serializeUidList(uids: List<String>): String {
    if (uids.isEmpty()) return "[]"
    return uids.joinToString(prefix = "[", separator = ",", postfix = "]") { uid ->
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
