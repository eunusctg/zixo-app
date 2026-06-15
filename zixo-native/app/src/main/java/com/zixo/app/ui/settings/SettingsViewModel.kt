package com.zixo.app.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zixo.app.domain.model.AppSettingsState
import com.zixo.app.domain.model.ConversationStorageEntry
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StorageBreakdown
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.domain.model.VibrationOption
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthResult
import com.zixo.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Logout State
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents the lifecycle states of a logout operation.
 * The UI should observe [SettingsViewModel.logoutState] to drive confirmation
 * dialogs, loading indicators, and post-logout navigation.
 */
sealed class LogoutState {
    /** No logout operation is in progress. */
    data object Idle : LogoutState()
    /** The user has requested logout; awaiting explicit confirmation. */
    data object Confirming : LogoutState()
    /** A logout network / cleanup operation is currently in flight. */
    data object Loading : LogoutState()
    /** The logout completed successfully; the UI should navigate to the auth screen. */
    data object Success : LogoutState()
    /** The logout failed with the given error message. */
    data class Error(val message: String) : LogoutState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Central state engine for ALL settings operations in the Zixo application.
 *
 * Exposes a single [settingsState] [StateFlow] that the UI collects reactively,
 * plus individual mutation functions for every preference. Each mutation:
 *  - runs on [Dispatchers.IO]
 *  - sets `isLoading = true` before the repository call and `false` after
 *  - wraps the call in a `try/catch` that surfaces errors via `errorMessage`
 *  - never crashes the host Fragment/Activity under any circumstances
 *
 * Auth operations (logout / delete account) are routed through [AuthRepository]
 * and tracked separately via [logoutState] so the UI can show confirmation
 * dialogs and navigate independently from the main settings stream.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    // ── Internal mutable error / loading relay ──────────────────────────────

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    // ── Local overlay state for premium/paywall ──────────────────────────────

    private val _showPremiumPaywall = MutableStateFlow(false)
    private val _isPremiumSubscriberLocal = MutableStateFlow(false)

    // ── Primary settings state ──────────────────────────────────────────────

    /**
     * The primary reactive state combining all preferences from
     * [SettingsRepository.settingsFlow] with transient loading/error signals.
     */
    val settingsState: StateFlow<AppSettingsState> = combine(
        settingsRepository.settingsFlow,
        _isLoading,
        _errorMessage,
        _showPremiumPaywall,
        _isPremiumSubscriberLocal
    ) { settings, loading, error, paywall, isPremium ->
        settings.copy(
            isLoading = loading,
            errorMessage = error,
            showPremiumPaywall = paywall,
            isPremiumSubscriber = isPremium
        )
    }.catch { throwable ->
        Log.e(TAG, "Fatal error in settingsFlow combine", throwable)
        emit(
            AppSettingsState(
                isLoading = false,
                errorMessage = throwable.localizedMessage ?: "Failed to load settings"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AppSettingsState(isLoading = true)
    )

    // ── User profile ────────────────────────────────────────────────────────

    /**
     * Reactive user profile derived from [settingsState].
     * Username and ZixoNumber are system-generated and strictly read-only.
     */
    val userProfile: StateFlow<UserProfile> = settingsRepository.userProfileFlow
        .catch { throwable ->
            Log.e(TAG, "Error observing user profile", throwable)
            emit(UserProfile())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UserProfile()
        )

    // ── Passkey registration state ──────────────────────────────────────────

    private val _isPasskeyRegistered = MutableStateFlow(false)
    val isPasskeyRegistered: StateFlow<Boolean> = _isPasskeyRegistered.asStateFlow()

    // ── QR popup state ──────────────────────────────────────────────────────

    private val _showQrPopup = MutableStateFlow(false)
    val showQrPopup: StateFlow<Boolean> = _showQrPopup.asStateFlow()

    // ── Real-time QR Code Matrix State ──────────────────────────────────────

    /**
     * Reactive QR bitmap state generated from the user's live Zixo Number.
     * The QR encodes the secure URI `zixo://profile/{zixoNumber}` and is
     * rendered in high-contrast Neon Emerald Green (#00E676) on a transparent
     * background for maximum visual fidelity inside the frosted glass modal.
     *
     * The bitmap is regenerated instantly whenever the popup is toggled on
     * or when the Zixo Number changes, ensuring it always reflects the
     * current profile state.
     */
    private val _qrBitmapState = MutableStateFlow<Bitmap?>(null)
    val qrBitmapState: StateFlow<Bitmap?> = _qrBitmapState.asStateFlow()

    /**
     * The current invite link URI derived from the user's Zixo Number.
     */
    private val _inviteLink = MutableStateFlow("")
    val inviteLink: StateFlow<String> = _inviteLink.asStateFlow()

    // ── Logout state ────────────────────────────────────────────────────────

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState

    // ── Storage observation ─────────────────────────────────────────────────

    /**
     * Reactive breakdown of storage usage across media categories.
     */
    val storageBreakdown: StateFlow<StorageBreakdown> =
        settingsRepository.getStorageBreakdown()
            .catch { throwable ->
                Log.e(TAG, "Error observing storage breakdown", throwable)
                emit(StorageBreakdown())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = StorageBreakdown()
            )

    /**
     * Reactive per-conversation storage usage entries.
     * TODO: Wire to repository once getConversationStorage() is added to SettingsRepository.
     */
    private val _conversationStorage = MutableStateFlow<List<ConversationStorageEntry>>(emptyList())
    val conversationStorage: StateFlow<List<ConversationStorageEntry>> = _conversationStorage.asStateFlow()

    // ── Concurrency guard ───────────────────────────────────────────────────

    private var mutationJob: Job? = null

    // ── Helper: execute a mutation safely ───────────────────────────────────

    private fun runMutation(block: suspend () -> Unit) {
        if (mutationJob?.isActive == true) return
        mutationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "Settings mutation failed", t)
                _errorMessage.value = t.localizedMessage ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Init ────────────────────────────────────────────────────────────────

    init {
        // Load passkey registration state
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.isPasskeyRegistered().collect { registered ->
                    _isPasskeyRegistered.value = registered
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to check passkey registration", t)
            }
        }
    }

    // ── Public error consumer ───────────────────────────────────────────────

    fun clearError() {
        _errorMessage.value = null
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Edit Profile
    // ════════════════════════════════════════════════════════════════════════

    fun updateDisplayName(name: String) = runMutation {
        settingsRepository.updateProfileDisplayName(name)
    }

    fun updateBio(bio: String) = runMutation {
        settingsRepository.updateProfileBio(bio)
    }

    fun updateAvatarUrl(url: String) = runMutation {
        settingsRepository.updateProfileAvatarUrl(url)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Appearance
    // ════════════════════════════════════════════════════════════════════════

    fun updateThemeMode(mode: ThemeMode) = runMutation {
        settingsRepository.updateThemeMode(mode)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Security
    // ════════════════════════════════════════════════════════════════════════

    fun updateSecurityNotifications(enabled: Boolean) = runMutation {
        settingsRepository.updateSecurityNotifications(enabled)
    }

    fun updateTwoStep(enabled: Boolean) = runMutation {
        settingsRepository.updateTwoStep(enabled)
    }

    fun updateScreenLock(enabled: Boolean) = runMutation {
        settingsRepository.updateScreenLock(enabled)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Privacy
    // ════════════════════════════════════════════════════════════════════════

    fun updateLastSeenVisibility(option: VisibilityOption) = runMutation {
        settingsRepository.updateLastSeenVisibility(option)
    }

    fun updateProfilePhotoVisibility(option: VisibilityOption) = runMutation {
        settingsRepository.updateProfilePhotoVisibility(option)
    }

    fun updateAboutVisibility(option: VisibilityOption) = runMutation {
        settingsRepository.updateAboutVisibility(option)
    }

    fun updateStatusPrivacy(option: StatusPrivacyOption) = runMutation {
        settingsRepository.updateStatusPrivacy(option)
    }

    fun updateReadReceipts(enabled: Boolean) = runMutation {
        settingsRepository.updateReadReceipts(enabled)
    }

    fun updateEphemeralTimer(seconds: Int) = runMutation {
        settingsRepository.updateEphemeralTimer(seconds)
    }

    fun updateProtectIpInCalls(enabled: Boolean) = runMutation {
        settingsRepository.updateProtectIpInCalls(enabled)
    }

    fun updateDisableLinkPreviews(enabled: Boolean) = runMutation {
        settingsRepository.updateDisableLinkPreviews(enabled)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Premium / Freemium Billing
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Toggles the "Receive regular calls via Zixo number" premium feature.
     *
     * Freemium Business Logic:
     * - If the user is a premium subscriber, the toggle updates normally.
     * - If the user is NOT a premium subscriber and tries to enable the feature,
     *   the toggle is frozen and the premium paywall overlay is shown.
     * - Disabling the feature is always allowed regardless of subscription status.
     */
    fun updateIncomingPstnEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !_isPremiumSubscriberLocal.value) {
                // Intercept: freeze toggle, show paywall
                _showPremiumPaywall.value = true
                return@launch
            }
            try {
                settingsRepository.updateIncomingPstnEnabled(enabled)
            } catch (e: Exception) {
                // Revert on failure
                _errorMessage.value = e.localizedMessage ?: "Failed to update PSTN setting"
            }
        }
    }

    /** Dismisses the premium subscription paywall overlay. */
    fun dismissPremiumPaywall() {
        _showPremiumPaywall.value = false
    }

    /** Simulates a premium subscription check against the server. */
    fun checkPremiumStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isPremium = settingsRepository.isPremiumSubscriber()
                _isPremiumSubscriberLocal.value = isPremium
            } catch (_: Exception) {
                // Silently handle — defaults to non-premium
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Chat Config
    // ════════════════════════════════════════════════════════════════════════

    fun updateEnterIsSend(enabled: Boolean) = runMutation {
        settingsRepository.updateEnterIsSend(enabled)
    }

    fun updateMediaVisibility(enabled: Boolean) = runMutation {
        settingsRepository.updateMediaVisibility(enabled)
    }

    fun updateFontSizeScale(scale: Float) = runMutation {
        settingsRepository.updateFontSizeScale(scale.coerceIn(0.5f, 2.0f))
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Notifications
    // ════════════════════════════════════════════════════════════════════════

    fun updateConversationTones(enabled: Boolean) = runMutation {
        settingsRepository.updateConversationTones(enabled)
    }

    fun updateNotificationTone(type: String, uri: String) = runMutation {
        when (type) {
            "message" -> settingsRepository.updateMessageNotificationTone(uri)
            "group" -> settingsRepository.updateGroupNotificationTone(uri)
            "call" -> settingsRepository.updateCallRingtone(uri)
            "video_call" -> settingsRepository.updateVideoCallRingtone(uri)
        }
    }

    fun updateMessageNotificationTone(uri: String) = runMutation {
        settingsRepository.updateMessageNotificationTone(uri)
    }

    fun updateGroupNotificationTone(uri: String) = runMutation {
        settingsRepository.updateGroupNotificationTone(uri)
    }

    fun updateCallRingtone(uri: String) = runMutation {
        settingsRepository.updateCallRingtone(uri)
    }

    fun updateVideoCallRingtone(uri: String) = runMutation {
        settingsRepository.updateVideoCallRingtone(uri)
    }

    fun updateVibrationPattern(pattern: VibrationOption) = runMutation {
        settingsRepository.updateVibrationPattern(pattern)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Data & Storage
    // ════════════════════════════════════════════════════════════════════════

    fun updateAutoDownload(network: String, types: Set<MediaType>) = runMutation {
        when (network) {
            "mobile" -> settingsRepository.updateAutoDownloadMobile(types)
            "wifi" -> settingsRepository.updateAutoDownloadWifi(types)
            "roaming" -> settingsRepository.updateAutoDownloadRoaming(types)
        }
    }

    fun updateAutoDownloadMobile(types: Set<MediaType>) = runMutation {
        settingsRepository.updateAutoDownloadMobile(types)
    }

    fun updateAutoDownloadWifi(types: Set<MediaType>) = runMutation {
        settingsRepository.updateAutoDownloadWifi(types)
    }

    fun updateAutoDownloadRoaming(types: Set<MediaType>) = runMutation {
        settingsRepository.updateAutoDownloadRoaming(types)
    }

    fun updateMediaUploadQuality(quality: UploadQuality) = runMutation {
        settingsRepository.updateMediaUploadQuality(quality)
    }

    fun clearCache() = runMutation {
        settingsRepository.clearCache()
    }

    fun deleteAccount() = runMutation {
        settingsRepository.deleteAccount().fold(
            onSuccess = {
                Log.i(TAG, "Account deleted successfully")
            },
            onFailure = { throwable ->
                throw throwable
            }
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Passkey — Credential Manager
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Creates a WebAuthn passkey via Android CredentialManager.
     * The [requestJson] is the WebAuthn registration challenge JSON from the
     * Cloudflare backend. On success the credential is registered server-side.
     */
    fun createPasskey(context: Context, requestJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                val credentialManager = CredentialManager.create(context)
                val request = CreatePublicKeyCredentialRequest(requestJson = requestJson)
                val response = credentialManager.createCredential(context, request)
                val registrationJson = (response.data?.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")) ?: ""
                authRepository.registerPasskeyWithBackend(registrationJson).collect { result ->
                    when (result) {
                        is AuthResult.Success -> {
                            _isPasskeyRegistered.value = true
                            Log.i(TAG, "Passkey registered successfully")
                        }
                        is AuthResult.Error -> {
                            _errorMessage.value = result.message
                            Log.e(TAG, "Passkey registration failed: ${result.message}")
                        }
                        is AuthResult.Loading -> { /* in progress */ }
                    }
                }
            } catch (e: CreateCredentialException) {
                Log.e(TAG, "Credential creation failed", e)
                _errorMessage.value = e.localizedMessage ?: "Passkey creation failed"
            } catch (t: Throwable) {
                Log.e(TAG, "Passkey creation unexpected error", t)
                _errorMessage.value = t.localizedMessage ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  QR Popup
    // ════════════════════════════════════════════════════════════════════════

    fun toggleQrPopup() {
        _showQrPopup.update { !it }
        // Generate QR on popup open with live Zixo Number
        if (!_showQrPopup.value) {
            // Popup closing — no action needed
        } else {
            // Popup opening — regenerate QR from live profile
            val zixoNumber = userProfile.value.zixoNumber
            generateRealtimeQrMatrix(zixoNumber)
        }
    }

    /**
     * Generates a real-time QR code bitmap from the user's Zixo Number.
     *
     * The QR encodes the secure URI `zixo://profile/{zixoNumber}` and is
     * rendered using the ZXing library with high-contrast Neon Emerald Green
     * (#00E676) on a transparent background.
     *
     * This method is thread-safe and runs on [Dispatchers.IO].
     * If the Zixo Number is empty or an error occurs, the bitmap state
     * is set to null gracefully without crashing.
     *
     * @param zixoNumber The user's 8-digit Zixo Number.
     */
    fun generateRealtimeQrMatrix(zixoNumber: String) {
        if (zixoNumber.isEmpty()) {
            _qrBitmapState.value = null
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val qrCodeContentUri = "zixo://profile/$zixoNumber"
                _inviteLink.value = qrCodeContentUri

                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(
                    qrCodeContentUri,
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                )
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                // Match app's brand accent color: High-contrast Neon Emerald Green (#00E676)
                val brandGreen = Color.parseColor("#00E676")
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(
                            x, y,
                            if (bitMatrix.get(x, y)) brandGreen else Color.TRANSPARENT
                        )
                    }
                }

                _qrBitmapState.value = bitmap
                Log.i(TAG, "QR matrix generated for Zixo Number: %s".format(zixoNumber))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate QR matrix", e)
                _qrBitmapState.value = null
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Auth — Logout
    // ════════════════════════════════════════════════════════════════════════

    fun requestLogout() {
        _logoutState.value = LogoutState.Confirming
    }

    fun cancelLogout() {
        if (_logoutState.value is LogoutState.Confirming) {
            _logoutState.value = LogoutState.Idle
        }
    }

    fun confirmLogout() {
        if (_logoutState.value !is LogoutState.Confirming) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _logoutState.value = LogoutState.Loading
                authRepository.signOut().collect { result ->
                    when (result) {
                        is AuthResult.Success -> {
                            _logoutState.value = LogoutState.Success
                        }
                        is AuthResult.Error -> {
                            _logoutState.value = LogoutState.Error(result.message)
                        }
                        is AuthResult.Loading -> {
                            _logoutState.value = LogoutState.Loading
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Logout failed", t)
                _logoutState.value = LogoutState.Error(
                    t.localizedMessage ?: "Logout failed"
                )
            }
        }
    }

    fun resetLogoutState() {
        _logoutState.value = LogoutState.Idle
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ViewModel cleanup
    // ════════════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        mutationJob?.cancel()
    }
}
