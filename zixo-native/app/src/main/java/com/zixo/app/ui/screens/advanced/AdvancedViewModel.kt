package com.zixo.app.ui.screens.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.SettingsRepository
import com.zixo.app.data.repository.UserRepository
import com.zixo.app.domain.model.AudioProfile
import com.zixo.app.domain.model.MediaCompressionProfile
import com.zixo.app.domain.model.SelfDestructTimer
import com.zixo.app.domain.model.Session
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

data class AdvancedUiState(
    val liveKitUrl: String = "",
    val sipOutboundPrefix: String = "",
    val simulcastEnabled: Boolean = false,
    val forceTurnRelay: Boolean = false,
    val audioProfile: AudioProfile = AudioProfile.HIGH_FIDELITY,
    val selfDestructDefault: SelfDestructTimer = SelfDestructTimer.OFF,
    val appSwitcherPrivacyBlur: Boolean = false,
    val mediaCompressionProfile: MediaCompressionProfile = MediaCompressionProfile.BALANCED,
    val debugLoggingEnabled: Boolean = false,
    val activeSessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val debugLogExportPath: String? = null,
    val error: String? = null
)

@HiltViewModel
class AdvancedViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedUiState())
    val uiState: StateFlow<AdvancedUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    private val liveKitUrlFlow = settingsRepository.liveKitUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val sipOutboundPrefixFlow = settingsRepository.sipOutboundPrefix
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val simulcastEnabledFlow = settingsRepository.simulcastEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val forceTurnRelayFlow = settingsRepository.forceTurnRelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val audioProfileFlow = settingsRepository.audioProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioProfile.HIGH_FIDELITY)

    private val selfDestructDefaultFlow = settingsRepository.selfDestructDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SelfDestructTimer.OFF)

    private val appSwitcherPrivacyBlurFlow = settingsRepository.appSwitcherPrivacyBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val mediaCompressionProfileFlow = settingsRepository.mediaCompressionProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaCompressionProfile.BALANCED)

    private val debugLoggingEnabledFlow = settingsRepository.debugLoggingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val activeSessionsFlow = settingsRepository.getActiveSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(
                liveKitUrlFlow,
                sipOutboundPrefixFlow,
                simulcastEnabledFlow,
                forceTurnRelayFlow,
                audioProfileFlow,
                selfDestructDefaultFlow,
                appSwitcherPrivacyBlurFlow,
                mediaCompressionProfileFlow,
                debugLoggingEnabledFlow,
                activeSessionsFlow,
                _isLoading
            ) { values ->
                _uiState.update { current ->
                    current.copy(
                        liveKitUrl = values[0] as String,
                        sipOutboundPrefix = values[1] as String,
                        simulcastEnabled = values[2] as Boolean,
                        forceTurnRelay = values[3] as Boolean,
                        audioProfile = values[4] as AudioProfile,
                        selfDestructDefault = values[5] as SelfDestructTimer,
                        appSwitcherPrivacyBlur = values[6] as Boolean,
                        mediaCompressionProfile = values[7] as MediaCompressionProfile,
                        debugLoggingEnabled = values[8] as Boolean,
                        activeSessions = values[9] as List<Session>,
                        isLoading = values[10] as Boolean
                    )
                }
            }.collect {}
        }
    }

    // ── LiveKit & SIP ───────────────────────────────────────────────────────

    fun setLiveKitUrl(url: String) {
        viewModelScope.launch { settingsRepository.setLiveKitUrl(url) }
    }

    fun setSipOutboundPrefix(prefix: String) {
        viewModelScope.launch { settingsRepository.setSipOutboundPrefix(prefix) }
    }

    // ── WebRTC ──────────────────────────────────────────────────────────────

    fun setSimulcastEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSimulcastEnabled(enabled) }
    }

    fun setForceTurnRelay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceTurnRelay(enabled) }
    }

    fun setAudioProfile(profile: AudioProfile) {
        viewModelScope.launch { settingsRepository.setAudioProfile(profile) }
    }

    // ── Self-Destruct & Privacy ─────────────────────────────────────────────

    fun setSelfDestructDefault(timer: SelfDestructTimer) {
        viewModelScope.launch { settingsRepository.setSelfDestructDefault(timer) }
    }

    fun setAppSwitcherPrivacyBlur(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppSwitcherPrivacyBlur(enabled) }
    }

    // ── Media ───────────────────────────────────────────────────────────────

    fun setMediaCompressionProfile(profile: MediaCompressionProfile) {
        viewModelScope.launch { settingsRepository.setMediaCompressionProfile(profile) }
    }

    // ── Debug ───────────────────────────────────────────────────────────────

    fun setDebugLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDebugLoggingEnabled(enabled) }
    }

    // ── Session Management ──────────────────────────────────────────────────

    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.revokeSession(sessionId).first()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to revoke session") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Maintenance ─────────────────────────────────────────────────────────

    fun vacuumDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                settingsRepository.vacuumDatabase()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to vacuum database") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportDebugLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.update { it.copy(debugLogExportPath = null, error = null) }
            try {
                val path = settingsRepository.exportDebugLogs().first()
                _uiState.update { it.copy(debugLogExportPath = path) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to export debug logs") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearDebugLogExportPath() {
        _uiState.update { it.copy(debugLogExportPath = null) }
    }
}
