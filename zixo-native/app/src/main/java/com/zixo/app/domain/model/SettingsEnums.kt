package com.zixo.app.domain.model

/**
 * Application-level enumerations that are NOT already defined in AppSettingsState.kt.
 *
 * ThemeMode, VibrationOption, VisibilityOption, StatusPrivacyOption, MediaType,
 * UploadQuality, CallState, CallEndReason, etc. are all defined in
 * AppSettingsState.kt and must NOT be duplicated here.
 */

/**
 * Font size options for app-wide typography scaling.
 */
enum class FontSize { SMALL, MEDIUM, LARGE }

/**
 * Last seen visibility restriction levels.
 * Alias for [VisibilityOption] defined in AppSettingsState.kt —
 * provided here for backward compatibility with existing code references.
 */
typealias LastSeenVisibility = VisibilityOption

/**
 * Auto-download media network constraints.
 */
enum class AutoDownloadMedia { WIFI_ONLY, CELLULAR, NEVER }

/**
 * Media compression profiles for Firebase upload.
 * LOSSLESS: Raw asset transfer.
 * BALANCED: Standard optimized compression.
 * EXTREME_SAVE: Heavy resolution scaling for weak networks.
 */
enum class MediaCompressionProfile { LOSSLESS, BALANCED, EXTREME_SAVE }

/**
 * Self-destructing media default countdown timers.
 */
enum class SelfDestructTimer { OFF, FIVE_SECONDS, ONE_MINUTE, ONE_HOUR }
