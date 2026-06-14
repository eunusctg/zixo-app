package com.zixo.app.domain.model

/**
 * Application theme modes.
 * DARK: Custom slate/dark emerald color matrix.
 * AMOLED: Absolute pure black (#000000) for power saving.
 * SYSTEM: Automatic binding to Android's configuration changes.
 */
enum class ThemeMode { DARK, AMOLED, SYSTEM }

/**
 * Font size options for app-wide typography scaling.
 */
enum class FontSize { SMALL, MEDIUM, LARGE }

/**
 * Last seen visibility restriction levels.
 */
enum class LastSeenVisibility { EVERYONE, CONTACTS, NOBODY }

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
 * Default call type selection.
 */
enum class DefaultCallType { ASK_EVERY_TIME, LIVEKIT_SIP, WEBRTC_VIDEO }

/**
 * Audio quality profiles for LiveKit Opus engine.
 * HIGH_FIDELITY: For app-to-app WebRTC.
 * NARROWBAND: Low-bandwidth 8kHz/16kHz for SIP trunk telephone lines.
 */
enum class AudioProfile { HIGH_FIDELITY, NARROWBAND }

/**
 * Self-destructing media default countdown timers.
 */
enum class SelfDestructTimer { OFF, FIVE_SECONDS, ONE_MINUTE, ONE_HOUR }

/**
 * Vibration pattern for incoming message notifications.
 * OFF: No vibration at all.
 * DEFAULT: System default vibration pattern.
 * SHORT: Brief single-pulse vibration.
 * LONG: Extended multi-pulse vibration.
 */
enum class VibrationPattern { OFF, DEFAULT, SHORT, LONG }
