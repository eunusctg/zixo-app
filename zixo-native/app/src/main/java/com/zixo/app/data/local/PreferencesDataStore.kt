package com.zixo.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.domain.model.VibrationOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Complete DataStore preferences class managing ALL settings from [AppSettingsState].
 *
 * Each preference exposes:
 *   - A **Flow-based getter** that emits the current value (or its default).
 *   - A **suspend setter** that atomically writes the new value.
 *
 * Enum values are serialized by their `name` strings.
 * [Set]<[MediaType]> is serialized as a JSON string via kotlinx.serialization.
 */
@Singleton
class PreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ── JSON serializer for Set<MediaType> ────────────────────────────────────

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }

    private val mediaTypeSetSerializer = SetSerializer(MediaType.serializer())

    // ── Theme & Appearance ────────────────────────────────────────────────────

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.DARK) }
            ?: ThemeMode.DARK
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
    }

    val fontSizeScale: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE_SCALE] ?: 1.0f
    }

    suspend fun setFontSizeScale(scale: Float) {
        dataStore.edit { prefs -> prefs[KEY_FONT_SIZE_SCALE] = scale }
    }

    // ── Security ──────────────────────────────────────────────────────────────

    val isSecurityNotificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SECURITY_NOTIFICATIONS] ?: false
    }

    suspend fun setSecurityNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SECURITY_NOTIFICATIONS] = enabled }
    }

    val isTwoStepEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TWO_STEP] ?: false
    }

    suspend fun setTwoStepEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_TWO_STEP] = enabled }
    }

    val isScreenLockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SCREEN_LOCK] ?: false
    }

    suspend fun setScreenLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SCREEN_LOCK] = enabled }
    }

    // ── Privacy ───────────────────────────────────────────────────────────────

    val lastSeenVisibility: Flow<VisibilityOption> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SEEN_VISIBILITY]?.let {
            runCatching { VisibilityOption.valueOf(it) }.getOrDefault(VisibilityOption.EVERYONE)
        } ?: VisibilityOption.EVERYONE
    }

    suspend fun setLastSeenVisibility(option: VisibilityOption) {
        dataStore.edit { prefs -> prefs[KEY_LAST_SEEN_VISIBILITY] = option.name }
    }

    val profilePhotoVisibility: Flow<VisibilityOption> = dataStore.data.map { prefs ->
        prefs[KEY_PROFILE_PHOTO_VISIBILITY]?.let {
            runCatching { VisibilityOption.valueOf(it) }.getOrDefault(VisibilityOption.EVERYONE)
        } ?: VisibilityOption.EVERYONE
    }

    suspend fun setProfilePhotoVisibility(option: VisibilityOption) {
        dataStore.edit { prefs -> prefs[KEY_PROFILE_PHOTO_VISIBILITY] = option.name }
    }

    val aboutVisibility: Flow<VisibilityOption> = dataStore.data.map { prefs ->
        prefs[KEY_ABOUT_VISIBILITY]?.let {
            runCatching { VisibilityOption.valueOf(it) }.getOrDefault(VisibilityOption.EVERYONE)
        } ?: VisibilityOption.EVERYONE
    }

    suspend fun setAboutVisibility(option: VisibilityOption) {
        dataStore.edit { prefs -> prefs[KEY_ABOUT_VISIBILITY] = option.name }
    }

    val statusPrivacy: Flow<StatusPrivacyOption> = dataStore.data.map { prefs ->
        prefs[KEY_STATUS_PRIVACY]?.let {
            runCatching { StatusPrivacyOption.valueOf(it) }.getOrDefault(StatusPrivacyOption.ALL_CONTACTS)
        } ?: StatusPrivacyOption.ALL_CONTACTS
    }

    suspend fun setStatusPrivacy(option: StatusPrivacyOption) {
        dataStore.edit { prefs -> prefs[KEY_STATUS_PRIVACY] = option.name }
    }

    val areReadReceiptsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_READ_RECEIPTS] ?: true
    }

    suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_READ_RECEIPTS] = enabled }
    }

    // ── Ephemeral / Disappearing Messages ─────────────────────────────────────

    val ephemeralDestructTimer: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_EPHEMERAL_TIMER] ?: 0
    }

    suspend fun setEphemeralDestructTimer(seconds: Int) {
        dataStore.edit { prefs -> prefs[KEY_EPHEMERAL_TIMER] = seconds }
    }

    // ── Advanced Network / Security ───────────────────────────────────────────

    val protectIpInCalls: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROTECT_IP_IN_CALLS] ?: true
    }

    suspend fun setProtectIpInCalls(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROTECT_IP_IN_CALLS] = enabled }
    }

    val disableLinkPreviews: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DISABLE_LINK_PREVIEWS] ?: false
    }

    suspend fun setDisableLinkPreviews(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DISABLE_LINK_PREVIEWS] = enabled }
    }

    // ── Premium / Freemium Billing ───────────────────────────────────────────

    val isIncomingPstnEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_INCOMING_PSTN_ENABLED] ?: false
    }

    suspend fun setIncomingPstnEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_INCOMING_PSTN_ENABLED] = enabled }
    }

    // ── Chat Behavior ─────────────────────────────────────────────────────────

    val enterIsSend: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ENTER_IS_SEND] ?: false
    }

    suspend fun setEnterIsSend(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ENTER_IS_SEND] = enabled }
    }

    val isMediaVisibilityEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MEDIA_VISIBILITY] ?: true
    }

    suspend fun setMediaVisibilityEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_MEDIA_VISIBILITY] = enabled }
    }

    // ── Notifications & Tones ─────────────────────────────────────────────────

    val areConversationTonesEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CONVERSATION_TONES] ?: true
    }

    suspend fun setConversationTonesEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CONVERSATION_TONES] = enabled }
    }

    val messageNotificationToneUri: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_MESSAGE_NOTIFICATION_TONE] ?: ""
    }

    suspend fun setMessageNotificationToneUri(uri: String) {
        dataStore.edit { prefs -> prefs[KEY_MESSAGE_NOTIFICATION_TONE] = uri }
    }

    val groupNotificationToneUri: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_GROUP_NOTIFICATION_TONE] ?: ""
    }

    suspend fun setGroupNotificationToneUri(uri: String) {
        dataStore.edit { prefs -> prefs[KEY_GROUP_NOTIFICATION_TONE] = uri }
    }

    val callRingtoneUri: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CALL_RINGTONE] ?: ""
    }

    suspend fun setCallRingtoneUri(uri: String) {
        dataStore.edit { prefs -> prefs[KEY_CALL_RINGTONE] = uri }
    }

    val videoCallRingtoneUri: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_VIDEO_CALL_RINGTONE] ?: ""
    }

    suspend fun setVideoCallRingtoneUri(uri: String) {
        dataStore.edit { prefs -> prefs[KEY_VIDEO_CALL_RINGTONE] = uri }
    }

    val vibrationPattern: Flow<VibrationOption> = dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_PATTERN]?.let {
            runCatching { VibrationOption.valueOf(it) }.getOrDefault(VibrationOption.DEFAULT)
        } ?: VibrationOption.DEFAULT
    }

    suspend fun setVibrationPattern(pattern: VibrationOption) {
        dataStore.edit { prefs -> prefs[KEY_VIBRATION_PATTERN] = pattern.name }
    }

    // ── Media Auto-Download ───────────────────────────────────────────────────

    val autoDownloadMobile: Flow<Set<MediaType>> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_DOWNLOAD_MOBILE]?.let { jsonStr ->
            runCatching { json.decodeFromString(mediaTypeSetSerializer, jsonStr) }
                .getOrDefault(emptySet())
        } ?: emptySet()
    }

    suspend fun setAutoDownloadMobile(types: Set<MediaType>) {
        val jsonStr = json.encodeToString(mediaTypeSetSerializer, types)
        dataStore.edit { prefs -> prefs[KEY_AUTO_DOWNLOAD_MOBILE] = jsonStr }
    }

    val autoDownloadWifi: Flow<Set<MediaType>> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_DOWNLOAD_WIFI]?.let { jsonStr ->
            runCatching { json.decodeFromString(mediaTypeSetSerializer, jsonStr) }
                .getOrDefault(setOf(MediaType.PHOTO))
        } ?: setOf(MediaType.PHOTO)
    }

    suspend fun setAutoDownloadWifi(types: Set<MediaType>) {
        val jsonStr = json.encodeToString(mediaTypeSetSerializer, types)
        dataStore.edit { prefs -> prefs[KEY_AUTO_DOWNLOAD_WIFI] = jsonStr }
    }

    val autoDownloadRoaming: Flow<Set<MediaType>> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_DOWNLOAD_ROAMING]?.let { jsonStr ->
            runCatching { json.decodeFromString(mediaTypeSetSerializer, jsonStr) }
                .getOrDefault(emptySet())
        } ?: emptySet()
    }

    suspend fun setAutoDownloadRoaming(types: Set<MediaType>) {
        val jsonStr = json.encodeToString(mediaTypeSetSerializer, types)
        dataStore.edit { prefs -> prefs[KEY_AUTO_DOWNLOAD_ROAMING] = jsonStr }
    }

    // ── Media Upload Quality ──────────────────────────────────────────────────

    val mediaUploadQuality: Flow<UploadQuality> = dataStore.data.map { prefs ->
        prefs[KEY_MEDIA_UPLOAD_QUALITY]?.let {
            runCatching { UploadQuality.valueOf(it) }.getOrDefault(UploadQuality.BALANCED)
        } ?: UploadQuality.BALANCED
    }

    suspend fun setMediaUploadQuality(quality: UploadQuality) {
        dataStore.edit { prefs -> prefs[KEY_MEDIA_UPLOAD_QUALITY] = quality.name }
    }

    // ── Clear All Preferences ─────────────────────────────────────────────────

    suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    // ── Preference Keys ───────────────────────────────────────────────────────

    companion object {
        // Theme & Appearance
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")

        // Security
        private val KEY_SECURITY_NOTIFICATIONS = booleanPreferencesKey("security_notifications_enabled")
        private val KEY_TWO_STEP = booleanPreferencesKey("two_step_enabled")
        private val KEY_SCREEN_LOCK = booleanPreferencesKey("screen_lock_enabled")

        // Privacy
        private val KEY_LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
        private val KEY_PROFILE_PHOTO_VISIBILITY = stringPreferencesKey("profile_photo_visibility")
        private val KEY_ABOUT_VISIBILITY = stringPreferencesKey("about_visibility")
        private val KEY_STATUS_PRIVACY = stringPreferencesKey("status_privacy")
        private val KEY_READ_RECEIPTS = booleanPreferencesKey("read_receipts_enabled")

        // Ephemeral
        private val KEY_EPHEMERAL_TIMER = intPreferencesKey("ephemeral_destruct_timer")

        // Advanced
        private val KEY_PROTECT_IP_IN_CALLS = booleanPreferencesKey("protect_ip_in_calls")
        private val KEY_DISABLE_LINK_PREVIEWS = booleanPreferencesKey("disable_link_previews")
        private val KEY_INCOMING_PSTN_ENABLED = booleanPreferencesKey("incoming_pstn_enabled")

        // Chat Behavior
        private val KEY_ENTER_IS_SEND = booleanPreferencesKey("enter_is_send")
        private val KEY_MEDIA_VISIBILITY = booleanPreferencesKey("media_visibility_enabled")

        // Notifications & Tones
        private val KEY_CONVERSATION_TONES = booleanPreferencesKey("conversation_tones_enabled")
        private val KEY_MESSAGE_NOTIFICATION_TONE = stringPreferencesKey("message_notification_tone_uri")
        private val KEY_GROUP_NOTIFICATION_TONE = stringPreferencesKey("group_notification_tone_uri")
        private val KEY_CALL_RINGTONE = stringPreferencesKey("call_ringtone_uri")
        private val KEY_VIDEO_CALL_RINGTONE = stringPreferencesKey("video_call_ringtone_uri")
        private val KEY_VIBRATION_PATTERN = stringPreferencesKey("vibration_pattern")

        // Media Auto-Download
        private val KEY_AUTO_DOWNLOAD_MOBILE = stringPreferencesKey("auto_download_mobile")
        private val KEY_AUTO_DOWNLOAD_WIFI = stringPreferencesKey("auto_download_wifi")
        private val KEY_AUTO_DOWNLOAD_ROAMING = stringPreferencesKey("auto_download_roaming")

        // Media Upload Quality
        private val KEY_MEDIA_UPLOAD_QUALITY = stringPreferencesKey("media_upload_quality")
    }
}
