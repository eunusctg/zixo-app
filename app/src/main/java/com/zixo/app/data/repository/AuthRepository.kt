package com.zixo.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zixo.app.data.model.ZixoUser
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _currentUser = MutableStateFlow<ZixoUser?>(null)
    val currentUser: StateFlow<ZixoUser?> = _currentUser

    val firebaseUser: FirebaseUser? get() = auth.currentUser

    val isSignedIn: Boolean get() = auth.currentUser != null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadUserProfile(user.uid)
            } else {
                _currentUser.value = null
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _currentUser.value = docToUser(doc, uid)
                }
            }
    }

    private fun docToUser(doc: com.google.firebase.firestore.DocumentSnapshot, fallbackUid: String): ZixoUser {
        return ZixoUser(
            uid = doc.getString("uid") ?: fallbackUid,
            displayName = doc.getString("displayName") ?: "",
            email = doc.getString("email") ?: "",
            username = doc.getString("username") ?: "",
            bio = doc.getString("bio") ?: "",
            avatar = doc.getString("avatar") ?: "",
            phone = doc.getString("phone") ?: "",
            online = doc.getBoolean("online") ?: false,
            lastSeen = safeTimestamp(doc, "lastSeen"),
            createdAt = safeTimestamp(doc, "createdAt"),
            zixoNumber = doc.getString("zixoNumber") ?: "",
            role = doc.getString("role") ?: "user",
            blockedUsers = (doc.get("blockedUsers") as? List<String>) ?: emptyList(),
            lastSeenVisibility = doc.getString("lastSeenVisibility") ?: "everyone",
            readReceipts = doc.getBoolean("readReceipts") ?: true,
            onlineStatus = doc.getBoolean("onlineStatus") ?: true
        )
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

    suspend fun signUp(email: String, password: String, displayName: String): Result<ZixoUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("User creation failed"))

            firebaseUser.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
            ).await()

            val username = "@${displayName.lowercase().replace("\\s+".toRegex(), "")}${(Math.random() * 1000).toInt()}"
            val zixoNumber = generateZixoNumber()

            val profile = ZixoUser(
                uid = firebaseUser.uid,
                displayName = displayName,
                email = email,
                username = username,
                bio = "Living free, connecting freely",
                avatar = "",
                phone = "",
                online = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                zixoNumber = zixoNumber,
                role = "user"
            )

            firestore.collection("users").document(firebaseUser.uid).set(
                mapOf(
                    "uid" to profile.uid,
                    "displayName" to profile.displayName,
                    "email" to profile.email,
                    "username" to profile.username,
                    "bio" to profile.bio,
                    "avatar" to profile.avatar,
                    "phone" to profile.phone,
                    "online" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "zixoNumber" to profile.zixoNumber,
                    "role" to profile.role,
                    "blockedUsers" to emptyList<String>(),
                    "lastSeenVisibility" to "everyone",
                    "readReceipts" to true,
                    "onlineStatus" to true
                )
            ).await()

            firestore.collection("usernames").document(username).set(
                mapOf("uid" to firebaseUser.uid)
            ).await()

            _currentUser.value = profile
            Result.success(profile)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password is too weak. Use at least 6 characters."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email address."))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("This email is already registered."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign up failed. Please try again."))
        }
    }

    suspend fun signIn(email: String, password: String): Result<ZixoUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Sign in failed"))

            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (doc.exists()) {
                val profile = docToUser(doc, firebaseUser.uid)

                firestore.collection("users").document(firebaseUser.uid).update(
                    mapOf("online" to true, "lastSeen" to FieldValue.serverTimestamp())
                ).await()

                _currentUser.value = profile
                Result.success(profile)
            } else {
                Result.failure(Exception("User profile not found."))
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email or password."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign in failed."))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Password reset failed."))
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection("users").document(uid).update(
                    mapOf("online" to false, "lastSeen" to FieldValue.serverTimestamp())
                ).await()
            }
            auth.signOut()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign out failed."))
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user signed in"))
            firestore.collection("users").document(user.uid).delete().await()
            user.delete().await()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Account deletion failed."))
        }
    }

    suspend fun updateProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
            firestore.collection("users").document(uid).update(updates).await()
            loadUserProfile(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Profile update failed."))
        }
    }

    private fun generateZixoNumber(): String {
        val p1 = (1000..9999).random()
        val p2 = (1000..9999).random()
        return "$p1$p2"
    }

    suspend fun signInWithGoogle(idToken: String): Result<ZixoUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Google sign-in failed"))
            val isNewUser = result.additionalUserInfo?.isNewUser == true

            if (isNewUser) {
                val displayName = firebaseUser.displayName ?: "Zixo User"
                val email = firebaseUser.email ?: ""
                val username = "@${displayName.lowercase().replace("\\s+".toRegex(), "")}${(Math.random() * 1000).toInt()}"
                val zixoNumber = generateZixoNumber()

                val profile = ZixoUser(
                    uid = firebaseUser.uid,
                    displayName = displayName,
                    email = email,
                    username = username,
                    bio = "Living free, connecting freely",
                    avatar = firebaseUser.photoUrl?.toString() ?: "",
                    phone = "",
                    online = true,
                    lastSeen = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                    zixoNumber = zixoNumber,
                    role = "user"
                )

                firestore.collection("users").document(firebaseUser.uid).set(
                    mapOf(
                        "uid" to profile.uid,
                        "displayName" to profile.displayName,
                        "email" to profile.email,
                        "username" to profile.username,
                        "bio" to profile.bio,
                        "avatar" to profile.avatar,
                        "phone" to profile.phone,
                        "online" to true,
                        "lastSeen" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "zixoNumber" to profile.zixoNumber,
                        "role" to profile.role,
                        "blockedUsers" to emptyList<String>(),
                        "lastSeenVisibility" to "everyone",
                        "readReceipts" to true,
                        "onlineStatus" to true
                    )
                ).await()

                firestore.collection("usernames").document(username).set(
                    mapOf("uid" to firebaseUser.uid)
                ).await()

                _currentUser.value = profile
                Result.success(profile)
            } else {
                val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
                if (doc.exists()) {
                    val profile = docToUser(doc, firebaseUser.uid)
                    firestore.collection("users").document(firebaseUser.uid).update(
                        mapOf("online" to true, "lastSeen" to FieldValue.serverTimestamp())
                    ).await()
                    _currentUser.value = profile
                    Result.success(profile)
                } else {
                    Result.failure(Exception("User profile not found."))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Google sign-in failed."))
        }
    }
}
