package com.zexo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object { private const val TAG = "UserRepository" }

    suspend fun searchUsers(query: String, currentUid: String, maxResults: Int = 20): Result<List<User>> {
        return try {
            val results = mutableListOf<User>()
            val searchLower = query.lowercase().removePrefix("@")
            val searchUpper = searchLower + "\uf8ff"
            
            // Search by username
            try {
                val q1 = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", "@$searchLower")
                    .whereLessThan("username", "@$searchUpper")
                    .limit(maxResults.toLong())
                    .get().await()
                for (doc in q1.documents) {
                    val user = parseUser(doc)
                    if (user != null && user.uid != currentUid) results.add(user)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Username search failed", e)
            }
            
            // Search by displayName
            try {
                val capitalSearch = searchLower.replaceFirstChar { it.uppercase() }
                val q2 = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("displayName", capitalSearch)
                    .whereLessThan("displayName", capitalSearch + "\uf8ff")
                    .limit(maxResults.toLong())
                    .get().await()
                for (doc in q2.documents) {
                    val user = parseUser(doc)
                    if (user != null && user.uid != currentUid && results.none { it.uid == user.uid }) {
                        results.add(user)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Display name search failed", e)
            }

            // Search by email
            try {
                val q3 = firestore.collection("users")
                    .whereEqualTo("email", query)
                    .limit(5L)
                    .get().await()
                for (doc in q3.documents) {
                    val user = parseUser(doc)
                    if (user != null && user.uid != currentUid && results.none { it.uid == user.uid }) {
                        results.add(user)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Email search failed", e)
            }

            // Search by Zixo number
            try {
                val q4 = firestore.collection("users")
                    .whereEqualTo("zixoNumber", query.uppercase())
                    .limit(5L)
                    .get().await()
                for (doc in q4.documents) {
                    val user = parseUser(doc)
                    if (user != null && user.uid != currentUid && results.none { it.uid == user.uid }) {
                        results.add(user)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Zixo number search failed", e)
            }

            // Search by phone
            try {
                val q5 = firestore.collection("users")
                    .whereEqualTo("phone", query)
                    .limit(5L)
                    .get().await()
                for (doc in q5.documents) {
                    val user = parseUser(doc)
                    if (user != null && user.uid != currentUid && results.none { it.uid == user.uid }) {
                        results.add(user)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Phone search failed", e)
            }
            
            // Fallback: client-side search
            if (results.isEmpty()) {
                try {
                    val allUsers = firestore.collection("users").limit(50L).get().await()
                    for (doc in allUsers.documents) {
                        val user = parseUser(doc)
                        if (user != null && user.uid != currentUid) {
                            val nameMatch = user.displayName.lowercase().contains(searchLower)
                            val unameMatch = user.username.lowercase().contains(searchLower)
                            val emailMatch = user.email.lowercase().contains(searchLower)
                            val zixoMatch = user.zixoNumber.contains(searchLower, ignoreCase = true)
                            val phoneMatch = user.phone.contains(query)
                            if (nameMatch || unameMatch || emailMatch || zixoMatch || phoneMatch) {
                                if (results.none { it.uid == user.uid }) results.add(user)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback search failed", e)
                }
            }
            
            Result.success(results.take(maxResults))
        } catch (e: Exception) {
            Log.e(TAG, "Search users failed", e)
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(currentUid: String, maxResults: Int = 50): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").limit(maxResults.toLong() + 1).get().await()
            val users = snapshot.documents.mapNotNull { doc -> parseUser(doc) }
                .filter { it.uid != currentUid }
            Result.success(users.take(maxResults))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByUid(uid: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val user = parseUser(doc)
            if (user != null) Result.success(user) else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePresence(uids: List<String>, callback: (Map<String, Pair<Boolean, Long>>) -> Unit) {
        val statuses = mutableMapOf<String, Pair<Boolean, Long>>()
        val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance(
            "https://zixo-call-default-rtdb.firebaseio.com/"
        )
        uids.forEach { uid ->
            rtdb.getReference("presence").child(uid)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val online = snapshot.child("online").getValue(Boolean::class.java) ?: false
                        val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                        statuses[uid] = Pair(online, lastSeen)
                        callback(statuses.toMap())
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
        }
    }

    private fun parseUser(doc: com.google.firebase.firestore.DocumentSnapshot): User? {
        return try {
            User(
                uid = doc.getString("uid") ?: return null,
                displayName = doc.getString("displayName") ?: "",
                email = doc.getString("email") ?: "",
                username = doc.getString("username") ?: "",
                bio = doc.getString("bio") ?: "",
                avatar = doc.getString("avatar") ?: "",
                phone = doc.getString("phone") ?: "",
                zixoNumber = doc.getString("zixoNumber") ?: "",
                online = doc.getBoolean("online") ?: false,
                lastSeen = doc.getLong("lastSeen") ?: 0L,
                createdAt = doc.getLong("createdAt") ?: 0L,
                role = doc.getString("role") ?: "user",
                fcmToken = doc.getString("fcmToken") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
