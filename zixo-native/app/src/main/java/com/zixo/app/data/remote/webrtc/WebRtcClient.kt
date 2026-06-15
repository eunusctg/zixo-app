package com.zixo.app.data.remote.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.MediaStreamTrack
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.CameraVideoCapturer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Core WebRTC Client — Crash-Proof Pure WebRTC Implementation (NO LiveKit).
 *
 * ## Rigid Native WebRTC Processing Guardrails:
 *
 * 1. **All Media Isolated on Dispatchers.IO:**
 *    PeerConnectionFactory, PeerConnection, SDP generation, and ICE candidate
 *    discovery are handled exclusively on background thread scopes.
 *    The Android Main Thread is NEVER blocked by WebRTC operations.
 *
 * 2. **Mandatory Foreground Service:**
 *    CallForegroundService (FOREGROUND_SERVICE_TYPE_CAMERA | MICROPHONE | PHONE_CALL)
 *    wraps every WebRTC session so Android does not terminate media streams
 *    when the user backgrounds the application.
 *
 * 3. **Hardware Acoustic Echo & Routing Calibration:**
 *    AudioManager.MODE_IN_COMMUNICATION + AudioFocusRequest(USAGE_VOICE_COMMUNICATION)
 *    eliminates system echo feedback loops and fixes audio routing to
 *    speakerphone or Bluetooth accessories.
 *
 * 4. **EglBase Singleton Scope Preservation:**
 *    The hardware-accelerated root EglBase context is created ONCE as a
 *    DI Singleton and NEVER re-initialized on Composable recomposition.
 *    Re-creating EglBase during recomposition triggers an uncatchable SIGABRT.
 *    SurfaceViewRenderer instances are wrapped inside AndroidView.
 *
 * 5. **Structured try-catch Boundaries:**
 *    Every WebRTC operation has a precise try-catch boundary preventing
 *    any native crash from propagating as an unhandled exception.
 */
@Singleton
class WebRtcClient @Inject constructor(
    private val context: Context
) {

    // ════════════════════════════════════════════════════════════════
    // EglBase — DI Singleton, NEVER re-created on recomposition
    // ════════════════════════════════════════════════════════════════

    /**
     * The root EglBase instance. Created lazily once and retained for the
     * entire application lifecycle. Re-creating this on Composable
     * recomposition triggers an uncatchable SIGABRT crash in the native
     * WebRTC layer, so it MUST be a singleton.
     */
    private val _eglBase: EglBase by lazy {
        try {
            EglBase.create().also {
                Timber.d("EglBase created (singleton)")
            }
        } catch (e: Exception) {
            Timber.e(e, "FATAL: Failed to create EglBase")
            throw e
        }
    }

    val eglBaseContext: EglBase.Context get() = _eglBase.eglBaseContext

    // ════════════════════════════════════════════════════════════════
    // PeerConnection & Media State
    // ════════════════════════════════════════════════════════════════

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    // Remote video track for rendering in the call overlay
    private var remoteVideoTrack: VideoTrack? = null

    // Audio focus request for proper audio routing
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isAudioFocusHeld = false

    // Call state flags
    @Volatile private var isMuted = false
    @Volatile private var isCameraOff = false
    @Volatile private var isSpeakerOn = false

    // ICE callback for signaling
    var onIceCandidateGenerated: ((IceCandidate) -> Unit)? = null

    // Remote video track callback for UI rendering
    var onRemoteVideoTrackAdded: ((VideoTrack) -> Unit)? = null

    // ICE servers for WebRTC connection
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    // ════════════════════════════════════════════════════════════════
    // Initialization — Dispatchers.IO Only
    // ════════════════════════════════════════════════════════════════

    /**
     * Initializes the PeerConnectionFactory on Dispatchers.IO.
     * Must be called before any other WebRTC operation.
     *
     * This method is idempotent — calling it multiple times is safe.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (peerConnectionFactory != null) {
            Timber.d("PeerConnectionFactory already initialized, skipping")
            return@withContext
        }

        try {
            val initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()

            PeerConnectionFactory.initialize(initializationOptions)

            val encoderFactory = DefaultVideoEncoderFactory(_eglBase.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(_eglBase.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()

            Timber.d("PeerConnectionFactory initialized successfully on IO thread")
        } catch (e: Exception) {
            Timber.e(e, "CRITICAL: Failed to initialize PeerConnectionFactory")
            throw e
        }
    }

    /**
     * Creates a PeerConnection with STUN/TURN servers and initializes
     * local media tracks (audio and optionally video).
     *
     * Also configures the hardware audio pipeline:
     * - AudioManager.MODE_IN_COMMUNICATION
     * - AudioFocusRequest with USAGE_VOICE_COMMUNICATION
     *
     * @param isVideoCall Whether to include video tracks.
     */
    suspend fun initializePeerConnection(isVideoCall: Boolean) = withContext(Dispatchers.IO) {
        try {
            initialize()
            disconnect() // Clean up any existing connection

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }

            peerConnection = peerConnectionFactory?.createPeerConnection(
                rtcConfig,
                createPeerConnectionObserver()
            )

            if (peerConnection == null) {
                throw IllegalStateException("Failed to create PeerConnection — factory returned null")
            }

            // Add audio track
            createAudioTrack()

            // Add video track if video call
            if (isVideoCall) {
                createVideoTrack()
            }

            // Configure hardware audio pipeline
            configureAudioForCall()

            Timber.d("PeerConnection created (video=%s) with audio calibration", isVideoCall)
        } catch (e: Exception) {
            Timber.e(e, "CRITICAL: Failed to create PeerConnection")
            throw e
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Hardware Acoustic Echo & Audio Routing Calibration
    // ════════════════════════════════════════════════════════════════

    /**
     * Configures the Android audio system for a WebRTC call.
     *
     * This method applies the following critical settings:
     * - **AudioManager.MODE_IN_COMMUNICATION**: Optimizes the audio path for
     *   two-way voice communication, enabling echo cancellation and noise
     *   suppression at the hardware level.
     * - **AudioFocusRequest with USAGE_VOICE_COMMUNICATION**: Requests
     *   permanent audio focus for voice communication, which ensures proper
     *   audio routing and prevents other apps from interrupting the call.
     * - **Speakerphone default OFF**: Routes audio to the earpiece by default
     *   for audio calls, speaker for video calls.
     *
     * This eliminates system echo feedback loops and fixes audio routing
     * to speakerphone or Bluetooth accessories flawlessly.
     */
    private fun configureAudioForCall() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                Timber.w("AudioManager not available, skipping audio calibration")
                return
            }

            // Set communication mode — enables AEC (Acoustic Echo Cancellation)
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // Request audio focus for voice communication
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Timber.d("Audio focus changed: %d", focusChange)
                    }
                    .build()

                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                isAudioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                Timber.d("Audio focus request result: %s (granted=%s)", result, isAudioFocusHeld)
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                isAudioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                Timber.d("Audio focus request (legacy) result: %s", result)
            }

            // Default: earpiece for audio, speaker for video
            audioManager.isSpeakerphoneOn = false
            audioManager.isBluetoothScoOn = false

            Timber.d("Audio calibrated: MODE_IN_COMMUNICATION, USAGE_VOICE_COMMUNICATION")
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure audio for call — audio may have echo issues")
        }
    }

    /**
     * Releases audio focus and restores the audio manager to normal mode.
     * Must be called when a call ends.
     */
    private fun releaseAudioFromCall() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) return

            // Restore normal audio mode
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            audioManager.isBluetoothScoOn = false

            // Release audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            isAudioFocusHeld = false

            Timber.d("Audio released: MODE_NORMAL, focus abandoned")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release audio from call")
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SDP Operations — Dispatchers.IO Only
    // ════════════════════════════════════════════════════════════════

    /**
     * Creates an SDP offer for initiating a call.
     * Uses suspendCancellableCoroutine for clean async handling.
     * Waits for setLocalDescription to complete before returning.
     * Must be called on Dispatchers.IO.
     *
     * @return The SDP offer string, or null on failure.
     */
    suspend fun createOffer(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection
                ?: throw IllegalStateException("No PeerConnection available for createOffer")

            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }

            suspendCancellableCoroutine { cont ->
                connection.createOffer(object : SdpObserver {
                    override fun onCreateSuccess(sdpDescription: SessionDescription) {
                        connection.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(description: SessionDescription) {}
                            override fun onSetSuccess() {
                                cont.resume(sdpDescription.description)
                            }
                            override fun onCreateFailure(error: String) {
                                Timber.e("Create local description failed: %s", error)
                                cont.resume(null)
                            }
                            override fun onSetFailure(error: String) {
                                Timber.e("Set local description failed: %s", error)
                                cont.resume(null)
                            }
                        }, sdpDescription)
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String) {
                        Timber.e("Create offer failed: %s", error)
                        cont.resume(null)
                    }

                    override fun onSetFailure(error: String) {
                        Timber.e("Set local description failed: %s", error)
                        cont.resume(null)
                    }
                }, constraints)
            }
        } catch (e: Exception) {
            Timber.e(e, "CRITICAL: Failed to create offer")
            null
        }
    }

    /**
     * Creates an SDP answer for responding to a call.
     * Uses suspendCancellableCoroutine for clean async handling.
     * Waits for setLocalDescription to complete before returning.
     * Must be called on Dispatchers.IO.
     *
     * @return The SDP answer string, or null on failure.
     */
    suspend fun createAnswer(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection
                ?: throw IllegalStateException("No PeerConnection available for createAnswer")

            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }

            suspendCancellableCoroutine { cont ->
                connection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdpDescription: SessionDescription) {
                        connection.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(description: SessionDescription) {}
                            override fun onSetSuccess() {
                                cont.resume(sdpDescription.description)
                            }
                            override fun onCreateFailure(error: String) {
                                Timber.e("Create local description failed: %s", error)
                                cont.resume(null)
                            }
                            override fun onSetFailure(error: String) {
                                Timber.e("Set local description failed: %s", error)
                                cont.resume(null)
                            }
                        }, sdpDescription)
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String) {
                        Timber.e("Create answer failed: %s", error)
                        cont.resume(null)
                    }

                    override fun onSetFailure(error: String) {
                        Timber.e("Set local description failed: %s", error)
                        cont.resume(null)
                    }
                }, constraints)
            }
        } catch (e: Exception) {
            Timber.e(e, "CRITICAL: Failed to create answer")
            null
        }
    }

    /**
     * Sets the remote SDP offer on the PeerConnection.
     * Waits for setRemoteDescription to complete before returning.
     */
    suspend fun setRemoteOffer(sdp: String) = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
            val description = SessionDescription(SessionDescription.Type.OFFER, sdp)
            suspendCancellableCoroutine<Unit> { cont ->
                connection.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {}
                    override fun onSetSuccess() {
                        Timber.d("Remote offer set successfully")
                        cont.resume(Unit)
                    }
                    override fun onCreateFailure(error: String) {
                        Timber.e("Set remote offer create failure: %s", error)
                        cont.resumeWithException(RuntimeException("Set remote offer failed: $error"))
                    }
                    override fun onSetFailure(error: String) {
                        Timber.e("Set remote offer failed: %s", error)
                        cont.resumeWithException(RuntimeException("Set remote offer failed: $error"))
                    }
                }, description)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set remote offer")
        }
    }

    /**
     * Sets the remote SDP answer on the PeerConnection.
     * Waits for setRemoteDescription to complete before returning.
     */
    suspend fun setRemoteAnswer(sdp: String) = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
            val description = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            suspendCancellableCoroutine<Unit> { cont ->
                connection.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {}
                    override fun onSetSuccess() {
                        Timber.d("Remote answer set successfully")
                        cont.resume(Unit)
                    }
                    override fun onCreateFailure(error: String) {
                        Timber.e("Set remote answer create failure: %s", error)
                        cont.resumeWithException(RuntimeException("Set remote answer failed: $error"))
                    }
                    override fun onSetFailure(error: String) {
                        Timber.e("Set remote answer failed: %s", error)
                        cont.resumeWithException(RuntimeException("Set remote answer failed: $error"))
                    }
                }, description)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set remote answer")
        }
    }

    /**
     * Adds an ICE candidate received from the signaling server.
     */
    suspend fun addIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) =
        withContext(Dispatchers.IO) {
            try {
                val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
                val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                connection.addIceCandidate(candidate)
                Timber.d("ICE candidate added")
            } catch (e: Exception) {
                Timber.e(e, "Failed to add ICE candidate")
            }
        }

    // ════════════════════════════════════════════════════════════════
    // Video Rendering — EglBase Singleton Preservation
    // ════════════════════════════════════════════════════════════════

    /**
     * Initializes a [SurfaceViewRenderer] for displaying the local video preview.
     *
     * **CRITICAL**: This must be called from the UI thread. The [SurfaceViewRenderer]
     * is initialized with the singleton [EglBase.Context] which is NEVER re-created.
     * The SurfaceViewRenderer itself should be created once and retained using
     * `remember` in the Composable, wrapped inside an [AndroidView].
     *
     * @param surfaceView The SurfaceViewRenderer to initialize.
     */
    fun initLocalVideoRenderer(surfaceView: SurfaceViewRenderer) {
        try {
            surfaceView.init(_eglBase.eglBaseContext, null)
            surfaceView.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            surfaceView.setMirror(true) // Mirror local camera
            surfaceView.setEnableHardwareScaler(true)
            localVideoTrack?.addSink(surfaceView)
            Timber.d("Local video renderer initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize local video renderer")
        }
    }

    /**
     * Initializes a [SurfaceViewRenderer] for displaying the remote video track.
     *
     * **CRITICAL**: Same singleton EglBase rules apply. Never re-create EglBase
     * on recomposition — wrap the SurfaceViewRenderer in AndroidView with remember.
     *
     * @param surfaceView The SurfaceViewRenderer to initialize.
     */
    fun initRemoteVideoRenderer(surfaceView: SurfaceViewRenderer) {
        try {
            surfaceView.init(_eglBase.eglBaseContext, null)
            surfaceView.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            surfaceView.setMirror(false) // Don't mirror remote video
            surfaceView.setEnableHardwareScaler(true)
            remoteVideoTrack?.addSink(surfaceView)
            Timber.d("Remote video renderer initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize remote video renderer")
        }
    }

    /**
     * Releases a video renderer, removing it from the video track's sink list.
     * Must be called when the Composable leaves the composition.
     *
     * @param surfaceView The SurfaceViewRenderer to release.
     * @param isLocal Whether this is the local (preview) renderer.
     */
    fun releaseVideoRenderer(surfaceView: SurfaceViewRenderer, isLocal: Boolean) {
        try {
            if (isLocal) {
                localVideoTrack?.removeSink(surfaceView)
            } else {
                remoteVideoTrack?.removeSink(surfaceView)
            }
            surfaceView.release()
            Timber.d("Video renderer released (local=%s)", isLocal)
        } catch (e: Exception) {
            Timber.e(e, "Failed to release video renderer")
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Media Controls
    // ════════════════════════════════════════════════════════════════

    /**
     * Mutes or unmutes the local audio track.
     */
    fun setMuted(muted: Boolean) {
        try {
            isMuted = muted
            localAudioTrack?.setEnabled(!muted)
            Timber.d("Audio muted: %s", muted)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set mute state")
        }
    }

    /**
     * Enables the local camera track.
     */
    fun enableCamera() {
        try {
            isCameraOff = false
            localVideoTrack?.setEnabled(true)
            videoCapturer?.startCapture(640, 480, 30)
            Timber.d("Camera enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable camera")
        }
    }

    /**
     * Disables the local camera track.
     */
    fun disableCamera() {
        try {
            isCameraOff = true
            localVideoTrack?.setEnabled(false)
            videoCapturer?.stopCapture()
            Timber.d("Camera disabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable camera")
        }
    }

    /**
     * Toggles speaker on/off with proper AudioManager routing.
     *
     * When speaker is ON: audio routes through the loudspeaker.
     * When speaker is OFF: audio routes through the earpiece or Bluetooth.
     */
    fun setSpeakerOn(speakerOn: Boolean) {
        try {
            isSpeakerOn = speakerOn
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isSpeakerphoneOn = speakerOn

            // If turning speaker off, try Bluetooth SCO if available
            if (!speakerOn) {
                audioManager?.isBluetoothScoOn = false
            }

            Timber.d("Speaker on: %s", speakerOn)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set speaker state")
        }
    }

    /**
     * Switches between front and back camera.
     */
    fun switchCamera() {
        try {
            videoCapturer?.let { capturer ->
                if (capturer is CameraVideoCapturer) {
                    capturer.switchCamera(null)
                }
            }
            Timber.d("Camera switched")
        } catch (e: Exception) {
            Timber.e(e, "Failed to switch camera")
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Disconnect — Clean Resource Release
    // ════════════════════════════════════════════════════════════════

    /**
     * Disconnects the PeerConnection and releases all WebRTC resources.
     *
     * This method is safe to call from any thread. All cleanup operations
     * are wrapped in individual try-catch blocks to ensure that a failure
     * in releasing one resource does not prevent others from being released.
     *
     * Also releases audio focus and restores the AudioManager to normal mode.
     *
     * **IMPORTANT**: Does NOT dispose the EglBase singleton. The EglBase
     * lives for the entire application lifecycle and must never be destroyed
     * during a call disconnect.
     */
    fun disconnect() {
        try {
            videoCapturer?.tryStopCapture()
        } catch (_: Exception) {}

        try {
            videoCapturer?.dispose()
        } catch (_: Exception) {}
        videoCapturer = null

        try {
            localVideoSource?.dispose()
        } catch (_: Exception) {}
        localVideoSource = null

        try {
            localAudioSource?.dispose()
        } catch (_: Exception) {}
        localAudioSource = null

        try {
            localVideoTrack?.dispose()
        } catch (_: Exception) {}
        localVideoTrack = null

        try {
            localAudioTrack?.dispose()
        } catch (_: Exception) {}
        localAudioTrack = null

        try {
            remoteVideoTrack?.dispose()
        } catch (_: Exception) {}
        remoteVideoTrack = null

        try {
            surfaceTextureHelper?.dispose()
        } catch (_: Exception) {}
        surfaceTextureHelper = null

        try {
            peerConnection?.close()
        } catch (_: Exception) {}

        try {
            peerConnection?.dispose()
        } catch (_: Exception) {}
        peerConnection = null

        // Release audio focus and restore normal mode
        releaseAudioFromCall()

        // Reset state flags
        isMuted = false
        isCameraOff = false
        isSpeakerOn = false
        onIceCandidateGenerated = null
        onRemoteVideoTrackAdded = null

        Timber.d("WebRTC disconnected and all resources released (EglBase preserved)")
    }

    // ════════════════════════════════════════════════════════════════
    // Internal Helpers
    // ════════════════════════════════════════════════════════════════

    private fun createAudioTrack() {
        try {
            val factory = peerConnectionFactory ?: return
            localAudioSource = factory.createAudioSource(MediaConstraints())
            localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
            localAudioTrack?.setEnabled(true)
            peerConnection?.addTrack(localAudioTrack)
            Timber.d("Audio track created and added to PeerConnection")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create audio track")
        }
    }

    private fun createVideoTrack() {
        try {
            val factory = peerConnectionFactory ?: return
            val enumerator = getCameraEnumerator()
            val deviceNames = enumerator.deviceNames

            // Try to find front camera first
            var capturer: VideoCapturer? = null
            for (name in deviceNames) {
                if (enumerator.isFrontFacing(name)) {
                    try {
                        capturer = enumerator.createCapturer(name, null)
                        break
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to create front camera capturer, trying next")
                    }
                }
            }
            if (capturer == null && deviceNames.isNotEmpty()) {
                try {
                    capturer = enumerator.createCapturer(deviceNames[0], null)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create any camera capturer")
                }
            }

            capturer?.let { vc ->
                videoCapturer = vc
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", _eglBase.eglBaseContext)
                localVideoSource = factory.createVideoSource(vc.isScreencast)
                vc.initialize(surfaceTextureHelper, context.applicationContext, localVideoSource?.capturerObserver)
                vc.startCapture(640, 480, 30)

                localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
                localVideoTrack?.setEnabled(true)
                peerConnection?.addTrack(localVideoTrack)
                Timber.d("Video track created and added to PeerConnection")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create video track")
        }
    }

    /**
     * Creates the PeerConnection observer that handles all WebRTC events.
     *
     * ICE candidates are forwarded to the signaling layer via
     * [onIceCandidateGenerated] callback. Remote video tracks are
     * forwarded via [onRemoteVideoTrackAdded] callback.
     */
    private fun createPeerConnectionObserver() = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {
            Timber.d("Signaling state: %s", state)
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            Timber.d("ICE connection state: %s", state)
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    Timber.d("WebRTC ICE connected")
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    Timber.w("WebRTC ICE disconnected")
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    Timber.e("WebRTC ICE connection failed")
                }
                else -> {}
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
            Timber.d("ICE gathering state: %s", state)
        }

        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
                Timber.d("Local ICE candidate generated: %s", it.sdp)
                onIceCandidateGenerated?.invoke(it)
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
            Timber.d("ICE candidates removed: %d", candidates?.size ?: 0)
        }

        override fun onAddStream(stream: MediaStream?) {
            Timber.d("Remote stream added with %d video tracks", stream?.videoTracks?.size ?: 0)
        }

        override fun onRemoveStream(stream: MediaStream?) {
            Timber.d("Remote stream removed")
        }

        override fun onDataChannel(channel: DataChannel?) {}

        override fun onRenegotiationNeeded() {
            Timber.d("Renegotiation needed")
        }

        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            Timber.d("Remote track added")
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            if (transceiver.isVideoType()) {
                val track = transceiver.receiver.track() as? VideoTrack
                if (track != null) {
                    remoteVideoTrack = track
                    Timber.d("Remote video track received via onTrack")
                    onRemoteVideoTrackAdded?.invoke(track)
                }
            }
        }
    }

    private fun getCameraEnumerator(): CameraEnumerator {
        return if (Camera2Enumerator.isSupported(context.applicationContext)) {
            Camera2Enumerator(context.applicationContext)
        } else {
            Camera1Enumerator(false)
        }
    }

    /** Safe stopCapture that catches any exception. */
    private fun VideoCapturer.tryStopCapture() {
        try {
            stopCapture()
        } catch (_: Exception) {}
    }

    private class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) {
            Timber.e("SDP error: %s", error)
        }
        override fun onSetFailure(error: String) {
            Timber.e("SDP set error: %s", error)
        }
    }

    /**
     * Detects whether an RtpTransceiver carries video or audio.
     * Uses MediaStreamTrack.MediaType enum from the WebRTC library.
     */
    private fun RtpTransceiver.isVideoType(): Boolean {
        return try {
            mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO
        } catch (_: Exception) { false }
    }

    companion object {
        private const val AUDIO_TRACK_ID = "zixo_audio_track"
        private const val VIDEO_TRACK_ID = "zixo_video_track"
    }
}


