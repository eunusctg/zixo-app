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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

            // Write call signaling data
            val callData = mapOf(
                "callId" to callId,
                "callerUid" to myUid,
                "callerDisplayName" to (firebaseAuth.currentUser?.displayName ?: ""),
                "calleeUid" to targetUid,
                "isVideoCall" to isVideoCall,
                "status" to "ringing",
                "createdAt" to System.currentTimeMillis()
            )
            realtimeDb.getReference("calls").child(callId).setValue(callData).await()

            // Start foreground service
            CallForegroundService.start(
                context = context,
                callType = if (isVideoCall) CallForegroundService.CALL_TYPE_VIDEO else CallForegroundService.CALL_TYPE_AUDIO,
                callerName = contact.contactDisplayName,
                callId = callId
            )

            // Initialize WebRTC peer connection and create offer
            webRtcClient.initializePeerConnection(isVideoCall)
            val sdpOffer = webRtcClient.createOffer()

            // Send the offer via Firebase signaling
            signalingClient.sendOffer(callId, myUid, sdpOffer)

            // Listen for answer
            signalingClient.observeAnswer(callId).collect { answer ->
                if (answer != null) {
                    webRtcClient.setRemoteAnswer(answer)

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

            // Update call status in Firebase
            realtimeDb.getReference("calls").child(callId).child("status")
                .setValue("ended").await()

            // Clean up WebRTC resources
            webRtcClient.disconnect()

            // Stop foreground service
            CallForegroundService.stop(context)

            // Log the call
            logCall(callId, currentState, durationSeconds)

            _callState.value = CallState.ENDED(
                callId = callId,
                durationSeconds = durationSeconds,
                endReason = CallEndReason.COMPLETED
            )
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to end call")
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
        "isGroupCall" to entry.isGroupCall,
        "duration" to entry.duration,
        "timestamp" to entry.timestamp,
        "endReason" to entry.endReason.name,
        "participantUids" to listOf(entry.callerUid, entry.calleeUid),
        "threadId" to entry.threadId,
        "isRead" to entry.isRead
    )
}
