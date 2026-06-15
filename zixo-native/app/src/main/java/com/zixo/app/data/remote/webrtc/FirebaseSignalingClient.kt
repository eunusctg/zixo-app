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
 * Firebase Realtime DB Signaling Client for WebRTC.
 *
 * Handles all signaling operations for WebRTC calls via Firebase Realtime Database:
 * - Send SDP Offer/Answer to /calls/{callId}/signaling/
 * - Send/receive ICE candidates via /calls/{callId}/candidates/
 * - Continuous listeners for incoming signaling data
 * - Mesh signaling support for group calls
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
 * NO LiveKit references — pure WebRTC + Firebase Realtime DB.
 */
@Singleton
class FirebaseSignalingClient @Inject constructor(
    private val realtimeDb: FirebaseDatabase
) {

    // ── Send Offer ────────────────────────────────────────────────────────────

    /**
     * Sends an SDP offer to the Firebase Realtime DB signaling path.
     *
     * @param callId The call ID.
     * @param senderUid The UID of the caller sending the offer.
     * @param sdpOffer The SDP offer string.
     */
    suspend fun sendOffer(callId: String, senderUid: String, sdpOffer: String?) =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                if (sdpOffer == null) return@withContext

                val offerData = mapOf(
                    "type" to "offer",
                    "sdp" to sdpOffer,
                    "senderUid" to senderUid,
                    "timestamp" to System.currentTimeMillis()
                )

                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("signaling")
                    .child("offer")
                    .setValue(offerData)
                    .await()

                Timber.d("SDP offer sent for call: %s", callId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to send SDP offer for call: %s", callId)
            }
        }

    // ── Send Answer ───────────────────────────────────────────────────────────

    /**
     * Sends an SDP answer to the Firebase Realtime DB signaling path.
     *
     * @param callId The call ID.
     * @param senderUid The UID of the callee sending the answer.
     * @param sdpAnswer The SDP answer string.
     */
    suspend fun sendAnswer(callId: String, senderUid: String, sdpAnswer: String?) =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                if (sdpAnswer == null) return@withContext

                val answerData = mapOf(
                    "type" to "answer",
                    "sdp" to sdpAnswer,
                    "senderUid" to senderUid,
                    "timestamp" to System.currentTimeMillis()
                )

                realtimeDb.getReference("calls")
                    .child(callId)
                    .child("signaling")
                    .child("answer")
                    .setValue(answerData)
                    .await()

                Timber.d("SDP answer sent for call: %s", callId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to send SDP answer for call: %s", callId)
            }
        }

    // ── Send ICE Candidate ────────────────────────────────────────────────────

    /**
     * Sends an ICE candidate to the Firebase Realtime DB.
     *
     * @param callId The call ID.
     * @param senderUid The UID of the sender.
     * @param sdpMid The media stream identification tag.
     * @param sdpMLineIndex The index of the m-line.
     * @param sdp The SDP candidate string.
     */
    suspend fun sendIceCandidate(
        callId: String,
        senderUid: String,
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
                "timestamp" to System.currentTimeMillis()
            )

            realtimeDb.getReference("calls")
                .child(callId)
                .child("candidates")
                .push()
                .setValue(candidateData)
                .await()

            Timber.d("ICE candidate sent for call: %s", callId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send ICE candidate for call: %s", callId)
        }
    }

    // ── Observe Offer ─────────────────────────────────────────────────────────

    /**
     * Observes the SDP offer for a call via continuous Firebase listener.
     *
     * @param callId The call ID.
     * @return A flow emitting the SDP offer string, or null.
     */
    fun observeOffer(callId: String): Flow<String?> = callbackFlow {
        val ref = realtimeDb.getReference("calls")
            .child(callId)
            .child("signaling")
            .child("offer")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    if (sdp != null) {
                        trySend(sdp)
                    }
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

    // ── Observe Answer ────────────────────────────────────────────────────────

    /**
     * Observes the SDP answer for a call via continuous Firebase listener.
     *
     * @param callId The call ID.
     * @return A flow emitting the SDP answer string, or null.
     */
    fun observeAnswer(callId: String): Flow<String?> = callbackFlow {
        val ref = realtimeDb.getReference("calls")
            .child(callId)
            .child("signaling")
            .child("answer")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    if (sdp != null) {
                        trySend(sdp)
                    }
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

    // ── Observe ICE Candidates ────────────────────────────────────────────────

    /**
     * Observes incoming ICE candidates for a call via continuous Firebase listener.
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
                            val senderUid = child.child("senderUid").getValue(String::class.java)
                            // Skip candidates sent by the local user
                            if (senderUid == localUid) continue

                            val sdpMid = child.child("sdpMid").getValue(String::class.java) ?: ""
                            val sdpMLineIndex = child.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                            val sdp = child.child("sdp").getValue(String::class.java) ?: ""

                            candidates.add(IceCandidateData(sdpMid, sdpMLineIndex, sdp))
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

    // ── Observe Call Status ───────────────────────────────────────────────────

    /**
     * Observes the call status (ringing, connected, ended, declined).
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

    // ── Clean Up Signaling Data ───────────────────────────────────────────────

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

    // ── Data Classes ──────────────────────────────────────────────────────────

    /**
     * Represents an ICE candidate received from the signaling server.
     */
    data class IceCandidateData(
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val sdp: String
    )
}
