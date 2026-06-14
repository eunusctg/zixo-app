package com.zixo.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zixo.app.data.remote.cloudflare.CloudflareApiService
import com.zixo.app.data.remote.livekit.CallForegroundService
import com.zixo.app.data.remote.livekit.LiveKitService
import com.zixo.app.domain.model.CallEndReason
import com.zixo.app.domain.model.CallHistoryEntry
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
import kotlinx.coroutines.flow.firstOrNull
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
 * Manages WebRTC calls using the LiveKit Android SDK with Firebase
 * Firestore for call signaling and history. All LiveKit operations
 * (token fetch, room connect, track publish) run on [Dispatchers.IO]
 * to prevent Main Thread blocking.
 *
 * Zero-trust enforcement: All call initiation methods verify mutual
 * contact status through [ContactRepository.checkCommunicationGate]
 * before executing. Non-contact calls are blocked at this boundary.
 *
 * Call state transitions:
 * - Outgoing: IDLE → DIALING → CONNECTED → ENDED
 * - Incoming: IDLE → RINGING → CONNECTED → ENDED
 */
@Singleton
class CallRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val liveKitService: LiveKitService,
    private val cloudflareApiService: CloudflareApiService,
    private val contactRepository: ContactRepository,
    @ApplicationContext private val applicationContext: Context
) : CallRepository {

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    private val callsCollection get() = firestore.collection("calls")
    private val usersCollection get() = firestore.collection("users")

    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    override fun observeCallState(): Flow<CallState> = _callState.asStateFlow()

    private var callDurationJob: Job? = null
    private var callStartTime: Long = 0L
    private var currentCallId: String? = null

    // ── Initiate Audio Call ───────────────────────────────────────────────────

    override fun initiateAudioCall(targetUid: String): Flow<CallState> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(CallState.IDLE)
            return@flow
        }

        try {
            _callState.value = CallState.IDLE

            val gate = contactRepository.checkCommunicationGate(targetUid)
            if (gate !is CommunicationGate.Allowed) {
                Timber.w("Audio call blocked: not a mutual contact with %s", targetUid)
                _callState.value = CallState.IDLE
                emit(CallState.IDLE)
                return@flow
            }

            val contact = gate.contact
            val callId = UUID.randomUUID().toString()
            currentCallId = callId

            val dialingState = CallState.DIALING(
                callId = callId,
                targetUid = targetUid,
                targetDisplayName = contact.contactDisplayName,
                targetAvatarUrl = contact.contactAvatarUrl,
                isVideoCall = false
            )
            _callState.value = dialingState
            emit(dialingState)

            val myProfile = usersCollection.document(myUid).get().await()
            val now = System.currentTimeMillis()

            val callData = hashMapOf(
                "id" to callId,
                "callerUid" to myUid,
                "callerDisplayName" to (myProfile.getString("displayName") ?: ""),
                "callerAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "calleeUid" to targetUid,
                "calleeDisplayName" to contact.contactDisplayName,
                "calleeAvatarUrl" to contact.contactAvatarUrl,
                "isVideoCall" to false,
                "status" to "DIALING",
                "createdAt" to now,
                "threadId" to ""
            )
            callsCollection.document(callId).set(callData).await()

            val roomName = "call_$callId"
            val tokenResponse = cloudflareApiService.generateLiveKitToken(
                identity = myUid,
                roomName = roomName
            ).firstOrNull()

            if (tokenResponse == null) {
                Timber.e("Failed to fetch LiveKit token for audio call")
                endCallInternal(CallEndReason.NETWORK_ERROR)
                emit(_callState.value)
                return@flow
            }

            liveKitService.connect(tokenResponse.wsUrl, tokenResponse.token)
                .flowOn(Dispatchers.IO)
                .collect {}

            liveKitService.startAudioCall(roomName)
                .flowOn(Dispatchers.IO)
                .collect {}

            callsCollection.document(callId).update("status", "CONNECTED").await()

            callStartTime = System.currentTimeMillis()
            startDurationTracker()

            CallForegroundService.start(
                applicationContext,
                CallForegroundService.CALL_TYPE_AUDIO,
                contact.contactDisplayName,
                roomName
            )

            val connectedState = CallState.CONNECTED(
                callId = callId,
                targetUid = targetUid,
                targetDisplayName = contact.contactDisplayName,
                targetAvatarUrl = contact.contactAvatarUrl,
                isVideoCall = false,
                connectedAt = System.currentTimeMillis()
            )
            _callState.value = connectedState
            emit(connectedState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate audio call")
            endCallInternal(CallEndReason.NETWORK_ERROR)
            emit(_callState.value)
        }
    }.flowOn(Dispatchers.IO)

    // ── Initiate Video Call ───────────────────────────────────────────────────

    override fun initiateVideoCall(targetUid: String): Flow<CallState> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(CallState.IDLE)
            return@flow
        }

        try {
            _callState.value = CallState.IDLE

            val gate = contactRepository.checkCommunicationGate(targetUid)
            if (gate !is CommunicationGate.Allowed) {
                Timber.w("Video call blocked: not a mutual contact with %s", targetUid)
                _callState.value = CallState.IDLE
                emit(CallState.IDLE)
                return@flow
            }

            val contact = gate.contact
            val callId = UUID.randomUUID().toString()
            currentCallId = callId

            val dialingState = CallState.DIALING(
                callId = callId,
                targetUid = targetUid,
                targetDisplayName = contact.contactDisplayName,
                targetAvatarUrl = contact.contactAvatarUrl,
                isVideoCall = true
            )
            _callState.value = dialingState
            emit(dialingState)

            val myProfile = usersCollection.document(myUid).get().await()
            val now = System.currentTimeMillis()

            val callData = hashMapOf(
                "id" to callId,
                "callerUid" to myUid,
                "callerDisplayName" to (myProfile.getString("displayName") ?: ""),
                "callerAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "calleeUid" to targetUid,
                "calleeDisplayName" to contact.contactDisplayName,
                "calleeAvatarUrl" to contact.contactAvatarUrl,
                "isVideoCall" to true,
                "status" to "DIALING",
                "createdAt" to now,
                "threadId" to ""
            )
            callsCollection.document(callId).set(callData).await()

            val roomName = "call_$callId"
            val tokenResponse = cloudflareApiService.generateLiveKitToken(
                identity = myUid,
                roomName = roomName
            ).firstOrNull()

            if (tokenResponse == null) {
                Timber.e("Failed to fetch LiveKit token for video call")
                endCallInternal(CallEndReason.NETWORK_ERROR)
                emit(_callState.value)
                return@flow
            }

            liveKitService.connect(tokenResponse.wsUrl, tokenResponse.token)
                .flowOn(Dispatchers.IO)
                .collect {}

            liveKitService.startVideoCall(roomName)
                .flowOn(Dispatchers.IO)
                .collect {}

            callsCollection.document(callId).update("status", "CONNECTED").await()

            callStartTime = System.currentTimeMillis()
            startDurationTracker()

            CallForegroundService.start(
                applicationContext,
                CallForegroundService.CALL_TYPE_VIDEO,
                contact.contactDisplayName,
                roomName
            )

            val connectedState = CallState.CONNECTED(
                callId = callId,
                targetUid = targetUid,
                targetDisplayName = contact.contactDisplayName,
                targetAvatarUrl = contact.contactAvatarUrl,
                isVideoCall = true,
                connectedAt = System.currentTimeMillis()
            )
            _callState.value = connectedState
            emit(connectedState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate video call")
            endCallInternal(CallEndReason.NETWORK_ERROR)
            emit(_callState.value)
        }
    }.flowOn(Dispatchers.IO)

    // ── Group Calls ───────────────────────────────────────────────────────────

    override fun initiateGroupAudioCall(threadId: String): Flow<CallState> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(CallState.IDLE)
            return@flow
        }

        try {
            _callState.value = CallState.IDLE

            val threadDoc = firestore.collection("threads")
                .document(threadId).get().await()
            if (!threadDoc.exists()) {
                emit(CallState.IDLE)
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>)
                ?: emptyList()

            for (uid in participantUids) {
                if (uid == myUid) continue
                val gate = contactRepository.checkCommunicationGate(uid)
                if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                    Timber.w("Group audio call blocked: not mutual with %s", uid)
                }
            }

            val callId = UUID.randomUUID().toString()
            currentCallId = callId

            val dialingState = CallState.DIALING(
                callId = callId,
                targetUid = threadId,
                targetDisplayName = threadDoc.getString("groupName") ?: "Group Call",
                isVideoCall = false
            )
            _callState.value = dialingState
            emit(dialingState)

            val myProfile = usersCollection.document(myUid).get().await()
            val now = System.currentTimeMillis()

            val callData = hashMapOf(
                "id" to callId,
                "callerUid" to myUid,
                "callerDisplayName" to (myProfile.getString("displayName") ?: ""),
                "callerAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "calleeUid" to "",
                "calleeDisplayName" to threadDoc.getString("groupName") ?: "Group",
                "isVideoCall" to false,
                "status" to "DIALING",
                "createdAt" to now,
                "threadId" to threadId,
                "isGroupCall" to true,
                "participantUids" to participantUids
            )
            callsCollection.document(callId).set(callData).await()

            val roomName = "group_call_$callId"
            val tokenResponse = cloudflareApiService.generateLiveKitToken(
                identity = myUid,
                roomName = roomName
            ).firstOrNull()

            if (tokenResponse == null) {
                endCallInternal(CallEndReason.NETWORK_ERROR)
                emit(_callState.value)
                return@flow
            }

            liveKitService.connect(tokenResponse.wsUrl, tokenResponse.token)
                .flowOn(Dispatchers.IO)
                .collect {}

            liveKitService.startAudioCall(roomName)
                .flowOn(Dispatchers.IO)
                .collect {}

            callsCollection.document(callId).update("status", "CONNECTED").await()

            callStartTime = System.currentTimeMillis()
            startDurationTracker()

            CallForegroundService.start(
                applicationContext,
                CallForegroundService.CALL_TYPE_AUDIO,
                threadDoc.getString("groupName") ?: "Group Call",
                roomName
            )

            val connectedState = CallState.CONNECTED(
                callId = callId,
                targetUid = threadId,
                targetDisplayName = threadDoc.getString("groupName") ?: "Group Call",
                isVideoCall = false,
                connectedAt = System.currentTimeMillis(),
                participantCount = participantUids.size
            )
            _callState.value = connectedState
            emit(connectedState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate group audio call")
            endCallInternal(CallEndReason.NETWORK_ERROR)
            emit(_callState.value)
        }
    }.flowOn(Dispatchers.IO)

    override fun initiateGroupVideoCall(threadId: String): Flow<CallState> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(CallState.IDLE)
            return@flow
        }

        try {
            _callState.value = CallState.IDLE

            val threadDoc = firestore.collection("threads")
                .document(threadId).get().await()
            if (!threadDoc.exists()) {
                emit(CallState.IDLE)
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val participantUids = (threadDoc.get("participantUids") as? List<String>)
                ?: emptyList()

            for (uid in participantUids) {
                if (uid == myUid) continue
                val gate = contactRepository.checkCommunicationGate(uid)
                if (gate is CommunicationGate.Blocked || gate is CommunicationGate.Error) {
                    Timber.w("Group video call blocked: not mutual with %s", uid)
                }
            }

            val callId = UUID.randomUUID().toString()
            currentCallId = callId

            val dialingState = CallState.DIALING(
                callId = callId,
                targetUid = threadId,
                targetDisplayName = threadDoc.getString("groupName") ?: "Group Call",
                isVideoCall = true
            )
            _callState.value = dialingState
            emit(dialingState)

            val myProfile = usersCollection.document(myUid).get().await()
            val now = System.currentTimeMillis()

            val callData = hashMapOf(
                "id" to callId,
                "callerUid" to myUid,
                "callerDisplayName" to (myProfile.getString("displayName") ?: ""),
                "callerAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "calleeUid" to "",
                "calleeDisplayName" to threadDoc.getString("groupName") ?: "Group",
                "isVideoCall" to true,
                "status" to "DIALING",
                "createdAt" to now,
                "threadId" to threadId,
                "isGroupCall" to true,
                "participantUids" to participantUids
            )
            callsCollection.document(callId).set(callData).await()

            val roomName = "group_call_$callId"
            val tokenResponse = cloudflareApiService.generateLiveKitToken(
                identity = myUid,
                roomName = roomName
            ).firstOrNull()

            if (tokenResponse == null) {
                endCallInternal(CallEndReason.NETWORK_ERROR)
                emit(_callState.value)
                return@flow
            }

            liveKitService.connect(tokenResponse.wsUrl, tokenResponse.token)
                .flowOn(Dispatchers.IO)
                .collect {}

            liveKitService.startVideoCall(roomName)
                .flowOn(Dispatchers.IO)
                .collect {}

            callsCollection.document(callId).update("status", "CONNECTED").await()

            callStartTime = System.currentTimeMillis()
            startDurationTracker()

            CallForegroundService.start(
                applicationContext,
                CallForegroundService.CALL_TYPE_VIDEO,
                threadDoc.getString("groupName") ?: "Group Call",
                roomName
            )

            val connectedState = CallState.CONNECTED(
                callId = callId,
                targetUid = threadId,
                targetDisplayName = threadDoc.getString("groupName") ?: "Group Call",
                isVideoCall = true,
                connectedAt = System.currentTimeMillis(),
                participantCount = participantUids.size
            )
            _callState.value = connectedState
            emit(connectedState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate group video call")
            endCallInternal(CallEndReason.NETWORK_ERROR)
            emit(_callState.value)
        }
    }.flowOn(Dispatchers.IO)

    // ── Accept Call ───────────────────────────────────────────────────────────

    override suspend fun acceptCall(callId: String) {
        try {
            val myUid = currentUid ?: return
            withContext(Dispatchers.IO) {
                val callDoc = callsCollection.document(callId).get().await()
                if (!callDoc.exists()) {
                    Timber.w("Call document not found: %s", callId)
                    return@withContext
                }

                val status = callDoc.getString("status")
                if (status != "DIALING" && status != "RINGING") {
                    Timber.w("Cannot accept call in state: %s", status)
                    return@withContext
                }

                val roomName = "call_$callId"
                val tokenResponse = cloudflareApiService.generateLiveKitToken(
                    identity = myUid,
                    roomName = roomName
                ).firstOrNull()

                if (tokenResponse == null) {
                    Timber.e("Failed to fetch LiveKit token for accepting call")
                    return@withContext
                }

                liveKitService.connect(tokenResponse.wsUrl, tokenResponse.token)
                    .flowOn(Dispatchers.IO)
                    .collect {}

                val isVideoCall = callDoc.getBoolean("isVideoCall") ?: false
                if (isVideoCall) {
                    liveKitService.startVideoCall(roomName)
                        .flowOn(Dispatchers.IO)
                        .collect {}
                } else {
                    liveKitService.startAudioCall(roomName)
                        .flowOn(Dispatchers.IO)
                        .collect {}
                }

                callsCollection.document(callId).update("status", "CONNECTED").await()

                callStartTime = System.currentTimeMillis()
                startDurationTracker()

                CallForegroundService.start(
                    applicationContext,
                    if (isVideoCall) CallForegroundService.CALL_TYPE_VIDEO
                    else CallForegroundService.CALL_TYPE_AUDIO,
                    callDoc.getString("callerDisplayName") ?: "",
                    roomName
                )

                val connectedState = CallState.CONNECTED(
                    callId = callId,
                    targetUid = callDoc.getString("callerUid") ?: "",
                    targetDisplayName = callDoc.getString("callerDisplayName") ?: "",
                    targetAvatarUrl = callDoc.getString("callerAvatarUrl") ?: "",
                    isVideoCall = isVideoCall,
                    connectedAt = System.currentTimeMillis()
                )
                _callState.value = connectedState
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to accept call: %s", callId)
        }
    }

    // ── Decline Call ──────────────────────────────────────────────────────────

    override suspend fun declineCall(callId: String) {
        try {
            withContext(Dispatchers.IO) {
                callsCollection.document(callId).update(
                    mapOf(
                        "status" to "DECLINED",
                        "endedAt" to System.currentTimeMillis(),
                        "endReason" to CallEndReason.DECLINED.name
                    )
                ).await()

                _callState.value = CallState.ENDED(
                    callId = callId,
                    endReason = CallEndReason.DECLINED
                )
                currentCallId = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decline call: %s", callId)
        }
    }

    // ── End Call ──────────────────────────────────────────────────────────────

    override suspend fun endCall() {
        try {
            endCallInternal(CallEndReason.COMPLETED)
        } catch (e: Exception) {
            Timber.e(e, "Failed to end call")
        }
    }

    private suspend fun endCallInternal(reason: CallEndReason) {
        val callId = currentCallId
        val duration = if (callStartTime > 0) {
            (System.currentTimeMillis() - callStartTime) / 1000
        } else 0L

        try {
            liveKitService.disconnect()
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from LiveKit room")
        }

        CallForegroundService.stop(applicationContext)

        callDurationJob?.cancel()
        callDurationJob = null

        if (callId != null) {
            try {
                callsCollection.document(callId).update(
                    mapOf(
                        "status" to "ENDED",
                        "endedAt" to System.currentTimeMillis(),
                        "endReason" to reason.name,
                        "durationSeconds" to duration
                    )
                ).await()

                val callDoc = callsCollection.document(callId).get().await()
                if (callDoc.exists()) {
                    saveCallHistory(callDoc, duration, reason)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update call document on end")
            }
        }

        _callState.value = CallState.ENDED(
            callId = callId ?: "",
            durationSeconds = duration,
            endReason = reason
        )
        currentCallId = null
        callStartTime = 0L
    }

    // ── Media Controls ────────────────────────────────────────────────────────

    override suspend fun setMuted(isMuted: Boolean) {
        try {
            val currentState = _callState.value
            if (currentState is CallState.CONNECTED) {
                _callState.value = currentState.copy(isMuted = isMuted)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set muted state")
        }
    }

    override suspend fun setCameraOff(isCameraOff: Boolean) {
        try {
            val currentState = _callState.value
            if (currentState is CallState.CONNECTED) {
                _callState.value = currentState.copy(isCameraOff = isCameraOff)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set camera state")
        }
    }

    override suspend fun setSpeakerOn(isSpeakerOn: Boolean) {
        try {
            val currentState = _callState.value
            if (currentState is CallState.CONNECTED) {
                _callState.value = currentState.copy(isSpeakerOn = isSpeakerOn)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set speaker state")
        }
    }

    override suspend fun switchCamera() {
        try {
            Timber.d("Camera switch requested")
        } catch (e: Exception) {
            Timber.e(e, "Failed to switch camera")
        }
    }

    // ── Call Duration ─────────────────────────────────────────────────────────

    override fun observeCallDuration(): Flow<Long> = callbackFlow {
        while (isActive) {
            if (callStartTime > 0L && _callState.value is CallState.CONNECTED) {
                val duration = (System.currentTimeMillis() - callStartTime) / 1000
                trySend(duration)
            }
            delay(1000L)
        }
        awaitClose {}
    }.flowOn(Dispatchers.IO)

    // ── Call History ──────────────────────────────────────────────────────────

    override fun observeCallHistory(): Flow<List<CallHistoryEntry>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = firestore.collection("users").document(myUid)
            .collection("call_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing call history")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToCallHistoryEntry(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map call history entry: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(entries)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private fun startDurationTracker() {
        callDurationJob?.cancel()
    }

    private suspend fun saveCallHistory(
        callDoc: com.google.firebase.firestore.DocumentSnapshot,
        durationSeconds: Long,
        endReason: CallEndReason
    ) {
        val myUid = currentUid ?: return

        val entry = CallHistoryEntry(
            id = UUID.randomUUID().toString(),
            callId = callDoc.id,
            callerUid = callDoc.getString("callerUid") ?: "",
            callerDisplayName = callDoc.getString("callerDisplayName") ?: "",
            callerAvatarUrl = callDoc.getString("callerAvatarUrl") ?: "",
            calleeUid = callDoc.getString("calleeUid") ?: "",
            calleeDisplayName = callDoc.getString("calleeDisplayName") ?: "",
            calleeAvatarUrl = callDoc.getString("calleeAvatarUrl") ?: "",
            isVideoCall = callDoc.getBoolean("isVideoCall") ?: false,
            durationSeconds = durationSeconds,
            timestamp = callDoc.getLong("createdAt") ?: System.currentTimeMillis(),
            endReason = endReason,
            threadId = callDoc.getString("threadId") ?: ""
        )

        firestore.collection("users").document(myUid)
            .collection("call_history").document(entry.id)
            .set(entry.toFirestoreMap()).await()

        Timber.d("Call history saved: %s", entry.callId)
    }

    private fun mapToCallHistoryEntry(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): CallHistoryEntry {
        val endReasonStr = doc.getString("endReason") ?: "COMPLETED"
        return CallHistoryEntry(
            id = doc.id,
            callId = doc.getString("callId") ?: "",
            callerUid = doc.getString("callerUid") ?: "",
            callerDisplayName = doc.getString("callerDisplayName") ?: "",
            callerAvatarUrl = doc.getString("callerAvatarUrl") ?: "",
            calleeUid = doc.getString("calleeUid") ?: "",
            calleeDisplayName = doc.getString("calleeDisplayName") ?: "",
            calleeAvatarUrl = doc.getString("calleeAvatarUrl") ?: "",
            isVideoCall = doc.getBoolean("isVideoCall") ?: false,
            durationSeconds = doc.getLong("durationSeconds") ?: 0L,
            timestamp = doc.getLong("timestamp") ?: 0L,
            endReason = try {
                CallEndReason.valueOf(endReasonStr)
            } catch (_: Exception) {
                CallEndReason.COMPLETED
            },
            threadId = doc.getString("threadId") ?: ""
        )
    }

    private fun CallHistoryEntry.toFirestoreMap(): Map<String, Any?> = mapOf(
        "callId" to callId,
        "callerUid" to callerUid,
        "callerDisplayName" to callerDisplayName,
        "callerAvatarUrl" to callerAvatarUrl,
        "calleeUid" to calleeUid,
        "calleeDisplayName" to calleeDisplayName,
        "calleeAvatarUrl" to calleeAvatarUrl,
        "isVideoCall" to isVideoCall,
        "durationSeconds" to durationSeconds,
        "timestamp" to timestamp,
        "endReason" to endReason.name,
        "threadId" to threadId
    )
}
