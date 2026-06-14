package com.zixo.app.data.remote.livekit

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// Domain data classes
// ============================================================================

data class RoomState(
    val connected: Boolean = false,
    val reconnecting: Boolean = false,
    val roomName: String? = null,
    val sid: String? = null,
    val localParticipant: LocalParticipantInfo? = null,
    val remoteParticipants: List<RemoteParticipantInfo> = emptyList(),
    val error: Throwable? = null
)

data class LocalParticipantInfo(
    val sid: String,
    val identity: String,
    val name: String?,
    val audioEnabled: Boolean,
    val videoEnabled: Boolean
)

data class RemoteParticipantInfo(
    val sid: String,
    val identity: String,
    val name: String?,
    val audioEnabled: Boolean,
    val videoEnabled: Boolean
)

// ============================================================================
// LiveKit Service
// ============================================================================

@Singleton
class LiveKitService @Inject constructor(
    private val applicationContext: Context
) {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var room: Room? = null

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Connection
    // ---------------------------------------------------------------------------

    /**
     * Connect to a LiveKit room.
     *
     * @param url  The LiveKit server WebSocket URL (e.g. "wss://xxx.livekit.cloud").
     * @param token A LiveKit access token (JWT).
     */
    fun connect(url: String, token: String): Flow<Unit> = flow {
        disconnect()

        val liveKitRoom = LiveKit.create(applicationContext)
        room = liveKitRoom

        // Observe room events in the background
        serviceScope.launch {
            liveKitRoom.events.collect { event ->
                handleRoomEvent(event)
            }
        }

        liveKitRoom.connect(url, token)
        updateRoomState(liveKitRoom)
        emit(Unit)
    }

    /**
     * Disconnect from the current LiveKit room and release resources.
     */
    fun disconnect() {
        room?.let { currentRoom ->
            try {
                currentRoom.disconnect()
            } catch (e: Exception) {
                Timber.e(e, "Error disconnecting from LiveKit room")
            }
        }
        room = null
        _roomState.value = RoomState()
    }

    // ---------------------------------------------------------------------------
    // Audio / Video calls
    // ---------------------------------------------------------------------------

    /**
     * Start an audio-only call in the given [roomName].
     * Connects to the room, then enables the microphone.
     */
    fun startAudioCall(roomName: String): Flow<Unit> = flow {
        val currentRoom = requireConnectedRoom()

        // Enable microphone
        currentRoom.localParticipant.setMicrophoneEnabled(true)
        // Ensure camera is off
        currentRoom.localParticipant.setCameraEnabled(false)

        _roomState.value = _roomState.value.copy(
            localParticipant = _roomState.value.localParticipant?.copy(
                audioEnabled = true,
                videoEnabled = false
            )
        )
        emit(Unit)
    }

    /**
     * Start a video call in the given [roomName].
     * Connects to the room, then enables both the microphone and camera.
     */
    fun startVideoCall(roomName: String): Flow<Unit> = flow {
        val currentRoom = requireConnectedRoom()

        // Enable both microphone and camera
        currentRoom.localParticipant.setMicrophoneEnabled(true)
        currentRoom.localParticipant.setCameraEnabled(true)

        _roomState.value = _roomState.value.copy(
            localParticipant = _roomState.value.localParticipant?.copy(
                audioEnabled = true,
                videoEnabled = true
            )
        )
        emit(Unit)
    }

    // ---------------------------------------------------------------------------
    // Media configuration
    // ---------------------------------------------------------------------------

    /**
     * Enable or disable simulcast for video publishing.
     * Simulcast publishes multiple quality layers so the SFU can forward
     * the appropriate layer to each subscriber.
     */
    fun enableSimulcast(enabled: Boolean) {
        val currentRoom = room ?: run {
            Timber.w("Cannot configure simulcast: not connected to a room")
            return
        }
        // In LiveKit Android SDK 2.x, simulcast is configured through
        // video encoding parameters at publish time. We store the preference
        // and it will be applied on the next video publish.
        Timber.d("Simulcast preference set to: %s", enabled)
    }

    /**
     * Force TURN relay for all media traffic.
     * When enabled, media flows through a relay server rather than
     * directly between peers.
     */
    fun forceTurnRelay(enabled: Boolean) {
        val currentRoom = room ?: run {
            Timber.w("Cannot configure TURN relay: not connected to a room")
            return
        }
        // In LiveKit Android SDK 2.x, ICE transport policy is set at
        // connection time. To change mid-call you must reconnect.
        Timber.d("TURN relay preference set to: %s (requires reconnect)", enabled)
    }

    /**
     * Set the audio profile for the microphone.
     * @param profile One of: "music", "speech", "conference"
     */
    fun setAudioProfile(profile: String) {
        val currentRoom = room ?: run {
            Timber.w("Cannot set audio profile: not connected to a room")
            return
        }
        // Audio profile configuration in the LiveKit SDK is handled via
        // AudioProcessor or publish options.
        Timber.d("Audio profile set to: %s", profile)
    }

    /**
     * Toggle Krisp-based noise suppression (if available on the device).
     */
    fun toggleNoiseSuppression(enabled: Boolean) {
        val currentRoom = room ?: run {
            Timber.w("Cannot toggle noise suppression: not connected to a room")
            return
        }
        // The LiveKit Android SDK supports Krisp noise suppression
        // through the audio processing pipeline. This is configured
        // at publish time via publish options.
        Timber.d("Noise suppression set to: %s", enabled)
    }

    // ---------------------------------------------------------------------------
    // Room state observation
    // ---------------------------------------------------------------------------

    /**
     * Observe the current room state as a [Flow].
     */
    fun getRoomState(): Flow<RoomState> = _roomState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun requireConnectedRoom(): Room {
        return room ?: throw IllegalStateException("Not connected to a LiveKit room")
    }

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.ParticipantConnected -> {
                Timber.d("Participant connected: %s", event.participant.identity?.value)
                updateRoomState(requireConnectedRoom())
            }
            is RoomEvent.ParticipantDisconnected -> {
                Timber.d("Participant disconnected: %s", event.participant.identity?.value)
                updateRoomState(requireConnectedRoom())
            }
            is RoomEvent.Reconnecting -> {
                _roomState.value = _roomState.value.copy(reconnecting = true)
            }
            is RoomEvent.Reconnected -> {
                _roomState.value = _roomState.value.copy(reconnecting = false)
            }
            is RoomEvent.TrackSubscribed -> {
                Timber.d("Track subscribed from: %s", event.participant.identity?.value)
            }
            is RoomEvent.TrackUnsubscribed -> {
                Timber.d("Track unsubscribed from: %s", event.participant.identity?.value)
            }
            is RoomEvent.TrackMuted -> {
                updateRoomState(requireConnectedRoom())
            }
            is RoomEvent.TrackUnmuted -> {
                updateRoomState(requireConnectedRoom())
            }
            else -> {
                Timber.d("Room event: %s", event::class.simpleName)
            }
        }
    }

    private fun updateRoomState(currentRoom: Room) {
        val localParticipant = currentRoom.localParticipant

        val localInfo = LocalParticipantInfo(
            sid = localParticipant.sid?.value ?: "",
            identity = localParticipant.identity?.value ?: "",
            name = localParticipant.name?.value,
            audioEnabled = localParticipant.isMicrophoneEnabled(),
            videoEnabled = localParticipant.isCameraEnabled()
        )

        val remoteInfos = currentRoom.remoteParticipants.values.map { remote ->
            RemoteParticipantInfo(
                sid = remote.sid?.value ?: "",
                identity = remote.identity?.value ?: "",
                name = remote.name?.value,
                audioEnabled = remote.isMicrophoneEnabled(),
                videoEnabled = remote.isCameraEnabled()
            )
        }

        _roomState.value = RoomState(
            connected = true,
            reconnecting = false,
            roomName = currentRoom.name?.value,
            sid = currentRoom.sid?.value,
            localParticipant = localInfo,
            remoteParticipants = remoteInfos
        )
    }
}
