package com.zixo.app.ui.settings

import android.util.Log
import com.zixo.app.data.repository.SettingsRepositoryImpl
import com.zixo.app.data.repository.AuthRepositoryImpl
import com.zixo.app.domain.model.AppSettings
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.StatusPrivacy
import com.zixo.app.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepositoryImpl,
    private val authRepository: AuthRepositoryImpl
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val TAG = "SettingsViewModel"

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val _showQrPopup = MutableStateFlow(false)
    val showQrPopup: StateFlow<Boolean> = _showQrPopup.asStateFlow()

    fun updateThemeMode(mode: ThemeMode) {
        scope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun updateRingtoneEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.updateRingtoneEnabled(enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.updateVibrationEnabled(enabled) }
    }

    fun updateMessagePreviewEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.updateMessagePreviewEnabled(enabled) }
    }

    fun updateReadReceiptsEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.updateReadReceiptsEnabled(enabled) }
    }

    fun updateStatusPrivacy(privacy: StatusPrivacy) {
        scope.launch { settingsRepository.updateStatusPrivacy(privacy) }
    }

    fun updateNotificationTone(tonePath: String) {
        scope.launch { settingsRepository.updateNotificationTone(tonePath) }
    }

    fun updateCallRingtone(tonePath: String) {
        scope.launch { settingsRepository.updateCallRingtone(tonePath) }
    }

    fun updateDisplayName(name: String) {
        scope.launch {
            _isSaving.value = true
            val result = settingsRepository.updateDisplayName(name)
            _saveMessage.value = if (result.isSuccess) "Name updated" else result.exceptionOrNull()?.message
            _isSaving.value = false
        }
    }

    fun updateBio(bio: String) {
        scope.launch {
            _isSaving.value = true
            val result = settingsRepository.updateBio(bio)
            _saveMessage.value = if (result.isSuccess) "Bio updated" else result.exceptionOrNull()?.message
            _isSaving.value = false
        }
    }

    fun updateAvatar(imageBytes: ByteArray) {
        scope.launch {
            _isSaving.value = true
            val result = settingsRepository.updateAvatar(imageBytes)
            _saveMessage.value = if (result.isSuccess) "Avatar updated" else result.exceptionOrNull()?.message
            _isSaving.value = false
        }
    }

    fun signOut() {
        scope.launch { authRepository.signOut() }
    }

    fun showQrPopup() { _showQrPopup.value = true }
    fun dismissQrPopup() { _showQrPopup.value = false }

    fun clearSaveMessage() { _saveMessage.value = null }
}
