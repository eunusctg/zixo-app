package com.zixo.app.domain.repository

import com.zixo.app.domain.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>

    suspend fun updateThemeMode(mode: com.zixo.app.domain.model.ThemeMode)
    suspend fun updateRingtoneEnabled(enabled: Boolean)
    suspend fun updateVibrationEnabled(enabled: Boolean)
    suspend fun updateMessagePreviewEnabled(enabled: Boolean)
    suspend fun updateReadReceiptsEnabled(enabled: Boolean)
    suspend fun updateStatusPrivacy(privacy: com.zixo.app.domain.model.StatusPrivacy)
    suspend fun updateBlockedUsers(users: List<String>)
    suspend fun updateNotificationTone(tonePath: String)
    suspend fun updateCallRingtone(tonePath: String)
    suspend fun updateDisplayName(name: String): Result<Unit>
    suspend fun updateBio(bio: String): Result<Unit>
    suspend fun updateAvatar(imageBytes: ByteArray): Result<String>
}
