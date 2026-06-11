package com.zexo.app.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.data.model.CallRecord
import com.zexo.app.data.model.CallSignal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val rtdb: FirebaseDatabase
) {
    companion object { private const val TAG = "CallRepository" }

    fun observeIncomingCalls(uid: String): Flow<List<Pair<String, CallSignal>>> = callbackFlow {
        val callsRef = rtdb.getReference("calls")
        val listener = callsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val calls = mutableListOf<Pair<String, CallSignal>>()
                for (child in snapshot.children) {
                    try {
                        val signal = CallSignal(
                            callerId = child.child("callerId").getValue(String::class.java) ?: "",
                            callerName = child.child("callerName").getValue(String::class.java) ?: "",
                            receiverId = child.child("receiverId").getValue(String::class.java) ?: "",
                            type = child.child("type").getValue(String::class.java) ?: "audio",
                            status = child.child("status").getValue(String::class.java) ?: "ringing",
                            offer = child.child("offer").getValue(String::class.java) ?: "",
                            answer = child.child("answer").getValue(String::class.java) ?: "",
                            createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                        )
                        if (signal.receiverId == uid && signal.status == "ringing") {
                            calls.add(Pair(child.key ?: "", signal))
                        }
                    } catch (_: Exception) {}
                }
                trySend(calls)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
        awaitClose { callsRef.removeEventListener(listener) }
    }

    suspend fun createCallSignal(signal: CallSignal): Result<String> {
        return try {
            val callId = "call_${System.currentTimeMillis()}_${(100000..999999).random()}"
            rtdb.getReference("calls").child(callId).setValue(mapOf(
                "callerId" to signal.callerId,
                "callerName" to signal.callerName,
                "receiverId" to signal.receiverId,
                "type" to signal.type,
                "status" to "ringing",
                "createdAt" to com.google.firebase.database.ServerValue.TIMESTAMP,
                "callerCandidates" to emptyList<String>(),
                "receiverCandidates" to emptyList<String>()
            )).await()
            Result.success(callId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endCallSignal(callId: String) {
        try {
            rtdb.getReference("calls").child(callId).removeValue()
        } catch (e: Exception) {
            Log.e(TAG, "End call signal failed", e)
        }
    }

    suspend fun saveCallRecord(record: CallRecord): Result<String> {
        return try {
            val doc = firestore.collection("calls").document()
            val data = mapOf(
                "callerId" to record.callerId,
                "callerName" to record.callerName,
                "callerAvatar" to record.callerAvatar,
                "receiverId" to record.receiverId,
                "receiverName" to record.receiverName,
                "receiverAvatar" to record.receiverAvatar,
                "type" to record.type,
                "direction" to record.direction,
                "duration" to record.duration,
                "timestamp" to FieldValue.serverTimestamp()
            )
            doc.set(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCallHistory(uid: String): Result<List<CallRecord>> {
        return try {
            val calls = mutableListOf<CallRecord>()
            
            val q1 = firestore.collection("calls")
                .whereEqualTo("callerId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50).get().await()
            
            val q2 = firestore.collection("calls")
                .whereEqualTo("receiverId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50).get().await()
            
            for (doc in q1.documents + q2.documents) {
                try {
                    calls.add(CallRecord(
                        id = doc.id,
                        callerId = doc.getString("callerId") ?: "",
                        callerName = doc.getString("callerName") ?: "",
                        callerAvatar = doc.getString("callerAvatar") ?: "",
                        receiverId = doc.getString("receiverId") ?: "",
                        receiverName = doc.getString("receiverName") ?: "",
                        receiverAvatar = doc.getString("receiverAvatar") ?: "",
                        type = doc.getString("type") ?: "audio",
                        direction = doc.getString("direction") ?: "outgoing",
                        duration = doc.getLong("duration") ?: 0L,
                        timestamp = doc.getLong("timestamp")
                    ))
                } catch (_: Exception) {}
            }
            
            Result.success(calls.sortedByDescending { it.timestamp ?: 0L })
        } catch (e: Exception) {
            Log.e(TAG, "Get call history failed", e)
            Result.failure(e)
        }
    }
}
