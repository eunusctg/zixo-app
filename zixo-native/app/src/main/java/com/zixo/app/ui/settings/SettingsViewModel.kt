package com.zixo.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.domain.model.AppSettingsState
import com.zixo.app.domain.model.ConversationStorageEntry
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StorageBreakdown
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.domain.model.VibrationOption
import com.zixo.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    /**
     * Holds a transient error message that is merged into [settingsState].
     * Cleared (set to null) whenever the next successful operation begins
     * or when [clearError] is called from the UI.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Tracks whether ANY mutation operation is currently in-flight.
     * Merged into [settingsState].isLoading so the UI can display a global
     * progress indicator.
     */
    private val _isLoading = MutableStateFlow(false)

    // ── Primary settings state ──────────────────────────────────────────────

    /**
     * The primary reactive state combining all preferences from
     * [SettingsRepository.settingsFlow] with transient loading/error signals.
     *
     * The underlying [AppSettingsState] emitted by the repository is
     * authoritative for all persisted values; only `isLoading` and
     * `errorMessage` are overridden by the ViewModel's own signals.
     */
    val settingsState: StateFlow<AppSettingsState> = combine(
        settingsRepository.settingsFlow,
        _isLoading,
        _errorMessage
    ) { settings, loading, error ->
        settings.copy(
            isLoading = loading,
            errorMessage = error
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

    // ── Logout state ────────────────────────────────────────────────────────

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)

    /**
     * Tracks the lifecycle of a logout / account deletion operation.
     * The UI should observe this to present confirmation dialogs and
     * navigate to the authentication screen on [LogoutState.Success].
     */
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
     * Reactive per-conversation storage usage entries, sorted for display.
     */
    val conversationStorage: StateFlow<List<ConversationStorageEntry>> =
        settingsRepository.getConversationStorage()
            .catch { throwable ->
                Log.e(TAG, "Error observing conversation storage", throwable)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    // ── Concurrency guard ───────────────────────────────────────────────────

    /**
     * Tracks the currently running mutation job so that overlapping calls
     * are safely ignored rather than creating parallel write races.
     */
    private var mutationJob: Job? = null

    // ── Helper: execute a mutation safely ───────────────────────────────────

    /**
     * Runs [block] on [Dispatchers.IO] inside a structured coroutine.
     * Sets loading/error states around the call and guarantees that
     * `isLoading` is always reset, even on failure.
     */
    private fun runMutation(block: suspend () -> Unit) {
        // Prevent overlapping mutations – the previous one must finish first
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

    // ── Public error consumer ───────────────────────────────────────────────

    /**
     * Clears the current error message on the state.
     * Call this from the UI after the error has been shown to the user.
     */
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

    fun updateAutoDownloadMobile(types: Set<MediaType>) = runMutation {
        settingsRepository.updateAutoDownloadMobile(types)
    }

    fun updateAutoDownloadWifi(types: Set<MediaType>) = runMutation {
        settingsRepository.updateAutoDownloadWifi(types)
    }

    fun updateMediaUploadQuality(quality: UploadQuality) = runMutation {
        settingsRepository.updateMediaUploadQuality(quality)
    }

    /**
     * Clears the application cache directories.
     * On success the [storageBreakdown] flow will automatically emit
     * updated values because the repository re-computes disk usage.
     */
    fun clearCache() = runMutation {
        settingsRepository.clearCache()
    }

    /**
     * Requests a portable account information report.
     * The result is delivered through [settingsState.errorMessage] on failure;
     * on success the repository returns a URL or report string which is
     * logged here for debugging (the UI can listen to repository-specific
     * flows if it needs the actual value).
     */
    fun requestAccountInfo() = runMutation {
        val result = settingsRepository.requestAccountInfo()
        result.fold(
            onSuccess = { report ->
                Log.i(TAG, "Account info report generated: $report")
            },
            onFailure = { throwable ->
                throw throwable  // will be caught by runMutation's try-catch
            }
        )
    }

    /**
     * Permanently deletes the user's account.
     * This is an irreversible operation that removes all Firestore documents
     * and the Firebase Authentication record.
     */
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
    //  Auth — Logout
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Puts the logout flow into the [LogoutState.Confirming] state so the
     * UI can present a confirmation dialog. Does NOT perform the actual
     * logout until [confirmLogout] is called.
     */
    fun requestLogout() {
        _logoutState.value = LogoutState.Confirming
    }

    /**
     * Cancels a pending logout confirmation dialog.
     */
    fun cancelLogout() {
        if (_logoutState.value is LogoutState.Confirming) {
            _logoutState.value = LogoutState.Idle
        }
    }

    /**
     * Executes the sign-out operation after the user has confirmed.
     * The state transitions through [LogoutState.Loading] and resolves
     * to either [LogoutState.Success] or [LogoutState.Error].
     */
    fun confirmLogout() {
        // Only proceed if we're in the Confirming state
        if (_logoutState.value !is LogoutState.Confirming) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _logoutState.value = LogoutState.Loading
                authRepository.signOut()
                _logoutState.value = LogoutState.Success
            } catch (t: Throwable) {
                Log.e(TAG, "Logout failed", t)
                _logoutState.value = LogoutState.Error(
                    t.localizedMessage ?: "Logout failed"
                )
            }
        }
    }

    /**
     * Resets the logout state back to [LogoutState.Idle].
     * Call this after the UI has consumed the terminal state
     * (Success / Error) to prevent re-consumption on configuration changes.
     */
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
