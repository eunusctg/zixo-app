package com.zexo.app.data.repository

import android.util.Log
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val rtdb: FirebaseDatabase
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            Log.d(TAG, "Signing up with email: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Send email verification
            firebaseUser.sendEmailVerification().await()
            
            // Create user profile
            val username = "@${displayName.lowercase().replace("""\s+""".toRegex(), "")}${(0..999).random()}"
            val user = User(
                uid = firebaseUser.uid,
                displayName = displayName,
                email = email,
                username = username,
                bio = "Living free, connecting freely",
                zixoNumber = generateZixoNumber(),
                online = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                role = if (email == "eunus527@gmail.com") "admin" else "user"
            )
            
            firestore.collection("users").document(firebaseUser.uid).set(user.toMap()).await()
            firestore.collection("usernames").document(username).set(mapOf("uid" to firebaseUser.uid)).await()
            
            // Set online presence
            setupPresence(firebaseUser.uid)
            
            Log.d(TAG, "Sign up successful for: $email")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "Signing in with email: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed")
            val user = getUserProfile(firebaseUser.uid).getOrDefault(User())
            
            // Update online status
            setupPresence(firebaseUser.uid)
            
            Log.d(TAG, "Sign in successful for: $email")
            Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid credentials", e)
            Result.failure(Exception("Incorrect email or password. Please check your credentials and try again."))
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.e(TAG, "Invalid user", e)
            Result.failure(Exception("No account found with this email. Please sign up first."))
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            val msg = e.message ?: "Sign in failed"
            Result.failure(Exception(msg))
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            Log.d(TAG, "Signing in with Google")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign in failed")
            
            var user = getUserProfile(firebaseUser.uid).getOrDefault(null)
            
            if (user == null) {
                // First time Google sign-in, create profile
                val displayName = firebaseUser.displayName ?: firebaseUser.email?.split("@")?.first() ?: "User"
                val username = "@${displayName.lowercase().replace("""\s+""".toRegex(), "")}${(0..999).random()}"
                user = User(
                    uid = firebaseUser.uid,
                    displayName = displayName,
                    email = firebaseUser.email ?: "",
                    username = username,
                    bio = "Living free, connecting freely",
                    avatar = firebaseUser.photoUrl?.toString() ?: "",
                    zixoNumber = generateZixoNumber(),
                    online = true,
                    lastSeen = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                    role = if (firebaseUser.email == "eunus527@gmail.com") "admin" else "user"
                )
                firestore.collection("users").document(firebaseUser.uid).set(user.toMap()).await()
                firestore.collection("usernames").document(username).set(mapOf("uid" to firebaseUser.uid)).await()
            } else {
                // Update online status for existing user
                firestore.collection("users").document(firebaseUser.uid)
                    .update(mapOf("online" to true, "lastSeen" to System.currentTimeMillis())).await()
            }
            
            setupPresence(firebaseUser.uid)
            Log.d(TAG, "Google sign in successful")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = User(
                    uid = doc.getString("uid") ?: "",
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
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get user profile failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update(updates).await()
            if (updates.containsKey("username")) {
                val username = updates["username"] as String
                firestore.collection("usernames").document(username).set(mapOf("uid" to uid)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            currentUid?.let { uid ->
                try {
                    firestore.collection("users").document(uid)
                        .update(mapOf("online" to false, "lastSeen" to System.currentTimeMillis())).await()
                } catch (_: Exception) {}
                try {
                    rtdb.getReference("presence").child(uid).removeValue()
                } catch (_: Exception) {}
            }
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun setupPresence(uid: String) {
        try {
            val presenceRef = rtdb.getReference("presence").child(uid)
            val connectedRef = rtdb.getReference(".info/connected")
            
            connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        presenceRef.setValue(mapOf(
                            "online" to true,
                            "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP
                        ))
                        presenceRef.onDisconnect().setValue(mapOf(
                            "online" to false,
                            "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP
                        ))
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.e(TAG, "Setup presence failed", e)
        }
    }

    private fun generateZixoNumber(): String {
        return "ZIXO${(100000..999999).random()}"
    }
}
