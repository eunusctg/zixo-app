package com.zexo.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zexo.app.data.model.CallRecord
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
    fun observeCallHistory(userId: String): Flow<List<CallRecord>> = callbackFlow {
        val registrations = mutableListOf<ListenerRegistration>()

        val callerReg = firestore.collection("calls")
            .whereEqualTo("callerId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val calls = snapshot?.documents?.mapNotNull { doc -> docToCallRecord(doc) } ?: emptyList()
                trySend(calls)
            }
        registrations.add(callerReg)

        val receiverReg = firestore.collection("calls")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val calls = snapshot?.documents?.mapNotNull { doc -> docToCallRecord(doc) } ?: emptyList()
                trySend(calls)
            }
        registrations.add(receiverReg)

        awaitClose { registrations.forEach { it.remove() } }
    }

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
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        } catch (e: Exception) { null }
    }
}
