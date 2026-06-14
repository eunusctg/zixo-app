package com.zixo.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.SettingsRepository
import com.zixo.app.data.repository.SettingsRepositoryImpl
import com.zixo.app.data.repository.StorageInfo
import com.zixo.app.data.repository.UserRepository
import com.zixo.app.domain.model.AutoDownloadMedia
import com.zixo.app.domain.model.ConversationStorageEntry
import com.zixo.app.domain.model.DefaultCallType
import com.zixo.app.domain.model.FontSize
import com.zixo.app.domain.model.LastSeenVisibility
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StorageBreakdown
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.domain.model.User
import com.zixo.app.domain.model.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    val showDeleteDialog: Boolean = false,
    // ── Chat Configuration
    val enterIsSend: Boolean = true,
    val isMediaVisibilityEnabled: Boolean = true,
    val fontSizeScale: Float = 1.0f,
    // ── Notification Configuration
    val areConversationTonesEnabled: Boolean = true,
    val messageNotificationToneUri: String = "",
    val groupNotificationToneUri: String = "",
    val callRingtoneUri: String = "",
    val videoCallRingtoneUri: String = "",
    val vibrationPattern: VibrationPattern = VibrationPattern.DEFAULT
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val domainSettingsRepository: SettingsRepositoryImpl
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

    private val enterIsSendFlow = settingsRepository.enterIsSend
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val isMediaVisibilityEnabledFlow = settingsRepository.isMediaVisibilityEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val fontSizeScaleFlow = settingsRepository.fontSizeScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1.0f)

    private val areConversationTonesEnabledFlow = settingsRepository.areConversationTonesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val messageNotificationToneUriFlow = settingsRepository.notificationTone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val groupNotificationToneUriFlow = settingsRepository.groupNotificationToneUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val callRingtoneUriFlow = settingsRepository.callRingtoneUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val videoCallRingtoneUriFlow = settingsRepository.videoCallRingtoneUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val vibrationPatternFlow = settingsRepository.vibrationPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VibrationPattern.DEFAULT)

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
                enterIsSendFlow,
                isMediaVisibilityEnabledFlow,
                fontSizeScaleFlow,
                areConversationTonesEnabledFlow,
                messageNotificationToneUriFlow,
                groupNotificationToneUriFlow,
                callRingtoneUriFlow,
                videoCallRingtoneUriFlow,
                vibrationPatternFlow,
                _isLoading
            ) { values ->
                _uiState.update { current ->
                    current.copy(
                        currentUser = values[0] as User?,
                        themeMode = values[1] as ThemeMode,
                        fontSize = values[2] as FontSize,
                        lastSeenVisibility = values[3] as LastSeenVisibility,
                        onlineStatusEnabled = values[4] as Boolean,
                        readReceiptsEnabled = values[5] as Boolean,
                        screenLockEnabled = values[6] as Boolean,
                        messagePreviewEnabled = values[7] as Boolean,
                        dndEnabled = values[8] as Boolean,
                        autoDownloadMedia = values[9] as AutoDownloadMedia,
                        defaultCallType = values[10] as DefaultCallType,
                        noiseSuppressionEnabled = values[11] as Boolean,
                        enterIsSend = values[12] as Boolean,
                        isMediaVisibilityEnabled = values[13] as Boolean,
                        fontSizeScale = values[14] as Float,
                        areConversationTonesEnabled = values[15] as Boolean,
                        messageNotificationToneUri = values[16] as String,
                        groupNotificationToneUri = values[17] as String,
                        callRingtoneUri = values[18] as String,
                        videoCallRingtoneUri = values[19] as String,
                        vibrationPattern = values[20] as VibrationPattern,
                        isLoading = values[21] as Boolean
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

    // ── Chat Configuration ──────────────────────────────────────────────────

    fun setEnterIsSend(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEnterIsSend(enabled) }
    }

    fun setMediaVisibilityEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMediaVisibilityEnabled(enabled) }
    }

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontSizeScale(scale) }
    }

    // ── Notification Configuration ──────────────────────────────────────────

    fun setConversationTonesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setConversationTonesEnabled(enabled) }
    }

    fun setMessageNotificationToneUri(uri: String) {
        viewModelScope.launch { settingsRepository.setNotificationTone(uri) }
    }

    fun setGroupNotificationToneUri(uri: String) {
        viewModelScope.launch { settingsRepository.setGroupNotificationToneUri(uri) }
    }

    fun setCallRingtoneUri(uri: String) {
        viewModelScope.launch { settingsRepository.setCallRingtoneUri(uri) }
    }

    fun setVideoCallRingtoneUri(uri: String) {
        viewModelScope.launch { settingsRepository.setVideoCallRingtoneUri(uri) }
    }

    fun setVibrationPattern(pattern: VibrationPattern) {
        viewModelScope.launch { settingsRepository.setVibrationPattern(pattern) }
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

    /**
     * Requests a GDPR-style account data export report.
     * The report is prepared server-side and delivered to the user's registered email.
     */
    fun requestAccountInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                authRepository.requestAccountInfo()
            } catch (_: Exception) {
                // Caller handles error via return flow
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

    // ── Storage & Data Hub ────────────────────────────────────────────────────

    val storageBreakdown: StateFlow<StorageBreakdown> =
        domainSettingsRepository.getStorageBreakdown()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageBreakdown())

    val conversationStorage: StateFlow<List<ConversationStorageEntry>> =
        domainSettingsRepository.getConversationStorage()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val autoDownloadMobile: StateFlow<Set<MediaType>> =
        domainSettingsRepository.settingsFlow
            .map { it.autoDownloadMobile }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val autoDownloadWifi: StateFlow<Set<MediaType>> =
        domainSettingsRepository.settingsFlow
            .map { it.autoDownloadWifi }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), setOf(MediaType.PHOTO))

    val mediaUploadQuality: StateFlow<UploadQuality> =
        domainSettingsRepository.settingsFlow
            .map { it.mediaUploadQuality }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UploadQuality.AUTO)

    fun updateAutoDownloadMobile(types: Set<MediaType>) {
        viewModelScope.launch { domainSettingsRepository.updateAutoDownloadMobile(types) }
    }

    fun updateAutoDownloadWifi(types: Set<MediaType>) {
        viewModelScope.launch { domainSettingsRepository.updateAutoDownloadWifi(types) }
    }

    fun updateMediaUploadQuality(quality: UploadQuality) {
        viewModelScope.launch { domainSettingsRepository.updateMediaUploadQuality(quality) }
    }
}
