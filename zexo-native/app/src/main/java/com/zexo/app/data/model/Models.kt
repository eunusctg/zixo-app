package com.zexo.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val username: String = "",
    val bio: String = "",
    val avatar: String = "",
    val phone: String = "",
    val zixoNumber: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L,
    val role: String = "user",
    val fcmToken: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "email" to email,
        "username" to username,
        "bio" to bio,
        "avatar" to avatar,
        "phone" to phone,
        "zixoNumber" to zixoNumber,
        "online" to online,
        "lastSeen" to lastSeen,
        "createdAt" to createdAt,
        "role" to role,
        "fcmToken" to fcmToken
    )
}

@IgnoreExtraProperties
data class Chat(
    @get:Exclude val id: String = "",
    val participants: List<String> = emptyList(),
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAvatar: String = "",
    val lastMessage: String = "",
    val lastMessageSender: String = "",
    val lastMessageTime: Long? = null,
    val unreadCount: Map<String, Int> = emptyMap(),
    val typing: List<String> = emptyList(),
    val pinned: Boolean = false,
    val muted: List<String> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

@IgnoreExtraProperties
data class Message(
    @get:Exclude val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text",
    val timestamp: Long? = null,
    val status: String = "sent",
    val replyTo: String = "",
    val starred: Boolean = false,
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L
)

@IgnoreExtraProperties
data class CallRecord(
    @get:Exclude val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val type: String = "audio",
    val direction: String = "outgoing",
    val duration: Long = 0L,
    val timestamp: Long? = null
)

@IgnoreExtraProperties
data class Status(
    @get:Exclude val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val type: String = "text",
    val content: String = "",
    val mediaUrl: String = "",
    val backgroundColor: String = "#6C5CE7",
    val textColor: String = "#FFFFFF",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val seenBy: List<String> = emptyList()
)

@IgnoreExtraProperties
data class CallSignal(
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val type: String = "audio",
    val status: String = "ringing",
    val offer: String = "",
    val answer: String = "",
    val createdAt: Long = 0L
)

@IgnoreExtraProperties
data class UserSettings(
    val darkTheme: Boolean = true,
    val notifications: Boolean = true,
    val biometricLock: Boolean = false,
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val readReceipts: Boolean = true,
    val typingIndicators: Boolean = true
)

@IgnoreExtraProperties
data class AdminConfig(
    val maintenanceMode: Boolean = false,
    val forceUpdate: Boolean = false,
    val latestVersion: String = "1.3.0",
    val minVersion: String = "1.0.0",
    val message: String = "",
    val registrationEnabled: Boolean = true
)

@IgnoreExtraProperties
data class AppVersion(
    val versionCode: Int = 3,
    val versionName: String = "1.3.0",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false
)
