package com.zixo.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zixo.app.data.remote.webrtc.FirebaseSignalingClient
import com.zixo.app.data.remote.webrtc.WebRtcClient
import com.zixo.app.data.remote.webrtc.CallForegroundService
import com.zixo.app.domain.model.CallDirection
import com.zixo.app.domain.model.CallEndReason
import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ContactRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.webrtc.IceCandidate
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [CallRepository].
 *
 * Manages WebRTC calls using pure WebRTC (io.github.webrtc-sdk:android)
 * with Firebase Realtime Database for signaling. NO LiveKit dependency.
 *
 * All WebRTC operations (PeerConnectionFactory, SDP, ICE) run on
 * [Dispatchers.IO]. The Android Main Thread is NEVER blocked by call operations.
 *
 * Zero-trust enforcement: All call initiation methods verify mutual
 * contact status through [ContactRepository.verifyMutualContact]
 * before executing. Non-contact calls are blocked at this boundary.
 */
@Singleton
class CallRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val contactRepository: ContactRepository,
    private val webRtcClient: WebRtcClient,
    private val signalingClient: FirebaseSignalingClient,
    @ApplicationContext private val context: Context
) : CallRepository {

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    private val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _incomingCalls = MutableStateFlow<CallState>(CallState.IDLE)

    private var durationJob: Job? = null
    private var connectedAt: Long = 0L

    private val callLogCollection get() = firestore.collection("call_log")

    // ════════════════════════════════════════════════════════════════════════
    //  Firebase Realtime DB Signaling Flows — Thread-Isolated
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Observes the real-time state of a call session from Firebase Realtime DB.
     *
     * Provides a continuous [com.google.firebase.database.ValueEventListener] on
     * `/calls/{callId}/`, streaming every state change as a Map. All listener
     * operations are safely decoupled from the UI thread via [callbackFlow] on
     * [Dispatchers.IO].
     *
     * This is the primary mechanism by which the calling UI reacts to remote
     * state transitions without polling.
     */
    override fun observeCallSession(callId: String): Flow<Map<String, Any>> = callbackFlow {
        val callRef = realtimeDb.getReference("calls/$callId")
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                try {
                    val data = snapshot.value as? Map<String, Any> ?: emptyMap()
                    trySend(data)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing call session data for callId: %s", callId)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Timber.e(error.toException(), "Call session listener cancelled for callId: %s", callId)
                close(error.toException())
            }
        }
        callRef.addValueEventListener(listener)
        awaitClose { callRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Observes ICE candidates for a specific call from Firebase Realtime DB.
     *
     * Listens to `/calls/{callId}/iceCandidates/` with a [com.google.firebase.database.ChildEventListener],
     * streaming each newly added ICE candidate as it arrives from the remote peer.
     * Candidates are parsed into [IceCandidate] objects and forwarded to the
     * WebRTC engine for addition to the PeerConnection.
     */
    override fun observeIceCandidates(callId: String): Flow<IceCandidate> = callbackFlow {
        val candidatesRef = realtimeDb.getReference("calls/$callId/iceCandidates")
        val childListener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {
                try {
                    val sdpMid = snapshot.child("sdpMid").getValue(String::class.java) ?: ""
                    val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                    val sdp = snapshot.child("sdp").getValue(String::class.java) ?: ""
                    if (sdp.isNotBlank()) {
                        trySend(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing ICE candidate for callId: %s", callId)
                }
            }

            override fun onChildChanged(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) { /* ICE candidates are append-only */ }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}

            override fun onChildMoved(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {}

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Timber.e(error.toException(), "ICE candidate listener cancelled for callId: %s", callId)
                close(error.toException())
            }
        }
        candidatesRef.addChildEventListener(childListener)
        awaitClose { candidatesRef.removeEventListener(childListener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Emits a call state update to Firebase Realtime DB.
     *
     * Writes the state string to `/calls/{callId}/callState`.
     * This operation runs on [Dispatchers.IO] and never blocks the Main Thread.
     */
    override suspend fun emitCallState(callId: String, state: String) = withContext(Dispatchers.IO) {
        try {
            realtimeDb.getReference("calls/$callId/callState").setValue(state)
            Timber.d("Call state emitted: %s for callId: %s", state, callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to emit call state: %s for callId: %s", state, callId)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Firebase Realtime DB Signaling Flows — Thread-Isolated
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Observes the real-time state of a call session from Firebase Realtime DB.
     *
     * Provides a continuous [ValueEventListener] on `/calls/{callId}/`,
     * streaming every state change as a Map. All listener operations are
     * safely decoupled from the UI thread via [callbackFlow] on [Dispatchers.IO].
     *
     * This is the primary mechanism by which the calling UI reacts to remote
     * state transitions without polling.
     */
    override fun observeCallSession(callId: String): Flow<Map<String, Any>> = callbackFlow {
        val callRef = realtimeDb.getReference("calls/$callId")
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                try {
                    val data = snapshot.value as? Map<String, Any> ?: emptyMap()
                    trySend(data)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing call session data for callId: %s", callId)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Timber.e(error.toException(), "Call session listener cancelled for callId: %s", callId)
                close(error.toException())
            }
        }
        callRef.addValueEventListener(listener)
        awaitClose { callRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Observes ICE candidates for a specific call from Firebase Realtime DB.
     *
     * Listens to `/calls/{callId}/iceCandidates/` with a [ChildEventListener],
     * streaming each newly added ICE candidate as it arrives from the remote peer.
     * Candidates are parsed into [IceCandidate] objects and forwarded to the
     * WebRTC engine for addition to the PeerConnection.
     */
    override fun observeIceCandidates(callId: String): Flow<IceCandidate> = callbackFlow {
        val candidatesRef = realtimeDb.getReference("calls/$callId/iceCandidates")
        val childListener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {
                try {
                    val sdpMid = snapshot.child("sdpMid").getValue(String::class.java) ?: ""
                    val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                    val sdp = snapshot.child("sdp").getValue(String::class.java) ?: ""
                    if (sdp.isNotBlank()) {
                        trySend(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing ICE candidate for callId: %s", callId)
                }
            }

            override fun onChildChanged(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) { /* ICE candidates are append-only */ }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}

            override fun onChildMoved(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?
            ) {}

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Timber.e(error.toException(), "ICE candidate listener cancelled for callId: %s", callId)
                close(error.toException())
            }
        }
        candidatesRef.addChildEventListener(childListener)
        awaitClose { candidatesRef.removeEventListener(childListener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Emits a call state update to Firebase Realtime DB.
     *
     * Writes the state string to `/calls/{callId}/callState`.
     * This operation runs on [Dispatchers.IO] and never blocks the Main Thread.
     */
    override suspend fun emitCallState(callId: String, state: String) = withContext(Dispatchers.IO) {
        try {
            realtimeDb.getReference("calls/$callId/callState").setValue(state)
            Timber.d("Call state emitted: %s for callId: %s", state, callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to emit call state: %s for callId: %s", state, callId)
        }
    }

    // ── Initiate Call ─────────────────────────────────────────────────────────

    override fun initiateCall(targetUid: String, isVideoCall: Boolean): Flow<CallState> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(CallState.IDLE)
            return@flow
        }

        try {
            // Zero-trust: verify mutual contact before initiating
            var gateResult: CommunicationGate? = null
            contactRepository.verifyMutualContact(targetUid).collect { gateResult = it }

            if (gateResult !is CommunicationGate.Allowed) {
                Timber.w("Call blocked — not a mutual contact: %s", targetUid)
                emit(CallState.IDLE)
                return@flow
            }

            val contact = (gateResult as CommunicationGate.Allowed).contact

            // Create the call document in Firebase Realtime DB
            val callId = UUID.randomUUID().toString()

            _callState.value = CallState.DIALING(
                callId = callId,
                targetUid = targetUid,
                targetDisplayName = contact.contactDisplayName,
                targetAvatarUrl = contact.contactAvatarUrl,
                isVideoCall = isVideoCall
            )
            emit(_callState.value)

            // Start foreground service BEFORE WebRTC initialization
            // This guarantees Android does not terminate media streams
            CallForegroundService.start(
                context = context,
                callType = if (isVideoCall) CallForegroundService.CALL_TYPE_VIDEO else CallForegroundService.CALL_TYPE_AUDIO,
                callerName = contact.contactDisplayName,
                callId = callId
            )

            // Create the call entry in Firebase Realtime DB with structured data
            signalingClient.createCall(callId, myUid, targetUid, isVideoCall)

            // Initialize WebRTC peer connection on Dispatchers.IO
            // AudioManager is calibrated inside initializePeerConnection
            webRtcClient.initializePeerConnection(isVideoCall)

            // Set up ICE candidate forwarding: local ICE → Firebase signaling
            webRtcClient.onIceCandidateGenerated = { candidate ->
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        signalingClient.sendIceCandidate(
                            callId = callId,
                            senderUid = myUid,
                            targetUid = targetUid,
                            sdpMid = candidate.sdpMid,
                            sdpMLineIndex = candidate.sdpMLineIndex,
                            sdp = candidate.sdp
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to forward ICE candidate to signaling")
                    }
                }
            }

            // Create SDP offer
            val sdpOffer = webRtcClient.createOffer()

            // Send the offer via Firebase signaling
            signalingClient.sendOffer(callId, myUid, targetUid, sdpOffer)

            // Update call status to ringing
            signalingClient.updateCallStatus(callId, "ringing")

            // Observe answer and ICE candidates concurrently
            var isAnswerReceived = false

            // Listen for answer
            signalingClient.observeAnswer(callId, myUid).collect { answerData ->
                if (answerData != null && !isAnswerReceived) {
                    isAnswerReceived = true
                    webRtcClient.setRemoteAnswer(answerData.sdp)

                    // Update call status to connected
                    signalingClient.updateCallStatus(callId, "connected")

                    _callState.value = CallState.CONNECTED(
                        callId = callId,
                        targetUid = targetUid,
                        targetDisplayName = contact.contactDisplayName,
                        targetAvatarUrl = contact.contactAvatarUrl,
                        isVideoCall = isVideoCall,
                        connectedAt = System.currentTimeMillis()
                    )
                    connectedAt = System.currentTimeMillis()
                    emit(_callState.value)

                    // Start duration tracking
                    startDurationTracking(callId)

                    // Start observing ICE candidates for this call
                    observeIceCandidates(callId, myUid)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate call")
            _callState.value = CallState.IDLE
            emit(CallState.IDLE)
        }
    }.flowOn(Dispatchers.IO)

    // ── Observe Incoming Calls ────────────────────────────────────────────────

    override fun observeIncomingCalls(): Flow<CallState> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(CallState.IDLE)
            close()
            return@callbackFlow
        }

        val callsRef = realtimeDb.getReference("calls")
        val listener = callsRef.orderByChild("calleeUid").equalTo(myUid)
            .addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                    try {
                        val status = snapshot.child("status").getValue(String::class.java)
                        if (status == "ringing") {
                            val callId = snapshot.key ?: return
                            val callerUid = snapshot.child("callerUid").getValue(String::class.java) ?: return
                            val callerDisplayName = snapshot.child("callerDisplayName").getValue(String::class.java) ?: ""
                            val isVideoCall = snapshot.child("isVideoCall").getValue(Boolean::class.java) ?: false

                            val incomingState = CallState.RINGING(
                                callId = callId,
                                callerUid = callerUid,
                                callerDisplayName = callerDisplayName,
                                callerAvatarUrl = "",
                                isVideoCall = isVideoCall
                            )
                            _incomingCalls.value = incomingState
                            trySend(incomingState)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing incoming call")
                    }
                }

                override fun onChildChanged(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) { /* handled in observeCallState */ }

                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
                override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Timber.e(error.toException(), "Incoming calls listener cancelled")
                }
            })

        awaitClose { callsRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // ── Answer Call ───────────────────────────────────────────────────────────

    override fun answerCall(callId: String): Flow<CallState> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            // Get call data
            val callSnapshot = realtimeDb.getReference("calls").child(callId).get().await()
            val callerUid = callSnapshot.child("callerUid").getValue(String::class.java) ?: ""
            val isVideoCall = callSnapshot.child("isVideoCall").getValue(Boolean::class.java) ?: false

            // Verify mutual contact
            var gateResult: CommunicationGate? = null
            contactRepository.verifyMutualContact(callerUid).collect { gateResult = it }

            if (gateResult !is CommunicationGate.Allowed) {
                Timber.w("Cannot answer call — not a mutual contact")
                emit(CallState.IDLE)
                return@flow
            }

            val contact = (gateResult as CommunicationGate.Allowed).contact

            // Update call status
            realtimeDb.getReference("calls").child(callId).child("status")
                .setValue("connected").await()

            // Start foreground service
            CallForegroundService.start(
                context = context,
                callType = if (isVideoCall) CallForegroundService.CALL_TYPE_VIDEO else CallForegroundService.CALL_TYPE_AUDIO,
                callerName = contact.contactDisplayName,
                callId = callId
            )

            // Initialize WebRTC and observe offer
            webRtcClient.initializePeerConnection(isVideoCall)

            signalingClient.observeOffer(callId).collect { offer ->
                if (offer != null) {
                    webRtcClient.setRemoteOffer(offer)
                    val sdpAnswer = webRtcClient.createAnswer()
                    signalingClient.sendAnswer(callId, myUid, sdpAnswer)

                    _callState.value = CallState.CONNECTED(
                        callId = callId,
                        targetUid = callerUid,
                        targetDisplayName = contact.contactDisplayName,
                        targetAvatarUrl = contact.contactAvatarUrl,
                        isVideoCall = isVideoCall,
                        connectedAt = System.currentTimeMillis()
                    )
                    connectedAt = System.currentTimeMillis()
                    emit(_callState.value)
                    startDurationTracking(callId)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to answer call")
            _callState.value = CallState.IDLE
            emit(CallState.IDLE)
        }
    }.flowOn(Dispatchers.IO)

    // ── Decline Call ──────────────────────────────────────────────────────────

    override fun declineCall(callId: String): Flow<Result<Unit>> = flow {
        try {
            realtimeDb.getReference("calls").child(callId).child("status")
                .setValue("declined").await()
            _callState.value = CallState.ENDED(
                callId = callId,
                endReason = CallEndReason.DECLINED
            )
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to decline call")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── End Call ──────────────────────────────────────────────────────────────

    override fun endCall(callId: String): Flow<Result<Unit>> = flow {
        try {
            val currentState = _callState.value
            val durationSeconds = if (currentState is CallState.CONNECTED) {
                (System.currentTimeMillis() - connectedAt) / 1000
            } else 0L

            // Update call status in Firebase via signaling client
            signalingClient.updateCallStatus(callId, "ended")

            // Clean up WebRTC resources (releases audio focus too)
            webRtcClient.disconnect()

            // Clean up signaling data from Firebase Realtime DB
            signalingClient.cleanupSignalingData(callId)

            // Stop foreground service
            CallForegroundService.stop(context)

            // Cancel duration tracking
            durationJob?.cancel()
            durationJob = null

            // Log the call
            logCall(callId, currentState, durationSeconds)

            _callState.value = CallState.ENDED(
                callId = callId,
                durationSeconds = durationSeconds,
                endReason = CallEndReason.COMPLETED
            )
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to end call — forcing cleanup")
            // Force cleanup even if something fails
            try { webRtcClient.disconnect() } catch (_: Exception) {}
            try { CallForegroundService.stop(context) } catch (_: Exception) {}
            try { signalingClient.cleanupSignalingData(callId) } catch (_: Exception) {}
            _callState.value = CallState.IDLE
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Toggle Mute ───────────────────────────────────────────────────────────

    override fun toggleMute(callId: String, isMuted: Boolean): Flow<Result<Unit>> = flow {
        try {
            webRtcClient.setMuted(isMuted)
            val currentState = _callState.value
            if (currentState is CallState.CONNECTED) {
                _callState.value = currentState.copy(isMuted = isMuted)
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle mute")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Toggle Camera ─────────────────────────────────────────────────────────

    override fun toggleCamera(callId: String, isCameraOff: Boolean): Flow<Result<Unit>> = flow {
        try {
            if (isCameraOff) {
                webRtcClient.disableCamera()
            } else {
                webRtcClient.enableCamera()
            }
            val currentState = _callState.value
            if (currentState is CallState.CONNECTED) {
                _callState.value = currentState.copy(isCameraOff = isCameraOff)
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle camera")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Toggle Speaker ────────────────────────────────────────────────────────

    override fun toggleSpeaker(callId: String, isSpeakerOn: Boolean): Flow<Result<Unit>> = flow {
        try {
            webRtcClient.setSpeakerOn(isSpeakerOn)
            val currentState = _callState.value
            if (currentState is CallState.CONNECTED) {
                _callState.value = currentState.copy(isSpeakerOn = isSpeakerOn)
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle speaker")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Observe Call State ────────────────────────────────────────────────────

    override fun observeCallState(callId: String): Flow<CallState> = flow {
        callState.collect { state ->
            if (state is CallState.IDLE ||
                (state is CallState.DIALING && state.callId == callId) ||
                (state is CallState.RINGING && state.callId == callId) ||
                (state is CallState.CONNECTED && state.callId == callId) ||
                (state is CallState.ENDED && state.callId == callId)
            ) {
                emit(state)
            }
        }
    }.flowOn(Dispatchers.IO)

    // ── Call History ──────────────────────────────────────────────────────────

    override fun getCallHistory(): Flow<List<CallLogEntry>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = callLogCollection
            .whereArrayContains("participantUids", myUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing call history")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val calls = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToCallLogEntry(doc, myUid)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map call log: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(calls)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── ICE Candidate Observer ──────────────────────────────────────────────

    /**
     * Observes remote ICE candidates from the signaling server and adds them
     * to the local PeerConnection. This runs continuously during a call to
     * ensure that new ICE candidates are processed as they arrive.
     *
     * @param callId The call ID.
     * @param localUid The local user's UID (to filter out self-sent candidates).
     */
    private fun observeIceCandidates(callId: String, localUid: String) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                signalingClient.observeIceCandidates(callId, localUid).collect { candidates ->
                    for (candidate in candidates) {
                        try {
                            webRtcClient.addIceCandidate(
                                candidate.sdpMid,
                                candidate.sdpMLineIndex,
                                candidate.sdp
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to add remote ICE candidate")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ICE candidate observer failed for call: %s", callId)
            }
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private fun startDurationTracking(callId: String) {
        durationJob?.cancel()
        durationJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000)
                val currentState = _callState.value
                if (currentState is CallState.CONNECTED) {
                    val elapsed = (System.currentTimeMillis() - connectedAt) / 1000
                    _callState.value = currentState.copy(durationSeconds = elapsed)
                }
            }
        }
    }

    private suspend fun logCall(callId: String, state: CallState, durationSeconds: Long) {
        try {
            val myUid = currentUid ?: return
            val entry = when (state) {
                is CallState.CONNECTED -> CallLogEntry(
                    id = UUID.randomUUID().toString(),
                    callId = callId,
                    callerUid = myUid,
                    calleeUid = state.targetUid,
                    callerName = firebaseAuth.currentUser?.displayName ?: "",
                    calleeName = state.targetDisplayName,
                    type = CallDirection.OUTGOING,
                    isVideoCall = state.isVideoCall,
                    callType = if (state.isVideoCall) com.zixo.app.domain.model.CallTechnology.WEBRTC_VIDEO else com.zixo.app.domain.model.CallTechnology.WEBRTC_AUDIO,
                    duration = durationSeconds,
                    timestamp = System.currentTimeMillis(),
                    endReason = CallEndReason.COMPLETED
                )
                is CallState.DIALING -> CallLogEntry(
                    id = UUID.randomUUID().toString(),
                    callId = callId,
                    callerUid = myUid,
                    calleeUid = state.targetUid,
                    callerName = firebaseAuth.currentUser?.displayName ?: "",
                    calleeName = state.targetDisplayName,
                    type = CallDirection.OUTGOING,
                    isVideoCall = state.isVideoCall,
                    callType = if (state.isVideoCall) com.zixo.app.domain.model.CallTechnology.WEBRTC_VIDEO else com.zixo.app.domain.model.CallTechnology.WEBRTC_AUDIO,
                    duration = 0L,
                    timestamp = System.currentTimeMillis(),
                    endReason = CallEndReason.CANCELLED
                )
                else -> return
            }

            callLogCollection.document(entry.id).set(entryToFirestoreMap(entry)).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to log call")
        }
    }

    private fun mapToCallLogEntry(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        myUid: String
    ): CallLogEntry? {
        return try {
            val callerUid = doc.getString("callerUid") ?: return null
            val calleeUid = doc.getString("calleeUid") ?: return null

            val direction = when {
                callerUid == myUid -> CallDirection.OUTGOING
                else -> CallDirection.INCOMING
            }

            val endReasonStr = doc.getString("endReason") ?: "COMPLETED"
            val endReason = try { CallEndReason.valueOf(endReasonStr) }
                catch (_: Exception) { CallEndReason.COMPLETED }

            val callTechnologyStr = doc.getString("callType") ?: "WEBRTC_AUDIO"

            CallLogEntry(
                id = doc.id,
                callId = doc.getString("callId") ?: "",
                callerUid = callerUid,
                calleeUid = calleeUid,
                callerName = doc.getString("callerName") ?: "",
                calleeName = doc.getString("calleeName") ?: "",
                callerAvatar = doc.getString("callerAvatar"),
                calleeAvatar = doc.getString("calleeAvatar"),
                type = if (endReason == CallEndReason.MISSED) CallDirection.MISSED else direction,
                isVideoCall = doc.getBoolean("isVideoCall") ?: false,
                callType = try { com.zixo.app.domain.model.CallTechnology.valueOf(callTechnologyStr) } catch (_: Exception) { com.zixo.app.domain.model.CallTechnology.WEBRTC_AUDIO },
                isGroupCall = doc.getBoolean("isGroupCall") ?: false,
                duration = doc.getLong("duration") ?: 0L,
                timestamp = doc.getLong("timestamp") ?: 0L,
                endReason = endReason,
                threadId = doc.getString("threadId") ?: "",
                isRead = doc.getBoolean("isRead") ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map call log entry")
            null
        }
    }

    private fun entryToFirestoreMap(entry: CallLogEntry): Map<String, Any?> = mapOf(
        "callId" to entry.callId,
        "callerUid" to entry.callerUid,
        "calleeUid" to entry.calleeUid,
        "callerName" to entry.callerName,
        "calleeName" to entry.calleeName,
        "callerAvatar" to entry.callerAvatar,
        "calleeAvatar" to entry.calleeAvatar,
        "type" to entry.type.name,
        "isVideoCall" to entry.isVideoCall,
        "callType" to entry.callType.name,
        "isGroupCall" to entry.isGroupCall,
        "duration" to entry.duration,
        "timestamp" to entry.timestamp,
        "endReason" to entry.endReason.name,
        "participantUids" to listOf(entry.callerUid, entry.calleeUid),
        "threadId" to entry.threadId,
        "isRead" to entry.isRead
    )

    // ── Get All Calls ────────────────────────────────────────────────────────

    override fun getAllCalls(): Flow<List<CallLogEntry>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = callLogCollection
            .whereArrayContains("participantUids", myUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing call history")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val calls = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToCallLogEntry(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map call log: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(calls)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Clear Call History ────────────────────────────────────────────────────

    override fun clearCallHistory(): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            val snapshot = callLogCollection
                .whereArrayContains("participantUids", myUid)
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Timber.d("Call history cleared: %d entries removed", snapshot.size())
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear call history")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Mapping Helper ────────────────────────────────────────────────────────

    private fun mapToCallLogEntry(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): CallLogEntry? {
        return try {
            val typeStr = doc.getString("type") ?: "OUTGOING"
            val endReasonStr = doc.getString("endReason") ?: "COMPLETED"
            val callTechnologyStr = doc.getString("callType") ?: "WEBRTC_AUDIO"
            val isVideo = doc.getBoolean("isVideoCall") ?: false

            CallLogEntry(
                id = doc.id,
                callId = doc.getString("callId") ?: "",
                callerUid = doc.getString("callerUid") ?: "",
                calleeUid = doc.getString("calleeUid") ?: "",
                callerName = doc.getString("callerName") ?: "",
                calleeName = doc.getString("calleeName") ?: "",
                callerAvatar = doc.getString("callerAvatar"),
                calleeAvatar = doc.getString("calleeAvatar"),
                type = try { CallDirection.valueOf(typeStr) } catch (_: Exception) { CallDirection.OUTGOING },
                isVideoCall = isVideo,
                callType = try { com.zixo.app.domain.model.CallTechnology.valueOf(callTechnologyStr) } catch (_: Exception) { com.zixo.app.domain.model.CallTechnology.WEBRTC_AUDIO },
                isGroupCall = doc.getBoolean("isGroupCall") ?: false,
                duration = doc.getLong("duration") ?: 0L,
                timestamp = doc.getLong("timestamp") ?: 0L,
                endReason = try { CallEndReason.valueOf(endReasonStr) } catch (_: Exception) { CallEndReason.COMPLETED },
                threadId = doc.getString("threadId") ?: "",
                isRead = doc.getBoolean("isRead") ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map CallLogEntry from document: %s", doc.id)
            null
        }
    }
}
