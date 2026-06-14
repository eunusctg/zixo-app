package com.zixo.app.domain.model

/**
 * Represents a user profile stored in Firestore.
 * All fields are dynamic and fetched from the authenticated user's session state.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val zixoNumber: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val blockedUsers: List<String> = emptyList(),
    val fcmToken: String? = null,
    val createdAt: Long = 0L
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

/**
 * Extended user profile including application settings.
 */
data class UserProfile(
    val user: User = User(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val lastSeenVisibility: LastSeenVisibility = LastSeenVisibility.EVERYONE,
    val onlineStatusEnabled: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val screenLockEnabled: Boolean = false,
    val messagePreviewEnabled: Boolean = true,
    val dndEnabled: Boolean = false,
    val autoDownloadMedia: AutoDownloadMedia = AutoDownloadMedia.WIFI_ONLY,
    val defaultCallType: DefaultCallType = DefaultCallType.ASK_EVERY_TIME,
    val noiseSuppressionEnabled: Boolean = true,
    val simulcastEnabled: Boolean = false,
    val forceTurnRelay: Boolean = false,
    val audioProfile: AudioProfile = AudioProfile.HIGH_FIDELITY,
    val selfDestructDefault: SelfDestructTimer = SelfDestructTimer.OFF,
    val appSwitcherPrivacyBlur: Boolean = false,
    val mediaCompressionProfile: MediaCompressionProfile = MediaCompressionProfile.BALANCED,
    val debugLoggingEnabled: Boolean = false
) {
    fun toUser(): User = user
}
