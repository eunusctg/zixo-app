package com.zixo.app.domain.model

import java.time.Instant

data class ChatThread(
    val id: String,
    val participantUids: List<String>,
    val lastMessage: String?,
    val lastMessageTimestamp: Instant?,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

data class Message(
    val id: String,
    val threadId: String,
    val senderUid: String,
    val content: String,
    val timestamp: Instant,
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}
