package com.zexo.app.data.model

data class AppSettings(
    val theme: String = "dark",
    val fontSize: String = "medium",
    val chatWallpaper: String = "",
    val lastSeenVisibility: String = "everyone",
    val statusPrivacy: String = "all_contacts",
    val onlineStatus: Boolean = true,
    val readReceipts: Boolean = true,
    val screenLock: Boolean = false,
    val messagePreview: Boolean = true,
    val notificationTone: String = "",
    val incomingCallRingtone: String = "",
    val dnd: Boolean = false,
    val autoDownloadMedia: String = "wifi",
    val defaultCallType: String = "ask",
    val noiseSuppression: Boolean = true,
    val simulcast: Boolean = true,
    val forceTurnRelay: Boolean = false,
    val audioProfile: String = "high_fidelity",
    val livekitUrl: String = "",
    val sipPrefix: String = "",
    val selfDestructTimer: String = "off",
    val privacyBlur: Boolean = false,
    val mediaCompression: String = "balanced",
    val debugLogging: Boolean = false
)

enum class ThemeMode(val label: String) { DARK("Dark"), AMOLED("AMOLED"), SYSTEM("System") }
enum class FontSize(val label: String) { SMALL("Small"), MEDIUM("Medium"), LARGE("Large") }
enum class LastSeenVisibility(val label: String) { EVERYONE("Everyone"), CONTACTS("Contacts"), NOBODY("Nobody") }
enum class StatusPrivacyOption(val label: String) { ALL_CONTACTS("My Contacts"), EXCLUDE_SOME("Share Except..."), ONLY_SHARE_WITH("Only Share With...") }
enum class AutoDownload(val label: String) { WIFI("Wi-Fi only"), CELLULAR("Cellular"), NEVER("Never") }
enum class CallType(val label: String) { ASK("Ask every time"), SIP("LiveKit SIP Call"), WEBRTC("WebRTC video") }
enum class AudioProfile(val label: String) { HIGH_FIDELITY("High Fidelity Audio"), NARROWBAND("Narrowband Optimization") }
enum class SelfDestructTimer(val label: String) { OFF("Off"), SECONDS_5("5 seconds"), MINUTE_1("1 minute"), HOUR_1("1 hour") }
enum class MediaCompression(val label: String) { LOSSLESS("Lossless"), BALANCED("Balanced"), EXTREME("Extreme Save") }
