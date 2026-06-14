package com.zixo.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.SettingsRepository
import com.zixo.app.data.repository.StorageInfo
import com.zixo.app.data.repository.UserRepository
import com.zixo.app.domain.model.AutoDownloadMedia
import com.zixo.app.domain.model.DefaultCallType
import com.zixo.app.domain.model.FontSize
import com.zixo.app.domain.model.LastSeenVisibility
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentUser: User? = null,
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
    val storageInfo: StorageInfo? = null,
    val isLoading: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    private val currentUserFlow = userRepository.getCurrentUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val themeModeFlow = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val fontSizeFlow = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FontSize.MEDIUM)

    private val lastSeenVisibilityFlow = settingsRepository.lastSeenVisibility
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LastSeenVisibility.EVERYONE)

    private val onlineStatusEnabledFlow = settingsRepository.onlineStatusEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val readReceiptsFlow = settingsRepository.readReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val screenLockEnabledFlow = settingsRepository.screenLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val messagePreviewEnabledFlow = settingsRepository.messagePreviewEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val dndEnabledFlow = settingsRepository.dndEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val autoDownloadMediaFlow = settingsRepository.autoDownloadMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutoDownloadMedia.WIFI_ONLY)

    private val defaultCallTypeFlow = settingsRepository.defaultCallType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultCallType.ASK_EVERY_TIME)

    private val noiseSuppressionEnabledFlow = settingsRepository.noiseSuppressionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            combine(
                currentUserFlow,
                themeModeFlow,
                fontSizeFlow,
                lastSeenVisibilityFlow,
                onlineStatusEnabledFlow,
                readReceiptsFlow,
                screenLockEnabledFlow,
                messagePreviewEnabledFlow,
                dndEnabledFlow,
                autoDownloadMediaFlow,
                defaultCallTypeFlow,
                noiseSuppressionEnabledFlow,
                _isLoading
            ) { values ->
                val currentUser = values[0] as User?
                val themeMode = values[1] as ThemeMode
                val fontSize = values[2] as FontSize
                val lastSeenVisibility = values[3] as LastSeenVisibility
                val onlineStatusEnabled = values[4] as Boolean
                val readReceiptsEnabled = values[5] as Boolean
                val screenLockEnabled = values[6] as Boolean
                val messagePreviewEnabled = values[7] as Boolean
                val dndEnabled = values[8] as Boolean
                val autoDownloadMedia = values[9] as AutoDownloadMedia
                val defaultCallType = values[10] as DefaultCallType
                val noiseSuppressionEnabled = values[11] as Boolean
                val isLoading = values[12] as Boolean

                _uiState.update { current ->
                    current.copy(
                        currentUser = currentUser,
                        themeMode = themeMode,
                        fontSize = fontSize,
                        lastSeenVisibility = lastSeenVisibility,
                        onlineStatusEnabled = onlineStatusEnabled,
                        readReceiptsEnabled = readReceiptsEnabled,
                        screenLockEnabled = screenLockEnabled,
                        messagePreviewEnabled = messagePreviewEnabled,
                        dndEnabled = dndEnabled,
                        autoDownloadMedia = autoDownloadMedia,
                        defaultCallType = defaultCallType,
                        noiseSuppressionEnabled = noiseSuppressionEnabled,
                        isLoading = isLoading
                    )
                }
            }.collect {}
        }

        loadStorageUsage()
    }

    // ── Theme & Appearance ──────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    // ── Privacy ─────────────────────────────────────────────────────────────

    fun setLastSeenVisibility(visibility: LastSeenVisibility) {
        viewModelScope.launch { settingsRepository.setLastSeenVisibility(visibility) }
    }

    fun setOnlineStatusEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOnlineStatusEnabled(enabled) }
    }

    fun setReadReceiptsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReadReceipts(enabled) }
    }

    fun setScreenLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setScreenLockEnabled(enabled) }
    }

    fun setMessagePreviewEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMessagePreviewEnabled(enabled) }
    }

    // ── Notifications & DND ─────────────────────────────────────────────────

    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDndEnabled(enabled) }
    }

    // ── Media ───────────────────────────────────────────────────────────────

    fun setAutoDownloadMedia(mode: AutoDownloadMedia) {
        viewModelScope.launch { settingsRepository.setAutoDownloadMedia(mode) }
    }

    // ── Calling ─────────────────────────────────────────────────────────────

    fun setDefaultCallType(callType: DefaultCallType) {
        viewModelScope.launch { settingsRepository.setDefaultCallType(callType) }
    }

    fun setNoiseSuppressionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNoiseSuppressionEnabled(enabled) }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun dismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    // ── Account Actions ─────────────────────────────────────────────────────

    fun logOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            try {
                authRepository.signOut()
            } catch (_: Exception) {
                // Auth state flow handles the transition
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteDialog = false) }
            try {
                authRepository.deleteAccount()
            } catch (_: Exception) {
                // Auth state flow handles the transition
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Storage ─────────────────────────────────────────────────────────────

    fun loadStorageUsage() {
        viewModelScope.launch {
            try {
                val storageInfo = settingsRepository.getStorageUsage().first()
                _uiState.update { it.copy(storageInfo = storageInfo) }
            } catch (_: Exception) {
                // Storage info remains null on failure
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                settingsRepository.clearCache()
                loadStorageUsage()
            } catch (_: Exception) {
                // Cache cleared as best-effort
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
