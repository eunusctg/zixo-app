package com.zixo.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.zixo.app.domain.model.AppSettings
import com.zixo.app.domain.model.StatusPrivacy
import com.zixo.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zixo_settings")

class PreferencesDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val RINGTONE_ENABLED = booleanPreferencesKey("ringtone_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val MESSAGE_PREVIEW_ENABLED = booleanPreferencesKey("message_preview_enabled")
        val READ_RECEIPTS_ENABLED = booleanPreferencesKey("read_receipts_enabled")
        val STATUS_PRIVACY = intPreferencesKey("status_privacy")
        val BLOCKED_USERS = stringPreferencesKey("blocked_users")
        val NOTIFICATION_TONE = stringPreferencesKey("notification_tone")
        val CALL_RINGTONE = stringPreferencesKey("call_ringtone")
        val CACHED_UID = stringPreferencesKey("cached_uid")
        val CACHED_DISPLAY_NAME = stringPreferencesKey("cached_display_name")
        val CACHED_USERNAME = stringPreferencesKey("cached_username")
        val CACHED_ZIXO_NUMBER = stringPreferencesKey("cached_zixo_number")
        val CACHED_PHONE = stringPreferencesKey("cached_phone")
        val CACHED_BIO = stringPreferencesKey("cached_bio")
        val CACHED_AVATAR_URL = stringPreferencesKey("cached_avatar_url")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppSettings(
                themeMode = ThemeMode.entries.getOrElse(prefs[Keys.THEME_MODE] ?: 2) { ThemeMode.SYSTEM },
                ringtoneEnabled = prefs[Keys.RINGTONE_ENABLED] ?: true,
                vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
                messagePreviewEnabled = prefs[Keys.MESSAGE_PREVIEW_ENABLED] ?: true,
                readReceiptsEnabled = prefs[Keys.READ_RECEIPTS_ENABLED] ?: true,
                statusPrivacy = StatusPrivacy.entries.getOrElse(prefs[Keys.STATUS_PRIVACY] ?: 0) { StatusPrivacy.ALL_CONTACTS },
                blockedUsers = prefs[Keys.BLOCKED_USERS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                notificationTone = prefs[Keys.NOTIFICATION_TONE] ?: "",
                callRingtone = prefs[Keys.CALL_RINGTONE] ?: ""
            )
        }

    suspend fun updateThemeMode(mode: ThemeMode) {
        try {
            context.dataStore.edit { it[Keys.THEME_MODE] = mode.ordinal }
        } catch (_: Exception) { /* Silently fail — non-critical */ }
    }

    suspend fun updateRingtoneEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { it[Keys.RINGTONE_ENABLED] = enabled }
        } catch (_: Exception) { }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
        } catch (_: Exception) { }
    }

    suspend fun updateMessagePreviewEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { it[Keys.MESSAGE_PREVIEW_ENABLED] = enabled }
        } catch (_: Exception) { }
    }

    suspend fun updateReadReceiptsEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { it[Keys.READ_RECEIPTS_ENABLED] = enabled }
        } catch (_: Exception) { }
    }

    suspend fun updateStatusPrivacy(privacy: StatusPrivacy) {
        try {
            context.dataStore.edit { it[Keys.STATUS_PRIVACY] = privacy.ordinal }
        } catch (_: Exception) { }
    }

    suspend fun updateBlockedUsers(users: List<String>) {
        try {
            context.dataStore.edit { it[Keys.BLOCKED_USERS] = users.joinToString(",") }
        } catch (_: Exception) { }
    }

    suspend fun updateNotificationTone(tonePath: String) {
        try {
            context.dataStore.edit { it[Keys.NOTIFICATION_TONE] = tonePath }
        } catch (_: Exception) { }
    }

    suspend fun updateCallRingtone(tonePath: String) {
        try {
            context.dataStore.edit { it[Keys.CALL_RINGTONE] = tonePath }
        } catch (_: Exception) { }
    }

    // Cached profile data for offline access
    suspend fun cacheUserProfile(uid: String, displayName: String, username: String, zixoNumber: String, phone: String, bio: String, avatarUrl: String) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.CACHED_UID] = uid
                prefs[Keys.CACHED_DISPLAY_NAME] = displayName
                prefs[Keys.CACHED_USERNAME] = username
                prefs[Keys.CACHED_ZIXO_NUMBER] = zixoNumber
                prefs[Keys.CACHED_PHONE] = phone
                prefs[Keys.CACHED_BIO] = bio
                prefs[Keys.CACHED_AVATAR_URL] = avatarUrl
            }
        } catch (_: Exception) { }
    }

    fun getCachedProfile(): Flow<Map<String, String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            mapOf(
                "uid" to (prefs[Keys.CACHED_UID] ?: ""),
                "displayName" to (prefs[Keys.CACHED_DISPLAY_NAME] ?: ""),
                "username" to (prefs[Keys.CACHED_USERNAME] ?: ""),
                "zixoNumber" to (prefs[Keys.CACHED_ZIXO_NUMBER] ?: ""),
                "phoneNumber" to (prefs[Keys.CACHED_PHONE] ?: ""),
                "bio" to (prefs[Keys.CACHED_BIO] ?: ""),
                "avatarUrl" to (prefs[Keys.CACHED_AVATAR_URL] ?: "")
            )
        }
}
