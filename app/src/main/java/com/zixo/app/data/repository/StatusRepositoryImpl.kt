package com.zixo.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.StatusUpdate
import com.zixo.app.domain.model.StatusType
import com.zixo.app.domain.model.UserStatus
import com.zixo.app.domain.repository.StatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StatusRepositoryImpl : StatusRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _userStatuses = MutableStateFlow<List<UserStatus>>(emptyList())
    override val userStatuses: StateFlow<List<UserStatus>> = _userStatuses.asStateFlow()

    private val _myStatuses = MutableStateFlow<List<StatusUpdate>>(emptyList())
    override val myStatuses: StateFlow<List<StatusUpdate>> = _myStatuses.asStateFlow()

    private val TAG = "StatusRepo"
    private var statusesListener: com.google.firebase.firestore.ListenerRegistration? = null

    override suspend fun createTextStatus(text: String, backgroundColor: String): Result<StatusUpdate> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val statusId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val status = StatusUpdate(
                id = statusId,
                userId = uid,
                type = StatusType.TEXT,
                text = text,
                backgroundColor = backgroundColor,
                timestamp = now,
                expiresAt = now + 24 * 60 * 60 * 1000 // 24 hours
            )
            firestore.collection("statuses").document(statusId)
                .set(status.toMap())
                .await()
            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create text status", e)
            Result.failure(e)
        }
    }

    override suspend fun createImageStatus(imageBytes: ByteArray, caption: String): Result<StatusUpdate> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val statusId = UUID.randomUUID().toString()
            val ref = storage.reference.child("statuses/$uid/$statusId.jpg")
            ref.putBytes(imageBytes).await()
            val imageUrl = ref.downloadUrl.await().toString()
            val now = System.currentTimeMillis()
            val status = StatusUpdate(
                id = statusId,
                userId = uid,
                type = StatusType.IMAGE,
                imageUrl = imageUrl,
                caption = caption,
                timestamp = now,
                expiresAt = now + 24 * 60 * 60 * 1000
            )
            firestore.collection("statuses").document(statusId)
                .set(status.toMap())
                .await()
            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create image status", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteStatus(statusId: String): Result<Unit> {
        return try {
            firestore.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete status", e)
            Result.failure(e)
        }
    }

    override suspend fun markStatusViewed(statusId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("statuses").document(statusId)
                .update("viewedBy", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark status viewed", e)
            Result.failure(e)
        }
    }

    override suspend fun addStatusReaction(statusId: String, emoji: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("statuses").document(statusId)
                .update("reactions.$uid", emoji)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add status reaction", e)
            Result.failure(e)
        }
    }

    override fun startListening() {
        try {
            val now = System.currentTimeMillis()
            // Simple query on single field — no composite index needed
            statusesListener = firestore.collection("statuses")
                .whereGreaterThan("expiresAt", now)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Status listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    try {
                        val allStatuses = snapshot.documents.mapNotNull { doc ->
                            try { documentToStatusUpdate(doc) } catch (_: Exception) { null }
                        }

                        val uid = auth.currentUser?.uid ?: ""
                        _myStatuses.value = allStatuses.filter { it.userId == uid }

                        // Group by user
                        val grouped = allStatuses
                            .groupBy { it.userId }
                            .map { (userId, statuses) ->
                                val first = statuses.first()
                                UserStatus(
                                    userId = userId,
                                    userName = first.userName,
                                    userAvatar = first.userAvatar,
                                    statuses = statuses.sortedByDescending { it.timestamp },
                                    hasUnviewed = statuses.any { s -> !s.viewedBy.contains(uid) },
                                    latestTimestamp = statuses.maxOf { it.timestamp }
                                )
                            }
                            .sortedByDescending { it.latestTimestamp }

                        _userStatuses.value = grouped
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process status snapshot", e)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start status listener", e)
        }
    }

    override fun stopListening() {
        statusesListener?.remove()
        statusesListener = null
    }

    private fun documentToStatusUpdate(doc: com.google.firebase.firestore.DocumentSnapshot): StatusUpdate {
        return StatusUpdate(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            userName = doc.getString("userName") ?: "",
            userAvatar = doc.getString("userAvatar") ?: "",
            type = try { StatusType.valueOf(doc.getString("type") ?: "TEXT") } catch (_: Exception) { StatusType.TEXT },
            text = doc.getString("text") ?: "",
            backgroundColor = doc.getString("backgroundColor") ?: "#1A2A32",
            imageUrl = doc.getString("imageUrl") ?: "",
            caption = doc.getString("caption") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L,
            expiresAt = doc.getLong("expiresAt") ?: 0L,
            viewedBy = (doc.get("viewedBy") as? List<String>) ?: emptyList(),
            reactions = (doc.get("reactions") as? Map<String, String>) ?: emptyMap()
        )
    }

    private fun StatusUpdate.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userAvatar" to userAvatar,
        "type" to type.name,
        "text" to text,
        "backgroundColor" to backgroundColor,
        "imageUrl" to imageUrl,
        "caption" to caption,
        "timestamp" to timestamp,
        "expiresAt" to expiresAt,
        "viewedBy" to viewedBy,
        "reactions" to reactions
    )
}
