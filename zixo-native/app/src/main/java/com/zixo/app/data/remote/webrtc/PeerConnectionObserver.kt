package com.zixo.app.data.remote.webrtc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.MediaStreamTrack
import org.webrtc.RtpTransceiver
import org.webrtc.VideoTrack
import timber.log.Timber

/**
 * Named PeerConnection observer for WebRTC event handling.
 *
 * Extracts anonymous inline PeerConnection.Observer callbacks from WebRtcClient
 * into a dedicated, testable, and reusable observer class. All callbacks log
 * via Timber for WebRTC debugging and emit state through Kotlin StateFlows
 * for reactive UI updates.
 *
 * ## State Emissions:
 * - [iceConnectionState]: Emits ICE connection state changes for call UI
 * - [iceCandidate]: Emits discovered ICE candidates for signaling
 * - [remoteVideoTrack]: Emits the remote video track for rendering
 * - [renegotiationNeeded]: Signals when SDP renegotiation is required
 *
 * All callbacks execute on the WebRTC signaling thread; UI consumption
 * must happen via StateFlow collection on the Main dispatcher.
 */
class PeerConnectionObserver : PeerConnection.Observer {

    private val _iceConnectionState = MutableStateFlow(PeerConnection.IceConnectionState.NEW)
    val iceConnectionState: StateFlow<PeerConnection.IceConnectionState> = _iceConnectionState.asStateFlow()

    private val _iceCandidate = MutableStateFlow<IceCandidate?>(null)
    val iceCandidate: StateFlow<IceCandidate?> = _iceCandidate.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _renegotiationNeeded = MutableStateFlow(false)
    val renegotiationNeeded: StateFlow<Boolean> = _renegotiationNeeded.asStateFlow()

    private val _signalingState = MutableStateFlow(PeerConnection.SignalingState.STABLE)
    val signalingState: StateFlow<PeerConnection.SignalingState> = _signalingState.asStateFlow()

    override fun onSignalingChange(newState: PeerConnection.SignalingState) {
        Timber.d("PeerConnectionObserver: Signaling state changed to %s", newState)
        _signalingState.value = newState
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
        Timber.d("PeerConnectionObserver: ICE connection state changed to %s", newState)
        _iceConnectionState.value = newState

        when (newState) {
            PeerConnection.IceConnectionState.CONNECTED -> {
                Timber.d("PeerConnectionObserver: ICE connected")
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                Timber.w("PeerConnectionObserver: ICE disconnected — may reconnect")
            }
            PeerConnection.IceConnectionState.FAILED -> {
                Timber.e("PeerConnectionObserver: ICE connection FAILED")
            }
            PeerConnection.IceConnectionState.CLOSED -> {
                Timber.d("PeerConnectionObserver: ICE connection closed")
            }
            else -> {
                Timber.d("PeerConnectionObserver: ICE state %s", newState)
            }
        }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Timber.d("PeerConnectionObserver: ICE connection receiving changed to %b", receiving)
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
        Timber.d("PeerConnectionObserver: ICE gathering state changed to %s", newState)
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        Timber.d(
            "PeerConnectionObserver: ICE candidate discovered — sdpMid=%s, sdpMLineIndex=%d",
            candidate.sdpMid, candidate.sdpMLineIndex
        )
        _iceCandidate.value = candidate
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {
        Timber.d("PeerConnectionObserver: %d ICE candidates removed", candidates.size)
    }

    override fun onAddStream(stream: MediaStream) {
        Timber.w("PeerConnectionObserver: onAddStream (legacy API) — %d video tracks", stream.videoTracks.size)
        if (stream.videoTracks.isNotEmpty()) {
            _remoteVideoTrack.value = stream.videoTracks[0]
        }
    }

    override fun onRemoveStream(stream: MediaStream) {
        Timber.d("PeerConnectionObserver: onRemoveStream (legacy API)")
        _remoteVideoTrack.value = null
    }

    override fun onDataChannel(channel: DataChannel) {
        Timber.d("PeerConnectionObserver: Data channel received — label=%s", channel.label())
    }

    override fun onRenegotiationNeeded() {
        Timber.d("PeerConnectionObserver: Renegotiation needed")
        _renegotiationNeeded.value = true
    }

    override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
        Timber.d("PeerConnectionObserver: onAddTrack — %d streams", streams.size)
        try {
            val track = receiver.track()
            if (track is VideoTrack) {
                Timber.d("PeerConnectionObserver: Remote video track added via onAddTrack")
                _remoteVideoTrack.value = track
            }
        } catch (e: Exception) {
            Timber.e(e, "PeerConnectionObserver: Failed to process onAddTrack")
        }
    }

    override fun onTrack(transceiver: RtpTransceiver) {
        Timber.d("PeerConnectionObserver: onTrack — mediaType=%s", transceiver.mediaType)
        try {
            val track = transceiver.receiver.track()
            if (track is VideoTrack && transceiver.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                Timber.d("PeerConnectionObserver: Remote video track added via onTrack")
                _remoteVideoTrack.value = track
            }
        } catch (e: Exception) {
            Timber.e(e, "PeerConnectionObserver: Failed to process onTrack")
        }
    }

    /**
     * Resets all StateFlows to their initial values.
     * Called when the PeerConnection is closed and the observer is recycled.
     */
    fun clear() {
        _iceConnectionState.value = PeerConnection.IceConnectionState.NEW
        _iceCandidate.value = null
        _remoteVideoTrack.value = null
        _renegotiationNeeded.value = false
        _signalingState.value = PeerConnection.SignalingState.STABLE
        Timber.d("PeerConnectionObserver: All states cleared")
    }
}
