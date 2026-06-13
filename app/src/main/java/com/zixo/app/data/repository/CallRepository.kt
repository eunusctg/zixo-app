package com.zixo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zixo.app.data.model.CallRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "CallRepository"
    }

    fun observeCallHistory(userId: String): Flow<List<CallRecord>> = callbackFlow {
        val registrations = mutableListOf<ListenerRegistration>()

        // Query only by callerId WITHOUT orderBy to avoid needing composite index.
        // We sort client-side instead.
        val callerReg = firestore.collection("calls")
            .whereEqualTo("callerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeCallHistory caller query failed: ${error.message}")
                    // Don't crash - just emit empty and continue
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val callerCalls = snapshot?.documents?.mapNotNull { doc -> docToCallRecord(doc) } ?: emptyList()

                // Merge with receiver calls that were already collected
                val currentReceiverCalls = _receiverCallsCache
                val merged = (callerCalls + currentReceiverCalls)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }
                trySend(merged)
            }
        registrations.add(callerReg)

        val receiverReg = firestore.collection("calls")
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeCallHistory receiver query failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val receiverCalls = snapshot?.documents?.mapNotNull { doc -> docToCallRecord(doc) } ?: emptyList()
                _receiverCallsCache = receiverCalls

                // Merge with caller calls
                val currentCallerCalls = _callerCallsCache
                val merged = (currentCallerCalls + receiverCalls)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }
                trySend(merged)
            }
        registrations.add(receiverReg)

        awaitClose { registrations.forEach { it.remove() } }
    }

    // Caches for merging caller/receiver results in real-time
    private var _callerCallsCache: List<CallRecord> = emptyList()
    private var _receiverCallsCache: List<CallRecord> = emptyList()

    suspend fun saveCallRecord(call: CallRecord): Result<String> {
        return try {
            val docRef = firestore.collection("calls").document()
            docRef.set(mapOf(
                "callerId" to call.callerId,
                "callerName" to call.callerName,
                "callerAvatar" to call.callerAvatar,
                "receiverId" to call.receiverId,
                "receiverName" to call.receiverName,
                "receiverAvatar" to call.receiverAvatar,
                "type" to call.type,
                "direction" to call.direction,
                "duration" to call.duration,
                "timestamp" to FieldValue.serverTimestamp()
            )).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to save call record"))
        }
    }

    private fun docToCallRecord(doc: com.google.firebase.firestore.DocumentSnapshot): CallRecord? {
        return try {
            CallRecord(
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
                timestamp = safeTimestamp(doc, "timestamp")
            )
        } catch (e: Exception) { null }
    }

    private fun safeTimestamp(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
        return try {
            doc.getLong(field) ?: 0L
        } catch (_: RuntimeException) {
            try {
                doc.getTimestamp(field)?.toDate()?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
