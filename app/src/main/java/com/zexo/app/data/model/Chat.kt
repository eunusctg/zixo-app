package com.zexo.app.data.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSender: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAvatar: String = "",
    val typing: List<String> = emptyList(),
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun getOtherUserId(currentUserId: String): String {
        return participants.firstOrNull { it != currentUserId } ?: ""
    }
}
