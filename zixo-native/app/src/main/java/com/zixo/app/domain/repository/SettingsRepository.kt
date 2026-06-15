package com.zixo.app.domain.repository

import com.zixo.app.domain.model.AppSettingsState
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StorageBreakdown
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.domain.model.VibrationOption
import com.zixo.app.domain.model.StatusPrivacyOption
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the settings domain layer.
 *
 * Exposes a unified [Flow] of [AppSettingsState] for reactive UI consumption,
 * granular update suspend functions for each setting, profile mutation methods
 * (read-only fields like username/zixoNumber are excluded by design),
 * storage analytics, and account lifecycle operations.
 */
interface SettingsRepository {

    // ── Aggregated State ──────────────────────────────────────────────────────

    /**
     * Emits the combined application settings state.
     * Merges local DataStore preferences with the remote user profile.
     */
    val settingsFlow: Flow<AppSettingsState>

    /**
     * Emits the current user profile from Firestore.
     * Username and ZixoNumber are system-generated and strictly read-only.
     */
    val userProfileFlow: Flow<UserProfile>

    // ── Theme & Appearance ────────────────────────────────────────────────────

    suspend fun updateThemeMode(mode: ThemeMode)

    // ── Security ──────────────────────────────────────────────────────────────

    suspend fun updateSecurityNotifications(enabled: Boolean)
    suspend fun updateTwoStep(enabled: Boolean)

    // ── Privacy ───────────────────────────────────────────────────────────────

    suspend fun updateLastSeenVisibility(option: VisibilityOption)
    suspend fun updateProfilePhotoVisibility(option: VisibilityOption)
    suspend fun updateAboutVisibility(option: VisibilityOption)
    suspend fun updateStatusPrivacy(option: StatusPrivacyOption)
    suspend fun updateReadReceipts(enabled: Boolean)

    // ── Ephemeral / Disappearing Messages ─────────────────────────────────────

    suspend fun updateEphemeralTimer(seconds: Int)

    // ── Security & Advanced ───────────────────────────────────────────────────

    suspend fun updateScreenLock(enabled: Boolean)
    suspend fun updateProtectIpInCalls(enabled: Boolean)
    suspend fun updateDisableLinkPreviews(enabled: Boolean)

    // ── Premium / Freemium Billing ───────────────────────────────────────────

    suspend fun updateIncomingPstnEnabled(enabled: Boolean)
    suspend fun isPremiumSubscriber(): Boolean

    // ── Chat Behavior ─────────────────────────────────────────────────────────

    suspend fun updateEnterIsSend(enabled: Boolean)
    suspend fun updateMediaVisibility(enabled: Boolean)
    suspend fun updateFontSizeScale(scale: Float)

    // ── Notifications & Tones ─────────────────────────────────────────────────

    suspend fun updateConversationTones(enabled: Boolean)
    suspend fun updateMessageNotificationTone(uri: String)
    suspend fun updateGroupNotificationTone(uri: String)
    suspend fun updateCallRingtone(uri: String)
    suspend fun updateVideoCallRingtone(uri: String)
    suspend fun updateVibrationPattern(pattern: VibrationOption)

    // ── Media Auto-Download ───────────────────────────────────────────────────

    suspend fun updateAutoDownloadMobile(types: Set<MediaType>)
    suspend fun updateAutoDownloadWifi(types: Set<MediaType>)
    suspend fun updateAutoDownloadRoaming(types: Set<MediaType>)

    // ── Media Upload Quality ──────────────────────────────────────────────────

    suspend fun updateMediaUploadQuality(quality: UploadQuality)

    // ── Profile Mutations (writable fields only) ──────────────────────────────

    suspend fun updateProfileDisplayName(name: String)
    suspend fun updateProfileBio(bio: String)
    suspend fun updateProfileAvatarUrl(url: String)

    // ── Storage Analytics ─────────────────────────────────────────────────────

    fun getStorageBreakdown(): Flow<StorageBreakdown>

    // ── Maintenance ───────────────────────────────────────────────────────────

    suspend fun clearCache()

    // ── Account Lifecycle ─────────────────────────────────────────────────────

    suspend fun requestAccountInfo(): Result<String>
    suspend fun deleteAccount(): Result<Unit>
}
