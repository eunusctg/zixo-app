package com.zexo.app.data.model

data class MessageReaction(
    val senderZixoNumber: String = "",
    val reactionEmoji: String = ""
)

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderZixoNumber: String = "",
    val text: String = "",
    val type: String = "text",
    val timestamp: Long = 0L,
    val status: String = "sending",
    val replyTo: String = "",
    val replyToTextPreview: String? = null,
    val replyToSenderName: String? = null,
    val forwardedFromZixoNumber: String? = null,
    val isDeletedForEveryone: Boolean = false,
    val deletedForUsers: List<String> = emptyList(),
    val reactions: List<MessageReaction> = emptyList(),
    val starred: Boolean = false,
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L
) {
    fun isOwnMessage(currentUserId: String): Boolean = senderId == currentUserId

    fun isDeletedForUser(currentUserId: String): Boolean =
        isDeletedForEveryone || deletedForUsers.contains(currentUserId)

    val displayText: String
        get() = if (isDeletedForEveryone) "This message was deleted." else text
}
