package com.zixo.app.ui.screens.settings

import android.content.Context
import android.os.StatFs
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.local.PreferencesManager
import com.zixo.app.data.model.AppSettings
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val showLogoutConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isLoggingOut: Boolean = false,
    val error: String? = null,
    val storageUsed: String = "Calculating..."
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<ZixoUser?> = authRepository.currentUser

    val settings: StateFlow<AppSettings> = preferencesManager.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    init {
        calculateStorageUsage()
    }

    private fun calculateStorageUsage() {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                val appDir = context.filesDir
                val cacheSize = getDirSize(cacheDir)
                val appSize = getDirSize(appDir)
                val totalSize = cacheSize + appSize
                _uiState.value = _uiState.value.copy(storageUsed = formatFileSize(totalSize))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(storageUsed = "Unknown")
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups.coerceAtMost(3)])
    }

    fun updateTheme(theme: String) { viewModelScope.launch { preferencesManager.updateTheme(theme) } }
    fun updateFontSize(size: String) { viewModelScope.launch { preferencesManager.updateFontSize(size) } }
    fun updateLastSeenVisibility(v: String) {
        viewModelScope.launch {
            preferencesManager.updateLastSeenVisibility(v)
            val uid = authRepository.firebaseUser?.uid ?: return@launch
            userRepository.updateUserProfile(uid, mapOf("lastSeenVisibility" to v))
        }
    }
    fun updateStatusPrivacy(privacy: String) {
        viewModelScope.launch {
            preferencesManager.updateStatusPrivacy(privacy)
            val uid = authRepository.firebaseUser?.uid ?: return@launch
            userRepository.updateUserProfile(uid, mapOf("statusPrivacy" to privacy))
        }
    }
    fun updateOnlineStatus(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateOnlineStatus(enabled)
            val uid = authRepository.firebaseUser?.uid ?: return@launch
            userRepository.updateUserProfile(uid, mapOf("onlineStatus" to enabled))
        }
    }
    fun updateReadReceipts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateReadReceipts(enabled)
            val uid = authRepository.firebaseUser?.uid ?: return@launch
            userRepository.updateUserProfile(uid, mapOf("readReceipts" to enabled))
        }
    }
    fun updateScreenLock(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateScreenLock(enabled) } }
    fun updateMessagePreview(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateMessagePreview(enabled) } }
    fun updateNotificationTone(uri: String) { viewModelScope.launch { preferencesManager.updateNotificationTone(uri) } }
    fun updateIncomingCallRingtone(uri: String) { viewModelScope.launch { preferencesManager.updateIncomingCallRingtone(uri) } }
    fun updateDnd(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateDnd(enabled) } }
    fun updateAutoDownloadMedia(mode: String) { viewModelScope.launch { preferencesManager.updateAutoDownloadMedia(mode) } }
    fun updateDefaultCallType(type: String) { viewModelScope.launch { preferencesManager.updateDefaultCallType(type) } }
    fun updateNoiseSuppression(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateNoiseSuppression(enabled) } }
    fun updateSimulcast(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateSimulcast(enabled) } }
    fun updateForceTurnRelay(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateForceTurnRelay(enabled) } }
    fun updateAudioProfile(profile: String) { viewModelScope.launch { preferencesManager.updateAudioProfile(profile) } }
    fun updateLivekitUrl(url: String) { viewModelScope.launch { preferencesManager.updateLivekitUrl(url) } }
    fun updateSipPrefix(prefix: String) { viewModelScope.launch { preferencesManager.updateSipPrefix(prefix) } }
    fun updateSelfDestructTimer(timer: String) { viewModelScope.launch { preferencesManager.updateSelfDestructTimer(timer) } }
    fun updatePrivacyBlur(enabled: Boolean) { viewModelScope.launch { preferencesManager.updatePrivacyBlur(enabled) } }
    fun updateMediaCompression(compression: String) { viewModelScope.launch { preferencesManager.updateMediaCompression(compression) } }
    fun updateDebugLogging(enabled: Boolean) { viewModelScope.launch { preferencesManager.updateDebugLogging(enabled) } }

    fun showLogoutConfirm() { _uiState.value = _uiState.value.copy(showLogoutConfirm = true) }
    fun hideLogoutConfirm() { _uiState.value = _uiState.value.copy(showLogoutConfirm = false) }
    fun showDeleteConfirm() { _uiState.value = _uiState.value.copy(showDeleteConfirm = true) }
    fun hideDeleteConfirm() { _uiState.value = _uiState.value.copy(showDeleteConfirm = false) }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            val result = authRepository.signOut()
            _uiState.value = _uiState.value.copy(
                isLoggingOut = false,
                showLogoutConfirm = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            _uiState.value = _uiState.value.copy(
                showDeleteConfirm = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                calculateStorageUsage()
            } catch (_: Exception) {}
        }
    }
}
