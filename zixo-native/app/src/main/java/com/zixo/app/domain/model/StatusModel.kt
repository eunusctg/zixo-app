package com.zixo.app.domain.model

/**
 * Status Model — Enforces zero-trust status privacy.
 *
 * A user's status updates (text, images, shapes, or 3D emojis) are
 * completely hidden from the public and are securely delivered ONLY
 * to verified profiles registered inside the mutual contact list array.
 * Non-contacts cannot see, preview, or interact with any status content.
 */

/**
 * Represents a single status update posted by a user.
 *
 * Status updates are ephemeral by design — they are automatically
 * removed from the server 24 hours after creation. Delivery is
 * restricted to the poster's verified mutual contacts only.
 */
data class StatusModel(
    val id: String = "",
    val senderUid: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val senderZixoNumber: String = "",
    val type: StatusContentType = StatusContentType.TEXT,
    val textContent: String? = null,
    val mediaUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaThumbnailUrl: String? = null,
    val backgroundColor: String? = null,         // Hex color for text statuses
    val fontName: String? = null,                // Custom font for text statuses
    val shapeType: StatusShapeType? = null,      // Shape overlay type
    val emoji3dCode: String? = null,             // 3D emoji code for 3D emoji statuses
    val caption: String? = null,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,                    // createdAt + 24 hours
    val viewedByUids: Set<String> = emptySet(),  // UIDs of contacts who have viewed this status
    val reactions: List<StatusReaction> = emptyList(),
    val isExpired: Boolean = false
) {
    /**
     * Whether this status has passed its expiration time.
     */
    val isActive: Boolean
        get() = !isExpired && expiresAt > System.currentTimeMillis()

    /**
     * Time remaining before this status expires, in milliseconds.
     */
    val timeRemainingMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
}

/**
 * Content type classification for status updates.
 */
enum class StatusContentType {
    TEXT,           // Plain or styled text
    IMAGE,          // Photo with optional caption
    VIDEO,          // Short video clip
    SHAPE,          // Shape/geometry overlay
    EMOJI_3D        // 3D emoji animation
}

/**
 * Shape overlay types for shape-based statuses.
 */
enum class StatusShapeType {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    STAR,
    HEART,
    DIAMOND,
    HEXAGON
}

/**
 * Represents a reaction to a status update.
 * Only mutual contacts can react to each other's statuses.
 */
data class StatusReaction(
    val uid: String = "",
    val displayName: String = "",
    val emoji: String = "",
    val timestamp: Long = 0L
)

/**
 * Represents a grouped status feed for a single user.
 *
 * All active statuses from the same sender are grouped together
 * so the viewer can swipe through them sequentially, similar
 * to the Stories pattern used by major messaging platforms.
 */
data class StatusGroupModel(
    val senderUid: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val senderZixoNumber: String = "",
    val statuses: List<StatusModel> = emptyList(),
    val hasUnviewedStatuses: Boolean = false,
    val latestTimestamp: Long = 0L
) {
    /**
     * The count of statuses in this group.
     */
    val statusCount: Int get() = statuses.size

    /**
     * The number of statuses the current user has not yet viewed.
     */
    val unviewedCount: Int get() = statuses.count { !it.isExpired }
}

/**
 * My status state — represents the current user's own status feed.
 */
data class MyStatusState(
    val myStatuses: List<StatusModel> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Status privacy configuration for the current user.
 *
 * Enforces the zero-trust model: statuses are ONLY visible to
 * verified mutual contacts. The user can further restrict visibility
 * by excluding specific contacts or only sharing with a selected subset.
 */
data class StatusPrivacyConfig(
    val option: StatusPrivacyOption = StatusPrivacyOption.ALL_CONTACTS,
    val excludedContactUids: Set<String> = emptySet(),    // Used with EXCLUDE_SOME
    val onlyShareWithUids: Set<String> = emptySet()       // Used with ONLY_SHARE_WITH
)
