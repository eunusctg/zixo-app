package com.zixo.app.domain.model

import java.time.Instant

/**
 * Complete Message Model — Supports single & group messaging with
 * reactions, replies, forwards, multi-tier deletion, and real-time
 * Firebase sync via continuous snapshot listeners.
 */

/**
 * Represents a single message within a chat thread.
 *
 * Messages are stored in Firestore under the thread's sub-collection
 * and synchronized in real-time via [addSnapshotListener]. Every
 * mutation (edit, delete, reaction) triggers an immediate snapshot
 * update that propagates to all connected devices through the
 * active Kotlin StateFlow pipeline.
 */
data class MessageModel(
    val id: String = "",
    val threadId: String = "",
    val senderUid: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L,                    // Epoch milliseconds
    val type: MessageContentType = MessageContentType.TEXT,
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaFileSize: Long = 0L,
    val mediaMimeType: String = "",
    val isRead: Boolean = false,
    val readByUids: Set<String> = emptySet(),    // UIDs of users who have read this message
    val deliveredToUids: Set<String> = emptySet(), // UIDs of users who received this message
    val replyToMessageId: String? = null,        // ID of the message this is replying to
    val replyToPreview: String? = null,          // Preview text of the replied-to message
    val replyToSenderName: String? = null,       // Display name of the sender of the replied message
    val forwardedFromUid: String? = null,        // Original sender UID if this is a forwarded message
    val forwardedFromName: String? = null,       // Original sender display name
    val isForwarded: Boolean = false,
    val reactions: List<MessageReaction> = emptyList(),
    val isDeletedForMe: Boolean = false,         // Soft delete — only hidden for the current user
    val isDeletedForEveryone: Boolean = false,   // Hard delete — removed from all devices
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val ephemeralExpiresAt: Long? = null,        // Epoch ms when this message self-destructs (null = no timer)
    val caption: String? = null                  // Caption for media messages
) {
    /**
     * Whether this message should be rendered as deleted/placeholder.
     * A message deleted for everyone shows a "This message was deleted" placeholder.
     * A message deleted for me is simply hidden from the local user's view.
     */
    val isDeleted: Boolean get() = isDeletedForEveryone || isDeletedForMe

    /**
     * Whether this message has an active ephemeral timer that hasn't expired yet.
     */
    val isEphemeralActive: Boolean
        get() = ephemeralExpiresAt != null && ephemeralExpiresAt!! > System.currentTimeMillis()
}

/**
 * Represents a reaction attached to a message.
 *
 * Reactions support 3D emoji symbols, standard Unicode emojis,
 * and custom sticker references. Each user can only have one
 * active reaction per message — adding a new one replaces the old.
 */
data class MessageReaction(
    val uid: String = "",                        // UID of the user who reacted
    val emoji: String = "",                      // The emoji character or symbol
    val customStickerId: String? = null,         // Optional custom sticker reference
    val timestamp: Long = 0L,                    // When the reaction was added
    val isThreeD: Boolean = false                // Whether this is a 3D emoji variant
)

/**
 * Content type classification for messages.
 */
enum class MessageContentType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO_VOICE,
    AUDIO_FILE,
    FILE,
    LOCATION,
    CONTACT_SHARE,
    STICKER,
    SYSTEM_NOTICE,        // System-generated messages (group created, user joined, etc.)
    DELETED_PLACEHOLDER   // Placeholder for "This message was deleted"
}

/**
 * Represents a chat thread (1-on-1 or group conversation).
 *
 * Thread metadata is stored in a top-level Firestore collection
 * and updated atomically whenever a new message is sent.
 * The [lastMessage] field is denormalized for efficient list rendering.
 */
data class ChatThreadModel(
    val id: String = "",
    val type: ThreadType = ThreadType.SINGLE,
    val participantUids: Set<String> = emptySet(),
    val participantProfiles: Map<String, ThreadParticipant> = emptyMap(),
    val groupName: String? = null,               // Only for group threads
    val groupAvatarUrl: String? = null,          // Only for group threads
    val groupDescription: String? = null,        // Only for group threads
    val groupAdminUids: Set<String> = emptySet(), // Only for group threads
    val createdByUid: String = "",
    val createdAt: Long = 0L,
    val lastMessage: LastMessageInfo? = null,
    val unreadCount: Int = 0,                    // Unread count for the current user
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val ephemeralTimerSeconds: Int = 0,          // Per-thread ephemeral timer override
    val wallpaperUrl: String? = null             // Per-thread wallpaper override
)

/**
 * Thread type classification.
 */
enum class ThreadType {
    SINGLE,     // 1-on-1 conversation
    GROUP       // Group conversation
}

/**
 * Participant profile embedded within a thread for efficient rendering.
 */
data class ThreadParticipant(
    val uid: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val zixoNumber: String = "",
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val joinedAt: Long = 0L,
    val isOnline: Boolean = false
)

/**
 * Participant role within a group thread.
 */
enum class ParticipantRole {
    ADMIN,
    MEMBER
}

/**
 * Denormalized last message info for thread list rendering.
 */
data class LastMessageInfo(
    val senderUid: String = "",
    val senderDisplayName: String = "",
    val content: String = "",
    val type: MessageContentType = MessageContentType.TEXT,
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

/**
 * Message action result from the interactive action menu.
 * Triggered by long-pressing a message in the chat view.
 */
sealed class MessageActionResult {
    data class React(val messageId: String, val emoji: String, val isThreeD: Boolean) : MessageActionResult()
    data class Reply(val messageId: String) : MessageActionResult()
    data class Forward(val messageId: String, val targetThreadIds: List<String>) : MessageActionResult()
    data class DeleteForMe(val messageId: String) : MessageActionResult()
    data class DeleteForEveryone(val messageId: String) : MessageActionResult()
    data class Copy(val messageId: String) : MessageActionResult()
    data class Edit(val messageId: String) : MessageActionResult()
}
