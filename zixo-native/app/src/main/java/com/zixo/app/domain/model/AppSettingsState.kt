package com.zixo.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Absolute decoupled models tracking real-time client settings parameters.
 *
 * Usernames and 8-digit Zixo Numbers are entirely system-generated on account
 * registration and must remain strictly read-only throughout the interface lifecycle.
 */

// ─────────────────────────────────────────────────────────────────────────────
// User Profile
// ─────────────────────────────────────────────────────────────────────────────

data class UserProfile(
    val displayName: String = "",
    val username: String = "",           // System-Generated, Strictly Read-Only
    val zixoNumber: String = "",         // System-Generated 8-Digit Code, Strictly Read-Only
    val avatarUrl: String = "",
    val bio: String = "",
    val phoneNumber: String = ""         // System-Generated / Fixed Verification Bind
) {
    /**
     * Format the 8-digit Zixo number as two 4-digit blocks separated by a space.
     * e.g., "12345678" → "1234 5678"
     */
    val formattedZixoNumber: String
        get() = if (zixoNumber.length == 8) {
            "${zixoNumber.substring(0, 4)} ${zixoNumber.substring(4, 8)}"
        } else {
            zixoNumber
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// App Settings State
// ─────────────────────────────────────────────────────────────────────────────

data class AppSettingsState(
    val userProfile: UserProfile = UserProfile(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isSecurityNotificationsEnabled: Boolean = false,
    val isTwoStepEnabled: Boolean = false,
    val lastSeenVisibility: VisibilityOption = VisibilityOption.EVERYONE,
    val profilePhotoVisibility: VisibilityOption = VisibilityOption.EVERYONE,
    val aboutVisibility: VisibilityOption = VisibilityOption.EVERYONE,
    val statusPrivacy: StatusPrivacyOption = StatusPrivacyOption.ALL_CONTACTS,
    val areReadReceiptsEnabled: Boolean = true,
    val ephemeralDestructTimer: Int = 0,                 // In seconds (0 = Off)
    val isScreenLockEnabled: Boolean = false,
    val protectIpInCalls: Boolean = true,
    val disableLinkPreviews: Boolean = false,
    val enterIsSend: Boolean = false,
    val isMediaVisibilityEnabled: Boolean = true,
    val fontSizeScale: Float = 1.0f,
    val areConversationTonesEnabled: Boolean = true,
    val messageNotificationToneUri: String = "",
    val groupNotificationToneUri: String = "",
    val callRingtoneUri: String = "",
    val videoCallRingtoneUri: String = "",
    val vibrationPattern: VibrationOption = VibrationOption.DEFAULT,
    val autoDownloadMobile: Set<MediaType> = emptySet(),
    val autoDownloadWifi: Set<MediaType> = setOf(MediaType.PHOTO),
    val autoDownloadRoaming: Set<MediaType> = emptySet(),
    val mediaUploadQuality: UploadQuality = UploadQuality.BALANCED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Enumerations
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Application theme modes.
 * DARK: Custom slate/dark emerald color matrix.
 * AMOLED: Absolute pure black (#000000) for power saving on OLED displays.
 * SYSTEM: Automatic binding to Android's configuration changes.
 */
enum class ThemeMode { DARK, AMOLED, SYSTEM }

/**
 * Visibility restriction levels for personal information fields.
 */
enum class VisibilityOption { EVERYONE, CONTACTS, NOBODY }

/**
 * Status privacy sharing granularity.
 * ALL_CONTACTS: Broadcast to every contact on the address book.
 * EXCLUDE_SOME: Share with all contacts except a blocked subset.
 * ONLY_SHARE_WITH: Share only with an explicitly selected subset.
 */
enum class StatusPrivacyOption { ALL_CONTACTS, EXCLUDE_SOME, ONLY_SHARE_WITH }

/**
 * Vibration feedback patterns for incoming notifications.
 */
enum class VibrationOption { OFF, DEFAULT, SHORT, LONG }

/**
 * Downloadable media type categories for auto-download rules.
 */
@Serializable
enum class MediaType { PHOTO, AUDIO, VIDEO, DOCUMENT }

/**
 * Upload compression profiles for outbound media.
 * AUTO: Adaptive quality based on current network conditions.
 * BEST_QUALITY: Minimal compression, highest fidelity transfer.
 * BALANCED: Standard optimized compression with good quality.
 */
enum class UploadQuality { AUTO, BEST_QUALITY, BALANCED }

// ─────────────────────────────────────────────────────────────────────────────
// Call State — WebRTC Engine (Sealed Class for Full Lifecycle)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sealed class representing the full lifecycle of a WebRTC call.
 *
 * All LiveKit session parameters, peer allocations, ICE setups, token
 * collections, and audio track rendering components are isolated in
 * asynchronous background workers running under Dispatchers.IO.
 * The Android Main Thread is NEVER blocked by call operations.
 *
 * When a call triggers, an animated fullscreen overlay pops over the
 * chat interface smoothly using a frosted translucent glass canvas,
 * preventing thread deadlocks or screen failures (black screen fix).
 */
sealed class CallState {
    /** No call is active. */
    data object IDLE : CallState()

    /** An outgoing call has been initiated; waiting for the remote peer to respond. */
    data class DIALING(
        val callId: String = "",
        val targetUid: String = "",
        val targetDisplayName: String = "",
        val targetAvatarUrl: String = "",
        val isVideoCall: Boolean = false,
        val startedAt: Long = System.currentTimeMillis()
    ) : CallState()

    /** An incoming call is ringing on the local device. */
    data class RINGING(
        val callId: String = "",
        val callerUid: String = "",
        val callerDisplayName: String = "",
        val callerAvatarUrl: String = "",
        val isVideoCall: Boolean = false,
        val receivedAt: Long = System.currentTimeMillis()
    ) : CallState()

    /** The call is actively connected with audio/video streams flowing. */
    data class CONNECTED(
        val callId: String = "",
        val targetUid: String = "",
        val targetDisplayName: String = "",
        val targetAvatarUrl: String = "",
        val isVideoCall: Boolean = false,
        val isMuted: Boolean = false,
        val isCameraOff: Boolean = false,
        val isSpeakerOn: Boolean = false,
        val connectedAt: Long = System.currentTimeMillis(),
        val participantCount: Int = 1,
        val durationSeconds: Long = 0L
    ) : CallState()

    /** The call has ended. */
    data class ENDED(
        val callId: String = "",
        val durationSeconds: Long = 0L,
        val endReason: CallEndReason = CallEndReason.COMPLETED
    ) : CallState()
}

/**
 * Reason why a call ended.
 */
enum class CallEndReason {
    COMPLETED,          // Normal hangup
    MISSED,             // Incoming call not answered
    DECLINED,           // Recipient declined the call
    CANCELLED,          // Caller cancelled before answer
    TIMEOUT,            // No answer within the timeout window
    NETWORK_ERROR,      // Connection failed due to network issues
    PERMISSION_DENIED   // Camera/microphone permission was denied
}

// ─────────────────────────────────────────────────────────────────────────────
// Storage Usage Breakdown
// ─────────────────────────────────────────────────────────────────────────────

data class StorageBreakdown(
    val totalBytes: Long = 0L,
    val callsBytes: Long = 0L,
    val messagesBytes: Long = 0L,
    val statusUploadsBytes: Long = 0L,
    val cloudSyncBytes: Long = 0L,
    val mediaBytes: Long = 0L
) {
    val totalMB: Float get() = totalBytes / (1024f * 1024f)
    val callsMB: Float get() = callsBytes / (1024f * 1024f)
    val messagesMB: Float get() = messagesBytes / (1024f * 1024f)
    val statusUploadsMB: Float get() = statusUploadsBytes / (1024f * 1024f)
    val cloudSyncMB: Float get() = cloudSyncBytes / (1024f * 1024f)
    val mediaMB: Float get() = mediaBytes / (1024f * 1024f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Conversation Storage Entry
// ─────────────────────────────────────────────────────────────────────────────

data class ConversationStorageEntry(
    val threadId: String,
    val displayName: String,
    val avatarUrl: String?,
    val storageBytes: Long,
    val isPinned: Boolean = false
) {
    val storageMB: Float get() = storageBytes / (1024f * 1024f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Ephemeral Timer Options
// ─────────────────────────────────────────────────────────────────────────────

enum class EphemeralTimerOption(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    TWENTY_FOUR_HOURS(86400, "24 Hours"),
    SEVEN_DAYS(604800, "7 Days"),
    NINETY_DAYS(7776000, "90 Days")
}

// ─────────────────────────────────────────────────────────────────────────────
// Two-Step Verification State
// ─────────────────────────────────────────────────────────────────────────────

data class TwoStepVerificationState(
    val isPinSet: Boolean = false,
    val pin: String = "",
    val confirmPin: String = "",
    val email: String = "",
    val isSetupMode: Boolean = false,
    val isConfirming: Boolean = false
)
