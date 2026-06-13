package com.zexo.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zexo.app.data.model.ZixoUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val profileCache = mutableMapOf<String, ZixoUser>()

    suspend fun getUserProfile(uid: String): Result<ZixoUser> {
        profileCache[uid]?.let { return Result.success(it) }
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = docToUser(doc)
                profileCache[uid] = user
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to fetch user profile"))
        }
    }

    fun observeUserProfile(uid: String): Flow<ZixoUser?> = callbackFlow {
        val reg: ListenerRegistration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val user = snapshot?.let { docToUser(it) }
                if (user != null) profileCache[uid] = user
                trySend(user)
            }
        awaitClose { reg.remove() }
    }

    suspend fun searchUsers(query: String, currentUid: String): Result<List<ZixoUser>> {
        return try {
            val searchLower = query.lowercase().replace("^@".toRegex(), "")
            val results = mutableListOf<ZixoUser>()

            try {
                val snap1 = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", "@$searchLower")
                    .whereLessThan("username", "@${searchLower}\uf8ff")
                    .limit(20).get().await()
                snap1.documents.forEach { doc ->
                    val user = docToUser(doc)
                    if (user.uid != currentUid) results.add(user)
                }
            } catch (_: Exception) {}

            try {
                val snap2 = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("displayName", searchLower.replaceFirstChar { it.uppercase() })
                    .whereLessThan("displayName", "${searchLower.replaceFirstChar { it.uppercase() }}\uf8ff")
                    .limit(20).get().await()
                snap2.documents.forEach { doc ->
                    val user = docToUser(doc)
                    if (user.uid != currentUid && results.none { it.uid == user.uid }) results.add(user)
                }
            } catch (_: Exception) {}

            if (results.isEmpty()) {
                try {
                    val snap3 = firestore.collection("users").limit(50).get().await()
                    snap3.documents.forEach { doc ->
                        val user = docToUser(doc)
                        if (user.uid != currentUid) {
                            val nameMatch = user.displayName.lowercase().contains(searchLower)
                            val unameMatch = user.username.lowercase().contains(searchLower)
                            if ((nameMatch || unameMatch) && results.none { it.uid == user.uid }) {
                                results.add(user)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            Result.success(results.take(20))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Search failed"))
        }
    }

    suspend fun getAllUsers(currentUid: String): Result<List<ZixoUser>> {
        return try {
            val snapshot = firestore.collection("users").limit(50).get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val user = docToUser(doc)
                if (user.uid != currentUid) user else null
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to fetch users"))
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update(updates).await()
            profileCache.remove(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to update profile"))
        }
    }

    private fun docToUser(doc: com.google.firebase.firestore.DocumentSnapshot): ZixoUser {
        return ZixoUser(
            uid = doc.getString("uid") ?: doc.id,
            displayName = doc.getString("displayName") ?: "",
            email = doc.getString("email") ?: "",
            username = doc.getString("username") ?: "",
            bio = doc.getString("bio") ?: "",
            avatar = doc.getString("avatar") ?: "",
            phone = doc.getString("phone") ?: "",
            online = doc.getBoolean("online") ?: false,
            lastSeen = doc.getLong("lastSeen") ?: 0L,
            createdAt = doc.getLong("createdAt") ?: 0L,
            zixoNumber = doc.getString("zixoNumber") ?: "",
            role = doc.getString("role") ?: "user",
            blockedUsers = (doc.get("blockedUsers") as? List<String>) ?: emptyList(),
            lastSeenVisibility = doc.getString("lastSeenVisibility") ?: "everyone",
            readReceipts = doc.getBoolean("readReceipts") ?: true,
            onlineStatus = doc.getBoolean("onlineStatus") ?: true
        )
    }
}
