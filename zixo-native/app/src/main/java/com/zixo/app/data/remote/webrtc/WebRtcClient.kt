package com.zixo.app.data.remote.webrtc

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.Dispatchers
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
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core WebRTC Client — Pure WebRTC implementation (NO LiveKit).
 *
 * Handles:
 * - PeerConnectionFactory initialization on Dispatchers.IO
 * - PeerConnection creation with STUN/TURN servers
 * - Local SDP creation (createOffer/createAnswer)
 * - ICE candidate handling
 * - Media stream management (audio/video tracks)
 *
 * All WebRTC operations are isolated on background threads.
 * The Android Main Thread is NEVER blocked by WebRTC operations.
 */
@Singleton
class WebRtcClient @Inject constructor(
    private val context: Context
) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val eglBase: EglBase by lazy { EglBase.create() }
    val eglBaseContext: EglBase.Context get() = eglBase.eglBaseContext

    private var isMuted = false
    private var isCameraOff = false
    private var isSpeakerOn = false

    // ICE servers for WebRTC connection
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    // ── Initialization ────────────────────────────────────────────────────────

    /**
     * Initializes the PeerConnectionFactory on Dispatchers.IO.
     * Must be called before any other WebRTC operation.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (peerConnectionFactory != null) return@withContext

        try {
            val initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)

            val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()

            Timber.d("PeerConnectionFactory initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize PeerConnectionFactory")
        }
    }

    /**
     * Creates a PeerConnection with STUN/TURN servers and initializes
     * local media tracks (audio and optionally video).
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
                object : PeerConnection.Observer {
                    override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                        Timber.d("Signaling state: %s", state)
                    }

                    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                        Timber.d("ICE connection state: %s", state)
                    }

                    override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                        Timber.d("ICE gathering state: %s", state)
                    }

                    override fun onIceCandidate(candidate: IceCandidate?) {
                        candidate?.let {
                            Timber.d("ICE candidate found: %s", it.sdp)
                        }
                    }

                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

                    override fun onAddStream(stream: MediaStream?) {
                        Timber.d("Remote stream added")
                    }

                    override fun onRemoveStream(stream: MediaStream?) {}

                    override fun onDataChannel(channel: DataChannel?) {}

                    override fun onRenegotiationNeeded() {
                        Timber.d("Renegotiation needed")
                    }

                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}

                    override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) {}
                }
            )

            // Add audio track
            createAudioTrack()

            // Add video track if video call
            if (isVideoCall) {
                createVideoTrack()
            }

            Timber.d("PeerConnection created (video=%s)", isVideoCall)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create PeerConnection")
        }
    }

    // ── SDP Operations ────────────────────────────────────────────────────────

    /**
     * Creates an SDP offer for initiating a call.
     * Must be called on Dispatchers.IO.
     *
     * @return The SDP offer string, or null on failure.
     */
    suspend fun createOffer(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }

            var sdp: String? = null
            connection.createOffer(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) {
                    sdp = description.description
                    connection.setLocalDescription(SimpleSdpObserver(), description)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String) {
                    Timber.e("Create offer failed: %s", error)
                }
                override fun onSetFailure(error: String) {}
            }, constraints)

            // Wait for the SDP to be created
            var attempts = 0
            while (sdp == null && attempts < 50) {
                kotlinx.coroutines.delay(100)
                attempts++
            }

            Timber.d("SDP offer created: %s chars", sdp?.length ?: 0)
            sdp
        } catch (e: Exception) {
            Timber.e(e, "Failed to create offer")
            null
        }
    }

    /**
     * Creates an SDP answer for responding to a call.
     * Must be called on Dispatchers.IO.
     *
     * @return The SDP answer string, or null on failure.
     */
    suspend fun createAnswer(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }

            var sdp: String? = null
            connection.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) {
                    sdp = description.description
                    connection.setLocalDescription(SimpleSdpObserver(), description)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String) {
                    Timber.e("Create answer failed: %s", error)
                }
                override fun onSetFailure(error: String) {}
            }, constraints)

            var attempts = 0
            while (sdp == null && attempts < 50) {
                kotlinx.coroutines.delay(100)
                attempts++
            }

            Timber.d("SDP answer created: %s chars", sdp?.length ?: 0)
            sdp
        } catch (e: Exception) {
            Timber.e(e, "Failed to create answer")
            null
        }
    }

    /**
     * Sets the remote SDP offer on the PeerConnection.
     */
    suspend fun setRemoteOffer(sdp: String) = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
            val description = SessionDescription(SessionDescription.Type.OFFER, sdp)
            connection.setRemoteDescription(SimpleSdpObserver(), description)
            Timber.d("Remote offer set")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set remote offer")
        }
    }

    /**
     * Sets the remote SDP answer on the PeerConnection.
     */
    suspend fun setRemoteAnswer(sdp: String) = withContext(Dispatchers.IO) {
        try {
            val connection = peerConnection ?: throw IllegalStateException("No PeerConnection")
            val description = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            connection.setRemoteDescription(SimpleSdpObserver(), description)
            Timber.d("Remote answer set")
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

    // ── Media Control ─────────────────────────────────────────────────────────

    /**
     * Mutes or unmutes the local audio track.
     */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        localAudioTrack?.setEnabled(!muted)
        Timber.d("Audio muted: %s", muted)
    }

    /**
     * Enables the local camera track.
     */
    fun enableCamera() {
        isCameraOff = false
        localVideoTrack?.setEnabled(true)
        try {
            videoCapturer?.startCapture(640, 480, 30)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start camera capture")
        }
        Timber.d("Camera enabled")
    }

    /**
     * Disables the local camera track.
     */
    fun disableCamera() {
        isCameraOff = true
        localVideoTrack?.setEnabled(false)
        videoCapturer?.stopCapture()
        Timber.d("Camera disabled")
    }

    /**
     * Toggles speaker on/off.
     */
    fun setSpeakerOn(speakerOn: Boolean) {
        isSpeakerOn = speakerOn
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = speakerOn
        Timber.d("Speaker on: %s", speakerOn)
    }

    /**
     * Switches between front and back camera.
     */
    fun switchCamera() {
        try {
            videoCapturer?.let { capturer ->
                val cameraEnumerator = getCameraEnumerator()
                if (cameraEnumerator is Camera1Enumerator) {
                    Camera1Enumerator.switchCamera(capturer)
                } else if (cameraEnumerator is Camera2Enumerator) {
                    Camera2Enumerator(context).switchCamera(capturer)
                }
            }
            Timber.d("Camera switched")
        } catch (e: Exception) {
            Timber.e(e, "Failed to switch camera")
        }
    }

    // ── Disconnect ────────────────────────────────────────────────────────────

    /**
     * Disconnects the PeerConnection and releases all WebRTC resources.
     * Must be called on Dispatchers.IO.
     */
    fun disconnect() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            localVideoSource?.dispose()
            localVideoSource = null

            localAudioSource?.dispose()
            localAudioSource = null

            localVideoTrack?.dispose()
            localVideoTrack = null

            localAudioTrack?.dispose()
            localAudioTrack = null

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            Timber.d("WebRTC disconnected and resources released")
        } catch (e: Exception) {
            Timber.e(e, "Error during WebRTC disconnect")
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private fun createAudioTrack() {
        val factory = peerConnectionFactory ?: return
        localAudioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack)
        Timber.d("Audio track created and added")
    }

    private fun createVideoTrack() {
        val factory = peerConnectionFactory ?: return
        val enumerator = getCameraEnumerator()
        val deviceNames = enumerator.deviceNames

        // Try to find front camera first
        var capturer: VideoCapturer? = null
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                capturer = enumerator.createCapturer(name, null)
                break
            }
        }
        if (capturer == null && deviceNames.isNotEmpty()) {
            capturer = enumerator.createCapturer(deviceNames[0], null)
        }

        capturer?.let { vc ->
            videoCapturer = vc
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            localVideoSource = factory.createVideoSource(vc.isScreencast)
            vc.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
            vc.startCapture(640, 480, 30)

            localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
            localVideoTrack?.setEnabled(true)
            peerConnection?.addTrack(localVideoTrack)
            Timber.d("Video track created and added")
        }
    }

    private fun getCameraEnumerator(): CameraEnumerator {
        return if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(false)
        }
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

    companion object {
        private const val AUDIO_TRACK_ID = "audio_track"
        private const val VIDEO_TRACK_ID = "video_track"
    }
}
