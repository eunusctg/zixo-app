package com.zixo.app.data.remote.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager as AndroidAudioManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decoupled audio calibration manager extracted from WebRtcClient.
 *
 * Handles all audio routing, focus management, and mode configuration
 * for WebRTC calls. Ensures echo elimination and proper audio path
 * selection for voice and video calls.
 *
 * ## Key Audio Configuration:
 * - **MODE_IN_COMMUNICATION**: Optimizes audio path for voice communication,
 *   enabling echo canceller and noise suppressor on the hardware codec.
 * - **USAGE_VOICE_COMMUNICATION**: Signals the system that this audio stream
 *   is a two-way voice call, enabling proper audio routing to earpiece,
 *   speakerphone, or Bluetooth accessories.
 *
 * All operations are thread-safe with @Volatile flags and comprehensive
 * try-catch boundaries. Audio focus is properly requested and released
 * to avoid conflicts with other audio apps.
 */
@Singleton
class ZixoAudioManager @Inject constructor(
    context: Context
) {
    private val audioManager: AndroidAudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AndroidAudioManager

    @Volatile
    private var audioFocusRequest: AudioFocusRequest? = null

    @Volatile
    private var isAudioFocusHeld = false

    @Volatile
    private var isInCall = false

    @Volatile
    private var previousAudioMode: Int = AndroidAudioManager.MODE_NORMAL

    @Volatile
    private var wasSpeakerOn = false

    @Volatile
    private var wasBluetoothScoOn = false

    /**
     * Configures the audio pipeline for an active WebRTC call.
     *
     * Sets MODE_IN_COMMUNICATION for echo cancellation, requests audio focus
     * with USAGE_VOICE_COMMUNICATION, and configures speaker routing based
     * on call type (speaker for video, earpiece for audio-only).
     *
     * @param isVideoCall Whether this is a video call (speaker on) or audio (earpiece).
     */
    suspend fun configureForCall(isVideoCall: Boolean) = withContext(Dispatchers.IO) {
        try {
            previousAudioMode = audioManager.mode
            wasSpeakerOn = audioManager.isSpeakerphoneOn
            wasBluetoothScoOn = audioManager.isBluetoothScoOn

            audioManager.mode = AndroidAudioManager.MODE_IN_COMMUNICATION

            requestAudioFocus()

            audioManager.isSpeakerphoneOn = isVideoCall

            isInCall = true
            Timber.d("ZixoAudioManager: Configured for %s call, mode=MODE_IN_COMMUNICATION",
                if (isVideoCall) "video" else "audio")
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to configure for call")
        }
    }

    /**
     * Switches audio output between speaker and earpiece.
     * Releases and re-requests audio focus with the new configuration.
     */
    fun switchAudioOutput(isSpeakerOn: Boolean) {
        try {
            audioManager.isSpeakerphoneOn = isSpeakerOn
            Timber.d("ZixoAudioManager: Audio output switched to %s",
                if (isSpeakerOn) "speaker" else "earpiece")
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to switch audio output")
        }
    }

    /**
     * Mutes or unmutes the microphone at the hardware level.
     */
    fun muteMicrophone(isMuted: Boolean) {
        try {
            audioManager.isMicrophoneMute = isMuted
            Timber.d("ZixoAudioManager: Microphone %s", if (isMuted) "muted" else "unmuted")
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to toggle microphone mute")
        }
    }

    /**
     * Releases audio focus and restores the previous audio mode.
     * Must be called when the call ends to restore normal audio behavior.
     */
    suspend fun releaseAudioFocus() = withContext(Dispatchers.IO) {
        try {
            if (isAudioFocusHeld) {
                abandonAudioFocus()
            }

            audioManager.mode = previousAudioMode
            audioManager.isSpeakerphoneOn = wasSpeakerOn

            if (wasBluetoothScoOn) {
                stopBluetoothSco()
            }

            isInCall = false
            Timber.d("ZixoAudioManager: Audio focus released, mode restored to %d", previousAudioMode)
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to release audio focus")
        }
    }

    /**
     * Checks if a Bluetooth audio device (SCO or A2DP) is available.
     */
    fun isBluetoothAvailable(): Boolean = try {
        audioManager.isBluetoothScoAvailableOffCall
    } catch (e: Exception) {
        Timber.e(e, "ZixoAudioManager: Bluetooth availability check failed")
        false
    }

    /**
     * Starts Bluetooth SCO audio routing for call audio.
     */
    fun startBluetoothSco() {
        try {
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            Timber.d("ZixoAudioManager: Bluetooth SCO started")
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to start Bluetooth SCO")
        }
    }

    /**
     * Stops Bluetooth SCO audio routing.
     */
    fun stopBluetoothSco() {
        try {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            Timber.d("ZixoAudioManager: Bluetooth SCO stopped")
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to stop Bluetooth SCO")
        }
    }

    /**
     * Returns whether the audio manager is currently configured for a call.
     */
    fun isInCallState(): Boolean = isInCall

    // ── Internal Helpers ──────────────────────────────────────────

    private fun requestAudioFocus() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AndroidAudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Timber.d("ZixoAudioManager: Audio focus change: %d", focusChange)
                    }
                    .build()

                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                isAudioFocusHeld = result == AndroidAudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { focusChange ->
                        Timber.d("ZixoAudioManager: Audio focus change (legacy): %d", focusChange)
                    },
                    AndroidAudioManager.STREAM_VOICE_CALL,
                    AndroidAudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                isAudioFocusHeld = result == AndroidAudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }

            Timber.d("ZixoAudioManager: Audio focus requested, granted=%b", isAudioFocusHeld)
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to request audio focus")
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            isAudioFocusHeld = false
            audioFocusRequest = null
            Timber.d("ZixoAudioManager: Audio focus abandoned")
        } catch (e: Exception) {
            Timber.e(e, "ZixoAudioManager: Failed to abandon audio focus")
        }
    }
}
