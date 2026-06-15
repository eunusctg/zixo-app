package com.zixo.app.data.remote.webrtc

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Realtime DB Signaling Client for WebRTC — Persistent Mesh Architecture.
 *
 * Handles all signaling operations for WebRTC calls via Firebase Realtime Database:
 * - Send SDP Offer/Answer to /calls/{callId}/signaling/
 * - Send/receive ICE candidates via /calls/{callId}/candidates/
 * - Persistent continuous listeners for incoming signaling data
 * - Call state tracking (IDLE, DIALING, RINGING, CONNECTED, ENDED)
 * - Online presence tracking via /presence/{uid}/
 * - Mesh signaling support for group calls
 *
 * ## Persistent Listener Architecture:
 * All listeners use [ValueEventListener] with continuous observation (NOT one-time reads).
 * The Firebase Realtime Database maintains persistent WebSocket connections that
 * instantly propagate signaling data across all connected devices.
 *
 * ## Mesh Signaling for Group Calls:
 * In a group call, each participant establishes a direct PeerConnection with every
 * other participant. The signaling path is organized as:
 * ```
 * /calls/{callId}/
 *   ├── status/              → "dialing" | "ringing" | "connected" | "ended"
 *   ├── participants/        → { uid: { joinedAt, isAudioEnabled, isVideoEnabled } }
 *   ├── signaling/
 *   │   ├── {senderUid}/
 *   │   │   ├── offer/       → { sdp, targetUid, timestamp }
 *   │   │   └── answer/      → { sdp, targetUid, timestamp }
 *   ├── candidates/
 *   │   ├── {senderUid}/
 *   │   │   ├── {pushId}/    → { sdpMid, sdpMLineIndex, sdp, targetUid, timestamp }
 * ```
 * This structure allows N×(N-1) mesh connections where each participant sends
 * individual offers to every other participant.
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
 */
@Singleton
class FirebaseSignalingClient @Inject constructor(
    private val realtimeDb: FirebaseDatabase
) {

    // ════════════════════════════════════════════════════════════════
    // Call Lifecycle Management
    // ════════════════════════════════════════════════════════════════

    /**
     * Creates a new call entry in Firebase Realtime DB.
     * Sets the initial status to "dialing" and registers the caller as a participant.
     *
     * @param callId The unique call ID.
     * @param callerUid The UID of the call initiator.
     * @param targetUid The UID of the call recipient.
     * @param isVideoCall Whether this is a video call.
     */
    suspend fun createCall(
        callId: String,
        callerUid: String,
        targetUid: String,
        isVideoCall: Boolean
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val callData = mapOf(
                "status" to "dialing",
                "callerUid" to callerUid,
                "targetUid" to targetUid,
                "isVideoCall" to isVideoCall,
                "createdAt" to System.currentTimeMillis(),
                "participants" to mapOf(
                    callerUid to mapOf(
                        "joinedAt" to System.currentTimeMillis(),
                        "isAudioEnabled" to true,
                        "isVideoEnabled" to isVideoCall
                    )
                )
            )

            realtimeDb.getReference("calls")
                .child(callId)
                .setValue(callData)
                .await()

            Timber.d("Call created: %s (video=%s)", callId, isVideoCall)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create call: %s", callId)
        }
    }

    /**
     * Updates the call status in Firebase Realtime DB.
     *
     * @param callId The call ID.
     * @param status The new status ("dialing", "ringing", "connected", "ended", "declined").
     */
    suspend fun updateCallStatus(callId: String, status: String) =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("status")
                    .setValue(status)
                    .await()

                Timber.d("Call status updated: %s → %s", callId, status)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update call status: %s", callId)
            }
        }

    /**
     * Adds a participant to an existing group call.
     *
     * @param callId The call ID.
     * @param uid The UID of the joining participant.
     * @param isVideoEnabled Whether the participant has video enabled.
     */
    suspend fun addParticipant(callId: String, uid: String, isVideoEnabled: Boolean = false) =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val participantData = mapOf(
                    "joinedAt" to System.currentTimeMillis(),
                    "isAudioEnabled" to true,
                    "isVideoEnabled" to isVideoEnabled
                )

                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("participants")
                    .child(uid)
                    .setValue(participantData)
                    .await()

                Timber.d("Participant added to call: %s → %s", uid, callId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add participant: %s", uid)
            }
        }

    // ════════════════════════════════════════════════════════════════
    // SDP Signaling (1-to-1 and Mesh)
    // ════════════════════════════════════════════════════════════════

    /**
     * Sends an SDP offer to the Firebase Realtime DB signaling path.
     *
     * For 1-to-1 calls: writes to /calls/{callId}/signaling/offer
     * For mesh group calls: writes to /calls/{callId}/signaling/{senderUid}/offer
     *
     * @param callId The call ID.
     * @param senderUid The UID of the caller sending the offer.
     * @param targetUid The UID of the intended recipient (for mesh routing).
     * @param sdpOffer The SDP offer string.
     */
    suspend fun sendOffer(
        callId: String,
        senderUid: String,
        targetUid: String = "",
        sdpOffer: String?
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            if (sdpOffer == null) return@withContext

            val offerData = mapOf(
                "type" to "offer",
                "sdp" to sdpOffer,
                "senderUid" to senderUid,
                "targetUid" to targetUid,
                "timestamp" to System.currentTimeMillis()
            )

            // For 1-to-1 calls, write to the simple offer path
            // For mesh calls, write to the sender-specific path
            if (targetUid.isBlank()) {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("signaling")
                    .child("offer")
                    .setValue(offerData)
                    .await()
            } else {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("signaling")
                    .child(senderUid)
                    .child("offer")
                    .setValue(offerData)
                    .await()
            }

            Timber.d("SDP offer sent for call: %s", callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send SDP offer for call: %s", callId)
        }
    }

    /**
     * Sends an SDP answer to the Firebase Realtime DB signaling path.
     *
     * @param callId The call ID.
     * @param senderUid The UID of the callee sending the answer.
     * @param targetUid The UID of the intended recipient (for mesh routing).
     * @param sdpAnswer The SDP answer string.
     */
    suspend fun sendAnswer(
        callId: String,
        senderUid: String,
        targetUid: String = "",
        sdpAnswer: String?
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            if (sdpAnswer == null) return@withContext

            val answerData = mapOf(
                "type" to "answer",
                "sdp" to sdpAnswer,
                "senderUid" to senderUid,
                "targetUid" to targetUid,
                "timestamp" to System.currentTimeMillis()
            )

            if (targetUid.isBlank()) {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("signaling")
                    .child("answer")
                    .setValue(answerData)
                    .await()
            } else {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("signaling")
                    .child(senderUid)
                    .child("answer")
                    .setValue(answerData)
                    .await()
            }

            Timber.d("SDP answer sent for call: %s", callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send SDP answer for call: %s", callId)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ICE Candidate Exchange
    // ════════════════════════════════════════════════════════════════

    /**
     * Sends an ICE candidate to the Firebase Realtime DB.
     *
     * @param callId The call ID.
     * @param senderUid The UID of the sender.
     * @param targetUid The UID of the intended recipient (for mesh routing).
     * @param sdpMid The media stream identification tag.
     * @param sdpMLineIndex The index of the m-line.
     * @param sdp The SDP candidate string.
     */
    suspend fun sendIceCandidate(
        callId: String,
        senderUid: String,
        targetUid: String = "",
        sdpMid: String,
        sdpMLineIndex: Int,
        sdp: String
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val candidateData = mapOf(
                "sdpMid" to sdpMid,
                "sdpMLineIndex" to sdpMLineIndex,
                "sdp" to sdp,
                "senderUid" to senderUid,
                "targetUid" to targetUid,
                "timestamp" to System.currentTimeMillis()
            )

            // For mesh routing, store under sender's candidates path
            val ref = if (targetUid.isBlank()) {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("candidates")
                    .push()
            } else {
                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("candidates")
                    .child(senderUid)
                    .push()
            }

            ref.setValue(candidateData).await()

            Timber.d("ICE candidate sent for call: %s", callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send ICE candidate for call: %s", callId)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Persistent Observation (Continuous Listeners)
    // ════════════════════════════════════════════════════════════════

    /**
     * Observes the SDP offer for a call via persistent Firebase listener.
     *
     * @param callId The call ID.
     * @param localUid The local user's UID (for mesh filtering).
     * @return A flow emitting SDP offer data.
     */
    fun observeOffer(callId: String, localUid: String = ""): Flow<SdpData?> = callbackFlow {
        val ref = realtimeDb.getReference("calls")
            .child(callId)
            .child("signaling")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Check simple offer path first (1-to-1)
                    val simpleOffer = snapshot.child("offer")
                    if (simpleOffer.exists()) {
                        val sdp = simpleOffer.child("sdp").getValue(String::class.java)
                        val senderUid = simpleOffer.child("senderUid").getValue(String::class.java) ?: ""
                        if (sdp != null && senderUid != localUid) {
                            trySend(SdpData(sdp = sdp, senderUid = senderUid, type = "offer"))
                            return
                        }
                    }

                    // Check mesh paths (signaling/{uid}/offer)
                    for (child in snapshot.children) {
                        val offerChild = child.child("offer")
                        if (offerChild.exists()) {
                            val sdp = offerChild.child("sdp").getValue(String::class.java)
                            val senderUid = offerChild.child("senderUid").getValue(String::class.java) ?: ""
                            val targetUid = offerChild.child("targetUid").getValue(String::class.java) ?: ""
                            if (sdp != null && senderUid != localUid &&
                                (targetUid.isBlank() || targetUid == localUid)
                            ) {
                                trySend(SdpData(sdp = sdp, senderUid = senderUid, type = "offer"))
                                return
                            }
                        }
                    }

                    trySend(null)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing offer for call: %s", callId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Offer listener cancelled for call: %s", callId)
            }
        })

        awaitClose { ref.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Observes the SDP answer for a call via persistent Firebase listener.
     *
     * @param callId The call ID.
     * @param localUid The local user's UID (for mesh filtering).
     * @return A flow emitting SDP answer data.
     */
    fun observeAnswer(callId: String, localUid: String = ""): Flow<SdpData?> = callbackFlow {
        val ref = realtimeDb.getReference("calls")
            .child(callId)
            .child("signaling")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Check simple answer path first (1-to-1)
                    val simpleAnswer = snapshot.child("answer")
                    if (simpleAnswer.exists()) {
                        val sdp = simpleAnswer.child("sdp").getValue(String::class.java)
                        val senderUid = simpleAnswer.child("senderUid").getValue(String::class.java) ?: ""
                        if (sdp != null && senderUid != localUid) {
                            trySend(SdpData(sdp = sdp, senderUid = senderUid, type = "answer"))
                            return
                        }
                    }

                    // Check mesh paths
                    for (child in snapshot.children) {
                        val answerChild = child.child("answer")
                        if (answerChild.exists()) {
                            val sdp = answerChild.child("sdp").getValue(String::class.java)
                            val senderUid = answerChild.child("senderUid").getValue(String::class.java) ?: ""
                            val targetUid = answerChild.child("targetUid").getValue(String::class.java) ?: ""
                            if (sdp != null && senderUid != localUid &&
                                (targetUid.isBlank() || targetUid == localUid)
                            ) {
                                trySend(SdpData(sdp = sdp, senderUid = senderUid, type = "answer"))
                                return
                            }
                        }
                    }

                    trySend(null)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing answer for call: %s", callId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Answer listener cancelled for call: %s", callId)
            }
        })

        awaitClose { ref.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    /**
     * Observes incoming ICE candidates for a call via persistent Firebase listener.
     *
     * @param callId The call ID.
     * @param localUid The local user's UID (to filter out self-sent candidates).
     * @return A flow emitting lists of ICE candidate data.
     */
    fun observeIceCandidates(callId: String, localUid: String): Flow<List<IceCandidateData>> =
        callbackFlow {
            val ref = realtimeDb.getReference("calls")
                .child(callId)
                .child("candidates")

            val listener = ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val candidates = mutableListOf<IceCandidateData>()
                        for (child in snapshot.children) {
                            // Check if this is a sender-specific path (mesh)
                            val hasSubChildren = child.hasChildren() && child.child("sdpMid").exists() == false
                            if (hasSubChildren) {
                                // Mesh path: candidates/{senderUid}/{pushId}
                                for (subChild in child.children) {
                                    val senderUid = subChild.child("senderUid").getValue(String::class.java)
                                    val targetUid = subChild.child("targetUid").getValue(String::class.java) ?: ""
                                    if (senderUid == localUid) continue
                                    if (targetUid.isNotBlank() && targetUid != localUid) continue

                                    val sdpMid = subChild.child("sdpMid").getValue(String::class.java) ?: ""
                                    val sdpMLineIndex = subChild.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                                    val sdp = subChild.child("sdp").getValue(String::class.java) ?: ""
                                    candidates.add(IceCandidateData(sdpMid, sdpMLineIndex, sdp))
                                }
                            } else {
                                // Simple path: candidates/{pushId}
                                val senderUid = child.child("senderUid").getValue(String::class.java)
                                val targetUid = child.child("targetUid").getValue(String::class.java) ?: ""
                                if (senderUid == localUid) continue
                                if (targetUid.isNotBlank() && targetUid != localUid) continue

                                val sdpMid = child.child("sdpMid").getValue(String::class.java) ?: ""
                                val sdpMLineIndex = child.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                                val sdp = child.child("sdp").getValue(String::class.java) ?: ""
                                candidates.add(IceCandidateData(sdpMid, sdpMLineIndex, sdp))
                            }
                        }
                        trySend(candidates)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing ICE candidates for call: %s", callId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e(error.toException(), "ICE candidate listener cancelled for call: %s", callId)
                }
            })

            awaitClose { ref.removeEventListener(listener) }
        }.flowOn(Dispatchers.IO)

    /**
     * Observes the call status via persistent Firebase listener.
     *
     * @param callId The call ID.
     * @return A flow emitting the call status string.
     */
    fun observeCallStatus(callId: String): Flow<String> = callbackFlow {
        val ref = realtimeDb.getReference("calls")
            .child(callId)
            .child("status")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: "unknown"
                trySend(status)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Call status listener cancelled for call: %s", callId)
            }
        })

        awaitClose { ref.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // ════════════════════════════════════════════════════════════════
    // Incoming Call Detection
    // ════════════════════════════════════════════════════════════════

    /**
     * Observes incoming calls for the local user via persistent Firebase listener.
     *
     * Listens to /calls/ where targetUid matches the local user's UID and
     * the status is "dialing" or "ringing".
     *
     * @param localUid The local user's UID.
     * @return A flow emitting lists of incoming call data.
     */
    fun observeIncomingCalls(localUid: String): Flow<List<IncomingCallData>> = callbackFlow {
        val ref = realtimeDb.getReference("calls")
            .orderByChild("targetUid")
            .equalTo(localUid)

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val calls = mutableListOf<IncomingCallData>()
                    for (child in snapshot.children) {
                        val status = child.child("status").getValue(String::class.java) ?: ""
                        if (status == "dialing" || status == "ringing") {
                            val callId = child.key ?: continue
                            val callerUid = child.child("callerUid").getValue(String::class.java) ?: ""
                            val isVideoCall = child.child("isVideoCall").getValue(Boolean::class.java) ?: false
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L

                            calls.add(IncomingCallData(
                                callId = callId,
                                callerUid = callerUid,
                                targetUid = localUid,
                                isVideoCall = isVideoCall,
                                status = status,
                                createdAt = createdAt
                            ))
                        }
                    }
                    trySend(calls)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing incoming calls")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Incoming calls listener cancelled")
            }
        })

        awaitClose { ref.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // ════════════════════════════════════════════════════════════════
    // Online Presence Tracking
    // ════════════════════════════════════════════════════════════════

    /**
     * Sets the user's online presence in Firebase Realtime DB.
     *
     * Uses the standard Firebase presence pattern:
     * - Sets /presence/{uid}/online = true with lastSeen timestamp
     * - Registers an onDisconnect handler that sets online = false when the
     *   client disconnects (even if the app crashes)
     *
     * @param uid The user's UID.
     */
    suspend fun setOnlinePresence(uid: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val presenceRef = realtimeDb.getReference("presence").child(uid)
            val now = System.currentTimeMillis()

            presenceRef.child("online").setValue(true).await()
            presenceRef.child("lastSeen").setValue(now).await()

            // On disconnect, mark as offline
            presenceRef.child("online").onDisconnect().setValue(false)
            presenceRef.child("lastSeen").onDisconnect().setValue(now)

            Timber.d("Online presence set for: %s", uid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set online presence for: %s", uid)
        }
    }

    /**
     * Observes the online status of a specific user via persistent Firebase listener.
     *
     * @param uid The user's UID to observe.
     * @return A flow emitting [PresenceData] with online status and lastSeen.
     */
    fun observePresence(uid: String): Flow<PresenceData> = callbackFlow {
        val ref = realtimeDb.getReference("presence").child(uid)

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
                    val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                    trySend(PresenceData(isOnline = isOnline, lastSeen = lastSeen))
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing presence for: %s", uid)
                    trySend(PresenceData(isOnline = false, lastSeen = 0L))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Presence listener cancelled for: %s", uid)
                trySend(PresenceData(isOnline = false, lastSeen = 0L))
            }
        })

        awaitClose { ref.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    // ════════════════════════════════════════════════════════════════
    // Clean Up Signaling Data
    // ════════════════════════════════════════════════════════════════

    /**
     * Removes all signaling data for a call from Firebase Realtime DB.
     * Should be called when a call ends to clean up stale data.
     *
     * @param callId The call ID.
     */
    suspend fun cleanupSignalingData(callId: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            realtimeDb.getReference("calls").child(callId).removeValue().await()
            Timber.d("Signaling data cleaned up for call: %s", callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clean up signaling data for call: %s", callId)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Data Classes
    // ════════════════════════════════════════════════════════════════

    /** Represents an ICE candidate received from the signaling server. */
    data class IceCandidateData(
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val sdp: String
    )

    /** Represents SDP signaling data (offer or answer). */
    data class SdpData(
        val sdp: String,
        val senderUid: String,
        val type: String // "offer" or "answer"
    )

    /** Represents an incoming call. */
    data class IncomingCallData(
        val callId: String,
        val callerUid: String,
        val targetUid: String,
        val isVideoCall: Boolean,
        val status: String,
        val createdAt: Long
    )

    /** Represents a user's online presence. */
    data class PresenceData(
        val isOnline: Boolean = false,
        val lastSeen: Long = 0L
    )
}
