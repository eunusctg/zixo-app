package com.zixo.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zixo.app.domain.model.AudioProfile
import com.zixo.app.domain.model.AutoDownloadMedia
import com.zixo.app.domain.model.DefaultCallType
import com.zixo.app.domain.model.FontSize
import com.zixo.app.domain.model.LastSeenVisibility
import com.zixo.app.domain.model.MediaCompressionProfile
import com.zixo.app.domain.model.SelfDestructTimer
import com.zixo.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ── Theme & Appearance ──────────────────────────────────────────

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE] = mode.name }
    }

    val fontSize: Flow<FontSize> = dataStore.data.map { prefs ->
        prefs[FONT_SIZE]?.let { FontSize.valueOf(it) } ?: FontSize.MEDIUM
    }

    suspend fun setFontSize(size: FontSize) {
        dataStore.edit { prefs -> prefs[FONT_SIZE] = size.name }
    }

    // ── Privacy ─────────────────────────────────────────────────────

    val lastSeenVisibility: Flow<LastSeenVisibility> = dataStore.data.map { prefs ->
        prefs[LAST_SEEN_VISIBILITY]?.let { LastSeenVisibility.valueOf(it) } ?: LastSeenVisibility.EVERYONE
    }

    suspend fun setLastSeenVisibility(visibility: LastSeenVisibility) {
        dataStore.edit { prefs -> prefs[LAST_SEEN_VISIBILITY] = visibility.name }
    }

    val onlineStatusEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONLINE_STATUS_ENABLED] ?: true
    }

    suspend fun setOnlineStatusEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[ONLINE_STATUS_ENABLED] = enabled }
    }

    val readReceiptsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[READ_RECEIPTS_ENABLED] ?: true
    }

    suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[READ_RECEIPTS_ENABLED] = enabled }
    }

    val screenLockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SCREEN_LOCK_ENABLED] ?: false
    }

    suspend fun setScreenLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SCREEN_LOCK_ENABLED] = enabled }
    }

    val messagePreviewEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[MESSAGE_PREVIEW_ENABLED] ?: true
    }

    suspend fun setMessagePreviewEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[MESSAGE_PREVIEW_ENABLED] = enabled }
    }

    val appSwitcherPrivacyBlur: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[APP_SWITCHER_PRIVACY_BLUR] ?: false
    }

    suspend fun setAppSwitcherPrivacyBlur(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[APP_SWITCHER_PRIVACY_BLUR] = enabled }
    }

    // ── Notifications & DND ─────────────────────────────────────────

    val dndEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DND_ENABLED] ?: false
    }

    suspend fun setDndEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DND_ENABLED] = enabled }
    }

    val notificationTone: Flow<String> = dataStore.data.map { prefs ->
        prefs[NOTIFICATION_TONE] ?: ""
    }

    suspend fun setNotificationTone(uri: String) {
        dataStore.edit { prefs -> prefs[NOTIFICATION_TONE] = uri }
    }

    // ── Media ───────────────────────────────────────────────────────

    val autoDownloadMedia: Flow<AutoDownloadMedia> = dataStore.data.map { prefs ->
        prefs[AUTO_DOWNLOAD_MEDIA]?.let { AutoDownloadMedia.valueOf(it) } ?: AutoDownloadMedia.WIFI_ONLY
    }

    suspend fun setAutoDownloadMedia(mode: AutoDownloadMedia) {
        dataStore.edit { prefs -> prefs[AUTO_DOWNLOAD_MEDIA] = mode.name }
    }

    val mediaCompressionProfile: Flow<MediaCompressionProfile> = dataStore.data.map { prefs ->
        prefs[MEDIA_COMPRESSION_PROFILE]?.let { MediaCompressionProfile.valueOf(it) } ?: MediaCompressionProfile.BALANCED
    }

    suspend fun setMediaCompressionProfile(profile: MediaCompressionProfile) {
        dataStore.edit { prefs -> prefs[MEDIA_COMPRESSION_PROFILE] = profile.name }
    }

    // ── Calling ─────────────────────────────────────────────────────

    val defaultCallType: Flow<DefaultCallType> = dataStore.data.map { prefs ->
        prefs[DEFAULT_CALL_TYPE]?.let { DefaultCallType.valueOf(it) } ?: DefaultCallType.ASK_EVERY_TIME
    }

    suspend fun setDefaultCallType(callType: DefaultCallType) {
        dataStore.edit { prefs -> prefs[DEFAULT_CALL_TYPE] = callType.name }
    }

    val noiseSuppressionEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOISE_SUPPRESSION_ENABLED] ?: true
    }

    suspend fun setNoiseSuppressionEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NOISE_SUPPRESSION_ENABLED] = enabled }
    }

    val liveKitUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[LIVEKIT_URL] ?: ""
    }

    suspend fun setLiveKitUrl(url: String) {
        dataStore.edit { prefs -> prefs[LIVEKIT_URL] = url }
    }

    val sipOutboundPrefix: Flow<String> = dataStore.data.map { prefs ->
        prefs[SIP_OUTBOUND_PREFIX] ?: ""
    }

    suspend fun setSipOutboundPrefix(prefix: String) {
        dataStore.edit { prefs -> prefs[SIP_OUTBOUND_PREFIX] = prefix }
    }

    val simulcastEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SIMULCAST_ENABLED] ?: false
    }

    suspend fun setSimulcastEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SIMULCAST_ENABLED] = enabled }
    }

    val forceTurnRelay: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[FORCE_TURN_RELAY] ?: false
    }

    suspend fun setForceTurnRelay(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[FORCE_TURN_RELAY] = enabled }
    }

    val audioProfile: Flow<AudioProfile> = dataStore.data.map { prefs ->
        prefs[AUDIO_PROFILE]?.let { AudioProfile.valueOf(it) } ?: AudioProfile.HIGH_FIDELITY
    }

    suspend fun setAudioProfile(profile: AudioProfile) {
        dataStore.edit { prefs -> prefs[AUDIO_PROFILE] = profile.name }
    }

    // ── Self-Destruct & Security ────────────────────────────────────

    val selfDestructDefault: Flow<SelfDestructTimer> = dataStore.data.map { prefs ->
        prefs[SELF_DESTRUCT_DEFAULT]?.let { SelfDestructTimer.valueOf(it) } ?: SelfDestructTimer.OFF
    }

    suspend fun setSelfDestructDefault(timer: SelfDestructTimer) {
        dataStore.edit { prefs -> prefs[SELF_DESTRUCT_DEFAULT] = timer.name }
    }

    // ── Chat Customization ──────────────────────────────────────────

    val chatWallpaper: Flow<String> = dataStore.data.map { prefs ->
        prefs[CHAT_WALLPAPER] ?: ""
    }

    suspend fun setChatWallpaper(assetPath: String) {
        dataStore.edit { prefs -> prefs[CHAT_WALLPAPER] = assetPath }
    }

    // ── Debug ───────────────────────────────────────────────────────

    val debugLoggingEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEBUG_LOGGING_ENABLED] ?: false
    }

    suspend fun setDebugLoggingEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DEBUG_LOGGING_ENABLED] = enabled }
    }

    // ── Clear all preferences ───────────────────────────────────────

    suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    // ── Preference Keys ─────────────────────────────────────────────

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val FONT_SIZE = stringPreferencesKey("font_size")
        private val LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
        private val ONLINE_STATUS_ENABLED = booleanPreferencesKey("online_status_enabled")
        private val READ_RECEIPTS_ENABLED = booleanPreferencesKey("read_receipts_enabled")
        private val SCREEN_LOCK_ENABLED = booleanPreferencesKey("screen_lock_enabled")
        private val MESSAGE_PREVIEW_ENABLED = booleanPreferencesKey("message_preview_enabled")
        private val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        private val AUTO_DOWNLOAD_MEDIA = stringPreferencesKey("auto_download_media")
        private val DEFAULT_CALL_TYPE = stringPreferencesKey("default_call_type")
        private val NOISE_SUPPRESSION_ENABLED = booleanPreferencesKey("noise_suppression_enabled")
        private val LIVEKIT_URL = stringPreferencesKey("livekit_url")
        private val SIP_OUTBOUND_PREFIX = stringPreferencesKey("sip_outbound_prefix")
        private val SIMULCAST_ENABLED = booleanPreferencesKey("simulcast_enabled")
        private val FORCE_TURN_RELAY = booleanPreferencesKey("force_turn_relay")
        private val AUDIO_PROFILE = stringPreferencesKey("audio_profile")
        private val SELF_DESTRUCT_DEFAULT = stringPreferencesKey("self_destruct_default")
        private val APP_SWITCHER_PRIVACY_BLUR = booleanPreferencesKey("app_switcher_privacy_blur")
        private val MEDIA_COMPRESSION_PROFILE = stringPreferencesKey("media_compression_profile")
        private val DEBUG_LOGGING_ENABLED = booleanPreferencesKey("debug_logging_enabled")
        private val CHAT_WALLPAPER = stringPreferencesKey("chat_wallpaper")
        private val NOTIFICATION_TONE = stringPreferencesKey("notification_tone")
    }
}


