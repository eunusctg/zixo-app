package com.zexo.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zexo.app.data.model.StatusUpdate
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
    fun observeStatuses(userZixoNumbers: List<String>): Flow<List<StatusUpdate>> = callbackFlow {
        val registrations = mutableListOf<ListenerRegistration>()

        if (userZixoNumbers.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val reg = firestore.collection("statuses")
            .whereIn("creatorZixoNumber", userZixoNumbers.take(10))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val statuses = snapshot?.documents?.mapNotNull { doc -> docToStatus(doc) } ?: emptyList()
                trySend(statuses.filter { !it.isExpired })
            }
        registrations.add(reg)

        awaitClose { registrations.forEach { it.remove() } }
    }

    suspend fun createStatus(status: StatusUpdate): Result<String> {
        return try {
            val docRef = firestore.collection("statuses").document()
            val data = mapOf(
                "creatorZixoNumber" to status.creatorZixoNumber,
                "creatorDisplayName" to status.creatorDisplayName,
                "creatorAvatarUrl" to status.creatorAvatarUrl,
                "timestamp" to FieldValue.serverTimestamp(),
                "textContent" to status.textContent,
                "mediaUrl" to status.mediaUrl,
                "overlaySymbols" to status.overlaySymbols,
                "backgroundColorHex" to status.backgroundColorHex,
                "reactions" to emptyMap<String, Int>(),
                "viewedBy" to emptyList<String>(),
                "expiresInHours" to status.expiresInHours,
                "isExpired" to false
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to create status"))
        }
    }

    suspend fun addReaction(statusId: String, emoji: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("statuses").document(statusId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val reactions = (snapshot.get("reactions") as? Map<String, Long>) ?: emptyMap()
                val currentCount = (reactions[emoji] ?: 0L).toInt() + 1
                val updatedReactions = reactions.toMutableMap()
                updatedReactions[emoji] = currentCount.toLong()
                transaction.update(docRef, "reactions", updatedReactions)
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to react to status"))
        }
    }

    suspend fun markStatusViewed(statusId: String, viewerZixoNumber: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("statuses").document(statusId)
            val doc = docRef.get().await()
            val viewedBy = (doc.get("viewedBy") as? List<String>) ?: emptyList()
            if (!viewedBy.contains(viewerZixoNumber)) {
                docRef.update("viewedBy", viewedBy + viewerZixoNumber).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to mark status as viewed"))
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        return try {
            firestore.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to delete status"))
        }
    }

    private fun docToStatus(doc: com.google.firebase.firestore.DocumentSnapshot): StatusUpdate? {
        return try {
            StatusUpdate(
                statusId = doc.id,
                creatorZixoNumber = doc.getString("creatorZixoNumber") ?: "",
                creatorDisplayName = doc.getString("creatorDisplayName") ?: "",
                creatorAvatarUrl = doc.getString("creatorAvatarUrl") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                textContent = doc.getString("textContent"),
                mediaUrl = doc.getString("mediaUrl"),
                overlaySymbols = (doc.get("overlaySymbols") as? List<String>) ?: emptyList(),
                backgroundColorHex = doc.getString("backgroundColorHex") ?: "#1A2A32",
                reactions = (doc.get("reactions") as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap(),
                viewedBy = (doc.get("viewedBy") as? List<String>) ?: emptyList(),
                expiresInHours = (doc.getLong("expiresInHours")?.toInt() ?: 24),
                isExpired = doc.getBoolean("isExpired") ?: false
            )
        } catch (e: Exception) { null }
    }
}
