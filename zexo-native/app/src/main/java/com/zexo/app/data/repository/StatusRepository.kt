package com.zexo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.data.model.Status
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object { private const val TAG = "StatusRepository" }

    fun observeStatuses(uids: List<String>): Flow<List<Status>> = callbackFlow {
        if (uids.isEmpty()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        
        // Get statuses from last 24 hours
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        var subscription: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            val query = firestore.collection("statuses")
                .whereGreaterThanOrEqualTo("createdAt", cutoff)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            
            subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Status listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val statuses = snapshot.documents.mapNotNull { doc ->
                        try {
                            Status(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                userAvatar = doc.getString("userAvatar") ?: "",
                                type = doc.getString("type") ?: "text",
                                content = doc.getString("content") ?: "",
                                mediaUrl = doc.getString("mediaUrl") ?: "",
                                backgroundColor = doc.getString("backgroundColor") ?: "#6C5CE7",
                                textColor = doc.getString("textColor") ?: "#FFFFFF",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                expiresAt = doc.getLong("expiresAt") ?: 0L,
                                seenBy = (doc.get("seenBy") as? List<String>) ?: emptyList()
                            )
                        } catch (_: Exception) { null }
                    }.filter { it.userId in uids && it.expiresAt > System.currentTimeMillis() }
                    trySend(statuses)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe statuses", e)
        }
        
        awaitClose { subscription?.remove() }
    }

    suspend fun createStatus(status: Status): Result<String> {
        return try {
            val doc = firestore.collection("statuses").document()
            val data = mapOf(
                "userId" to status.userId,
                "userName" to status.userName,
                "userAvatar" to status.userAvatar,
                "type" to status.type,
                "content" to status.content,
                "mediaUrl" to status.mediaUrl,
                "backgroundColor" to status.backgroundColor,
                "textColor" to status.textColor,
                "createdAt" to System.currentTimeMillis(),
                "expiresAt" to System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                "seenBy" to emptyList<String>()
            )
            doc.set(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markStatusSeen(statusId: String, uid: String): Result<Unit> {
        return try {
            firestore.collection("statuses").document(statusId)
                .update("seenBy", FieldValue.arrayUnion(uid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
