package com.zixo.app.domain.repository

import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.CallState
import kotlinx.coroutines.flow.Flow

/**
 * Call Repository Interface — Pure WebRTC Call Engine with Firebase Signaling
 *
 * All WebRTC operations (PeerConnectionFactory, SDP creation, ICE handling)
 * run on Dispatchers.IO. The Android Main Thread is NEVER blocked by call operations.
 *
 * Signaling uses Firebase Realtime Database (/calls/{callId}/) for
 * SDP Offer/Answer exchange and ICE candidate relay.
 *
 * Zero-trust enforcement: All call initiation methods verify mutual
 * contact status through [ContactRepository.verifyMutualContact]
 * before executing. Non-contact calls are blocked at this boundary.
 */
interface CallRepository {

    /**
     * Initiates a 1-on-1 call with a mutual contact.
     *
     * Before initiating, this method verifies that the target user
     * is a verified mutual contact. If not, the call is rejected.
     * All WebRTC operations (PeerConnection, SDP, ICE) run on Dispatchers.IO.
     *
     * @param targetUid The UID of the contact to call.
     * @param isVideoCall Whether this is a video call (false = audio only).
     * @return A flow emitting the [CallState] transitions.
     */
    fun initiateCall(targetUid: String, isVideoCall: Boolean): Flow<CallState>

    /**
     * Observes incoming calls in real-time via Firebase Realtime DB listener.
     *
     * @return A flow emitting [CallState.RINGING] for each incoming call.
     */
    fun observeIncomingCalls(): Flow<CallState>

    /**
     * Accepts an incoming call that is currently in the RINGING state.
     * Connects WebRTC PeerConnection and publishes local audio/video tracks.
     *
     * @param callId The ID of the call to accept.
     * @return A flow emitting the [CallState] transitions.
     */
    fun answerCall(callId: String): Flow<CallState>

    /**
     * Declines an incoming call that is currently in the RINGING state.
     *
     * @param callId The ID of the call to decline.
     * @return A flow emitting Result success or failure.
     */
    fun declineCall(callId: String): Flow<Result<Unit>>

    /**
     * Ends the current active call.
     * Disconnects WebRTC PeerConnection, removes all tracks,
     * and transitions the call state to [CallState.ENDED].
     *
     * @param callId The ID of the call to end.
     * @return A flow emitting Result success or failure.
     */
    fun endCall(callId: String): Flow<Result<Unit>>

    /**
     * Toggles the local microphone mute state during an active call.
     *
     * @param callId The ID of the active call.
     * @param isMuted Whether the microphone should be muted.
     * @return A flow emitting Result success or failure.
     */
    fun toggleMute(callId: String, isMuted: Boolean): Flow<Result<Unit>>

    /**
     * Toggles the local camera on/off during an active video call.
     *
     * @param callId The ID of the active call.
     * @param isCameraOff Whether the camera should be turned off.
     * @return A flow emitting Result success or failure.
     */
    fun toggleCamera(callId: String, isCameraOff: Boolean): Flow<Result<Unit>>

    /**
     * Toggles the speaker on/off during an active call.
     *
     * @param callId The ID of the active call.
     * @param isSpeakerOn Whether the speaker should be enabled.
     * @return A flow emitting Result success or failure.
     */
    fun toggleSpeaker(callId: String, isSpeakerOn: Boolean): Flow<Result<Unit>>

    /**
     * Observes the current call state for a specific call.
     *
     * @param callId The ID of the call to observe.
     * @return A flow emitting the current [CallState].
     */
    fun observeCallState(callId: String): Flow<CallState>

    /**
     * Gets the call history for the current user.
     * Uses Firestore addSnapshotListener for real-time updates.
     *
     * @return A flow emitting the list of past [CallLogEntry] entries.
     */
    fun getCallHistory(): Flow<List<CallLogEntry>>
}
