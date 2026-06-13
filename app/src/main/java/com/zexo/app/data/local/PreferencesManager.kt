package com.zexo.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.zexo.app.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zixo_settings")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val CHAT_WALLPAPER = stringPreferencesKey("chat_wallpaper")
        val LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
        val STATUS_PRIVACY = stringPreferencesKey("status_privacy")
        val ONLINE_STATUS = booleanPreferencesKey("online_status")
        val READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val SCREEN_LOCK = booleanPreferencesKey("screen_lock")
        val MESSAGE_PREVIEW = booleanPreferencesKey("message_preview")
        val NOTIFICATION_TONE = stringPreferencesKey("notification_tone")
        val INCOMING_CALL_RINGTONE = stringPreferencesKey("incoming_call_ringtone")
        val DND = booleanPreferencesKey("dnd")
        val AUTO_DOWNLOAD_MEDIA = stringPreferencesKey("auto_download_media")
        val DEFAULT_CALL_TYPE = stringPreferencesKey("default_call_type")
        val NOISE_SUPPRESSION = booleanPreferencesKey("noise_suppression")
        val SIMULCAST = booleanPreferencesKey("simulcast")
        val FORCE_TURN_RELAY = booleanPreferencesKey("force_turn_relay")
        val AUDIO_PROFILE = stringPreferencesKey("audio_profile")
        val LIVEKIT_URL = stringPreferencesKey("livekit_url")
        val SIP_PREFIX = stringPreferencesKey("sip_prefix")
        val SELF_DESTRUCT_TIMER = stringPreferencesKey("self_destruct_timer")
        val PRIVACY_BLUR = booleanPreferencesKey("privacy_blur")
        val MEDIA_COMPRESSION = stringPreferencesKey("media_compression")
        val DEBUG_LOGGING = booleanPreferencesKey("debug_logging")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            AppSettings(
                theme = prefs[Keys.THEME] ?: "dark",
                fontSize = prefs[Keys.FONT_SIZE] ?: "medium",
                chatWallpaper = prefs[Keys.CHAT_WALLPAPER] ?: "",
                lastSeenVisibility = prefs[Keys.LAST_SEEN_VISIBILITY] ?: "everyone",
                statusPrivacy = prefs[Keys.STATUS_PRIVACY] ?: "all_contacts",
                onlineStatus = prefs[Keys.ONLINE_STATUS] ?: true,
                readReceipts = prefs[Keys.READ_RECEIPTS] ?: true,
                screenLock = prefs[Keys.SCREEN_LOCK] ?: false,
                messagePreview = prefs[Keys.MESSAGE_PREVIEW] ?: true,
                notificationTone = prefs[Keys.NOTIFICATION_TONE] ?: "",
                incomingCallRingtone = prefs[Keys.INCOMING_CALL_RINGTONE] ?: "",
                dnd = prefs[Keys.DND] ?: false,
                autoDownloadMedia = prefs[Keys.AUTO_DOWNLOAD_MEDIA] ?: "wifi",
                defaultCallType = prefs[Keys.DEFAULT_CALL_TYPE] ?: "ask",
                noiseSuppression = prefs[Keys.NOISE_SUPPRESSION] ?: true,
                simulcast = prefs[Keys.SIMULCAST] ?: true,
                forceTurnRelay = prefs[Keys.FORCE_TURN_RELAY] ?: false,
                audioProfile = prefs[Keys.AUDIO_PROFILE] ?: "high_fidelity",
                livekitUrl = prefs[Keys.LIVEKIT_URL] ?: "",
                sipPrefix = prefs[Keys.SIP_PREFIX] ?: "",
                selfDestructTimer = prefs[Keys.SELF_DESTRUCT_TIMER] ?: "off",
                privacyBlur = prefs[Keys.PRIVACY_BLUR] ?: false,
                mediaCompression = prefs[Keys.MEDIA_COMPRESSION] ?: "balanced",
                debugLogging = prefs[Keys.DEBUG_LOGGING] ?: false
            )
        }

    suspend fun updateTheme(theme: String) { context.dataStore.edit { it[Keys.THEME] = theme } }
    suspend fun updateFontSize(size: String) { context.dataStore.edit { it[Keys.FONT_SIZE] = size } }
    suspend fun updateChatWallpaper(wallpaper: String) { context.dataStore.edit { it[Keys.CHAT_WALLPAPER] = wallpaper } }
    suspend fun updateLastSeenVisibility(visibility: String) { context.dataStore.edit { it[Keys.LAST_SEEN_VISIBILITY] = visibility } }
    suspend fun updateStatusPrivacy(privacy: String) { context.dataStore.edit { it[Keys.STATUS_PRIVACY] = privacy } }
    suspend fun updateOnlineStatus(enabled: Boolean) { context.dataStore.edit { it[Keys.ONLINE_STATUS] = enabled } }
    suspend fun updateReadReceipts(enabled: Boolean) { context.dataStore.edit { it[Keys.READ_RECEIPTS] = enabled } }
    suspend fun updateScreenLock(enabled: Boolean) { context.dataStore.edit { it[Keys.SCREEN_LOCK] = enabled } }
    suspend fun updateMessagePreview(enabled: Boolean) { context.dataStore.edit { it[Keys.MESSAGE_PREVIEW] = enabled } }
    suspend fun updateNotificationTone(tone: String) { context.dataStore.edit { it[Keys.NOTIFICATION_TONE] = tone } }
    suspend fun updateIncomingCallRingtone(tone: String) { context.dataStore.edit { it[Keys.INCOMING_CALL_RINGTONE] = tone } }
    suspend fun updateDnd(enabled: Boolean) { context.dataStore.edit { it[Keys.DND] = enabled } }
    suspend fun updateAutoDownloadMedia(mode: String) { context.dataStore.edit { it[Keys.AUTO_DOWNLOAD_MEDIA] = mode } }
    suspend fun updateDefaultCallType(type: String) { context.dataStore.edit { it[Keys.DEFAULT_CALL_TYPE] = type } }
    suspend fun updateNoiseSuppression(enabled: Boolean) { context.dataStore.edit { it[Keys.NOISE_SUPPRESSION] = enabled } }
    suspend fun updateSimulcast(enabled: Boolean) { context.dataStore.edit { it[Keys.SIMULCAST] = enabled } }
    suspend fun updateForceTurnRelay(enabled: Boolean) { context.dataStore.edit { it[Keys.FORCE_TURN_RELAY] = enabled } }
    suspend fun updateAudioProfile(profile: String) { context.dataStore.edit { it[Keys.AUDIO_PROFILE] = profile } }
    suspend fun updateLivekitUrl(url: String) { context.dataStore.edit { it[Keys.LIVEKIT_URL] = url } }
    suspend fun updateSipPrefix(prefix: String) { context.dataStore.edit { it[Keys.SIP_PREFIX] = prefix } }
    suspend fun updateSelfDestructTimer(timer: String) { context.dataStore.edit { it[Keys.SELF_DESTRUCT_TIMER] = timer } }
    suspend fun updatePrivacyBlur(enabled: Boolean) { context.dataStore.edit { it[Keys.PRIVACY_BLUR] = enabled } }
    suspend fun updateMediaCompression(compression: String) { context.dataStore.edit { it[Keys.MEDIA_COMPRESSION] = compression } }
    suspend fun updateDebugLogging(enabled: Boolean) { context.dataStore.edit { it[Keys.DEBUG_LOGGING] = enabled } }
    suspend fun clearAllSettings() { context.dataStore.edit { it.clear() } }
}
