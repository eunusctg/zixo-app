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
    private val rtdb: FirebaseDatabase,
    private val restApi: FirebaseAuthRestApi
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        // Try Firebase SDK first
        return try {
            Log.d(TAG, "Signing up with email (SDK): $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
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
            
            setupPresence(firebaseUser.uid)
            
            Log.d(TAG, "Sign up successful for: $email")
            Result.success(user)
        } catch (sdkError: Exception) {
            Log.w(TAG, "SDK sign-up failed, trying REST API fallback", sdkError)
            
            // Fallback: Use REST API directly
            try {
                val restResult = restApi.signUpWithEmail(email, password)
                if (!restResult.success) {
                    return Result.failure(Exception(restResult.error))
                }
                
                // Now sign in via SDK with the created credentials to get full SDK integration
                try {
                    val sdkResult = auth.signInWithEmailAndPassword(email, password).await()
                    val firebaseUser = sdkResult.user
                    
                    if (firebaseUser != null) {
                        // Update display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                        
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
                        setupPresence(firebaseUser.uid)
                        
                        Log.d(TAG, "Sign up (REST+SDK) successful for: $email")
                        return Result.success(user)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SDK sign-in after REST sign-up also failed", e)
                }
                
                // If SDK sign-in also failed, return the REST API result
                // The user is created but SDK integration might be limited
                val username = "@${displayName.lowercase().replace("""\s+""".toRegex(), "")}${(0..999).random()}"
                val user = User(
                    uid = restResult.uid,
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
                
                // Try to create Firestore profile (might fail without SDK auth)
                try {
                    firestore.collection("users").document(restResult.uid).set(user.toMap()).await()
                    firestore.collection("usernames").document(username).set(mapOf("uid" to restResult.uid)).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore profile creation failed", e)
                }
                
                Result.failure(Exception("Account created but sign-in requires app update. Please contact support."))
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed completely", e)
                Result.failure(e)
            }
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        // Try Firebase SDK first
        return try {
            Log.d(TAG, "Signing in with email (SDK): $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed")
            val user = getUserProfile(firebaseUser.uid).getOrDefault(User())
            
            setupPresence(firebaseUser.uid)
            
            Log.d(TAG, "Sign in successful for: $email")
            Result.success(user)
        } catch (sdkError: FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid credentials", sdkError)
            Result.failure(Exception("Incorrect email or password. Please check your credentials and try again."))
        } catch (sdkError: FirebaseAuthInvalidUserException) {
            Log.e(TAG, "Invalid user", sdkError)
            Result.failure(Exception("No account found with this email. Please sign up first."))
        } catch (sdkError: Exception) {
            Log.w(TAG, "SDK sign-in failed, trying REST API fallback", sdkError)
            
            // Fallback: Use REST API directly
            try {
                val restResult = restApi.signInWithEmail(email, password)
                if (!restResult.success) {
                    return Result.failure(Exception(restResult.error))
                }
                
                // Now try to sign in via SDK to get full integration
                try {
                    val sdkResult = auth.signInWithEmailAndPassword(email, password).await()
                    val firebaseUser = sdkResult.user
                    
                    if (firebaseUser != null) {
                        val user = getUserProfile(firebaseUser.uid).getOrDefault(User())
                        setupPresence(firebaseUser.uid)
                        Log.d(TAG, "Sign in (REST+SDK) successful for: $email")
                        return Result.success(user)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SDK sign-in after REST also failed", e)
                }
                
                // REST API auth succeeded but SDK integration failed
                // This likely means the Android app isn't registered in Firebase Console
                Result.failure(Exception(
                    "Authentication succeeded but the app needs to be updated. " +
                    "Please update Zixo to the latest version or contact support. " +
                    "(Error: Android app not registered in Firebase)"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed completely", e)
                val msg = e.message ?: "Sign in failed"
                Result.failure(Exception(msg))
            }
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
            // Fallback to REST API
            val restResult = restApi.sendPasswordReset(email)
            if (restResult.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(restResult.error))
            }
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
