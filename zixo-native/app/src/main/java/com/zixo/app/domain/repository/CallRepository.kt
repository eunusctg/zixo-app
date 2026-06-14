package com.zixo.app.domain.repository

import com.zixo.app.domain.model.CallEndReason
import com.zixo.app.domain.model.CallState
import kotlinx.coroutines.flow.Flow

/**
 * Call Repository Interface — LiveKit WebRTC Call Engine
 *
 * All LiveKit session parameters, peer allocations, ICE setups, token
 * collections, and audio track rendering components are isolated in
 * asynchronous background workers running under Dispatchers.IO.
 * The Android Main Thread is NEVER blocked by call operations.
 *
 * Zero-trust enforcement: All call initiation methods verify mutual
 * contact status through [ContactRepository.checkCommunicationGate]
 * before executing. Non-contact calls are blocked at this boundary.
 */
interface CallRepository {

    /**
     * Observes the current call state in real-time.
     *
     * The UI collects this StateFlow to render the appropriate call
     * overlay (idle, dialing, ringing, connected) as a frosted
     * translucent glass canvas over the chat interface.
     *
     * @return A flow emitting the current [CallState].
     */
    fun observeCallState(): Flow<CallState>

    /**
     * Initiates a 1-on-1 audio call with a mutual contact.
     *
     * Before initiating, this method verifies that the target user
     * is a verified mutual contact. If not, the call is rejected.
     * All LiveKit operations (token fetch, room connect, track publish)
     * run on Dispatchers.IO to prevent Main Thread blocking.
     *
     * @param targetUid The UID of the contact to call.
     * @return A flow emitting the [CallState] transitions.
     */
    fun initiateAudioCall(targetUid: String): Flow<CallState>

    /**
     * Initiates a 1-on-1 video call with a mutual contact.
     *
     * Before initiating, this method verifies that the target user
     * is a verified mutual contact. If not, the call is rejected.
     *
     * @param targetUid The UID of the contact to call.
     * @return A flow emitting the [CallState] transitions.
     */
    fun initiateVideoCall(targetUid: String): Flow<CallState>

    /**
     * Initiates a group audio call in a LiveKit Room.
     *
     * All participants must be verified mutual contacts.
     * Uses LiveKit Room multipoint protocols with participant
     * track configurations that update as members join/leave.
     *
     * @param threadId The ID of the group thread to start a call in.
     * @return A flow emitting the [CallState] transitions.
     */
    fun initiateGroupAudioCall(threadId: String): Flow<CallState>

    /**
     * Initiates a group video call in a LiveKit Room.
     *
     * @param threadId The ID of the group thread to start a call in.
     * @return A flow emitting the [CallState] transitions.
     */
    fun initiateGroupVideoCall(threadId: String): Flow<CallState>

    /**
     * Accepts an incoming call that is currently in the RINGING state.
     *
     * Connects to the LiveKit Room and publishes local audio/video tracks.
     *
     * @param callId The ID of the call to accept.
     */
    suspend fun acceptCall(callId: String)

    /**
     * Declines an incoming call that is currently in the RINGING state.
     *
     * @param callId The ID of the call to decline.
     */
    suspend fun declineCall(callId: String)

    /**
     * Ends the current active call.
     *
     * Disconnects from the LiveKit Room, unpublishes all tracks,
     * and transitions the call state to [CallState.ENDED].
     * The foreground service notification is removed.
     */
    suspend fun endCall()

    /**
     * Toggles the local microphone mute state during an active call.
     *
     * @param isMuted Whether the microphone should be muted.
     */
    suspend fun setMuted(isMuted: Boolean)

    /**
     * Toggles the local camera on/off during an active video call.
     *
     * @param isCameraOff Whether the camera should be turned off.
     */
    suspend fun setCameraOff(isCameraOff: Boolean)

    /**
     * Toggles the speaker on/off during an active call.
     *
     * @param isSpeakerOn Whether the speaker should be enabled.
     */
    suspend fun setSpeakerOn(isSpeakerOn: Boolean)

    /**
     * Switches between front and back camera during a video call.
     */
    suspend fun switchCamera()

    /**
     * Observes the call duration for the current active call.
     *
     * @return A flow emitting the duration in seconds, updated every second.
     */
    fun observeCallDuration(): Flow<Long>

    /**
     * Observes the call history for the current user.
     *
     * Uses Firestore [addSnapshotListener] for real-time updates.
     *
     * @return A flow emitting the list of past call entries.
     */
    fun observeCallHistory(): Flow<List<CallHistoryEntry>>
}

/**
 * Represents a past call entry in the call history.
 */
data class CallHistoryEntry(
    val id: String = "",
    val callId: String = "",
    val callerUid: String = "",
    val callerDisplayName: String = "",
    val callerAvatarUrl: String = "",
    val calleeUid: String = "",
    val calleeDisplayName: String = "",
    val calleeAvatarUrl: String = "",
    val isVideoCall: Boolean = false,
    val durationSeconds: Long = 0L,
    val timestamp: Long = 0L,
    val endReason: CallEndReason = CallEndReason.COMPLETED,
    val threadId: String = ""
)
