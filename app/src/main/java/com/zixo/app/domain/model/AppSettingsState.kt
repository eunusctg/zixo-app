package com.zixo.app.domain.model

/** Core user profile model bound to Firestore /users/{uid} */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val zixoNumber: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = 0L,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

/** Auth state sealed class for clean state management */
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
    data object Unauthenticated : AuthState()
}

/** Message data model for chat */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val deletedForUsers: List<String> = emptyList(),
    val isDeletedForEveryone: Boolean = false,
    val reactions: Map<String, String> = emptyMap()
) {
    /** Check if message is deleted for a specific user */
    fun isDeletedForUser(userId: String): Boolean =
        deletedForUsers.contains(userId) || isDeletedForEveryone

    /** Get display text — shows placeholder if deleted for everyone */
    fun displayText(currentUserId: String): String = when {
        isDeletedForEveryone -> "This message was deleted."
        deletedForUsers.contains(currentUserId) -> "This message was deleted."
        else -> text
    }
}

/** Message reaction model */
data class MessageReaction(
    val emoji: String = "",
    val userId: String = "",
    val timestamp: Long = 0L
)

/** Chat / conversation model */
data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Map<String, Int> = emptyMap(),
    val createdAt: Long = 0L,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAvatar: String = ""
)

/** Call record model */
data class CallRecord(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    val type: String = "audio", // audio or video
    val status: String = "missed", // missed, answered, declined
    val isOutgoing: Boolean = false
)

/** App settings persisted in DataStore */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val ringtoneEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val messagePreviewEnabled: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val statusPrivacy: StatusPrivacy = StatusPrivacy.ALL_CONTACTS,
    val blockedUsers: List<String> = emptyList(),
    val notificationTone: String = "",
    val callRingtone: String = ""
)

enum class ThemeMode { DARK, AMOLED, SYSTEM }

enum class StatusPrivacy { ALL_CONTACTS, MY_CONTACTS_EXCEPT, ONLY_SHARE_WITH }

/** Calling state machine — prevents black screen */
sealed class CallState {
    data object Idle : CallState()
    data class Dialing(val callRecord: CallRecord) : CallState()
    data class Active(val callRecord: CallRecord, val isMuted: Boolean = false, val isVideoOn: Boolean = false) : CallState()
    data object Ended : CallState()
}
