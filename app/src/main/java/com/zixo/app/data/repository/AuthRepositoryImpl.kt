package com.zixo.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.domain.model.AuthState
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class AuthRepositoryImpl(
    private val dataStore: PreferencesDataStore
) : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val TAG = "AuthRepository"

    init {
        // Check if user is already signed in
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            scope.launch {
                try {
                    val profile = fetchUserProfile(firebaseUser.uid)
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                    dataStore.cacheUserProfile(
                        profile.uid, profile.displayName, profile.username,
                        profile.zixoNumber, profile.phoneNumber, profile.bio, profile.avatarUrl
                    )
                    updateOnlineStatus(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load cached user", e)
                    loadCachedProfile()
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
        return try {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Authentication failed: no user returned")

            val profile = fetchOrCreateUserProfile(firebaseUser.uid, firebaseUser.displayName ?: "", firebaseUser.photoUrl?.toString() ?: "")
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)

            dataStore.cacheUserProfile(
                profile.uid, profile.displayName, profile.username,
                profile.zixoNumber, profile.phoneNumber, profile.bio, profile.avatarUrl
            )
            updateOnlineStatus(true)

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            updateOnlineStatus(false)
            auth.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshUserProfile(): Result<UserProfile> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val profile = fetchUserProfile(uid)
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            dataStore.cacheUserProfile(
                profile.uid, profile.displayName, profile.username,
                profile.zixoNumber, profile.phoneNumber, profile.bio, profile.avatarUrl
            )
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh profile failed", e)
            loadCachedProfile()
            Result.failure(e)
        }
    }

    override suspend fun updateOnlineStatus(isOnline: Boolean) {
        try {
            val uid = auth.currentUser?.uid ?: return
            firestore.collection("users").document(uid)
                .update(mapOf("isOnline" to isOnline, "lastSeen" to System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update online status", e)
        }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    private suspend fun fetchUserProfile(uid: String): UserProfile {
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) throw Exception("User profile not found")
        return documentToProfile(doc)
    }

    private suspend fun fetchOrCreateUserProfile(uid: String, displayName: String, photoUrl: String): UserProfile {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                documentToProfile(doc)
            } else {
                createNewUserProfile(uid, displayName, photoUrl)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch/create failed, creating new profile", e)
            createNewUserProfile(uid, displayName, photoUrl)
        }
    }

    private suspend fun createNewUserProfile(uid: String, displayName: String, photoUrl: String): UserProfile {
        val zixoNumber = generateZixoNumber()
        val username = generateUsername(displayName)

        val profile = UserProfile(
            uid = uid,
            displayName = displayName.ifBlank { "Zixo User" },
            username = username,
            zixoNumber = zixoNumber,
            phoneNumber = "",
            bio = "",
            avatarUrl = photoUrl,
            createdAt = System.currentTimeMillis(),
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )

        try {
            firestore.collection("users").document(uid)
                .set(profile.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save new user profile to Firestore", e)
        }

        return profile
    }

    /** Generate 8-digit Zixo Number formatted as "XXXX XXXX" */
    private suspend fun generateZixoNumber(): String {
        var attempts = 0
        while (attempts < 20) {
            val raw = Random.nextInt(10000000, 99999999).toString()
            val formatted = "${raw.substring(0, 4)} ${raw.substring(4)}"
            // Check uniqueness
            try {
                val existing = firestore.collection("users")
                    .whereEqualTo("zixoNumber", formatted)
                    .limit(1).get().await()
                if (existing.isEmpty) return formatted
            } catch (e: Exception) {
                // If query fails, just use the generated number
                return formatted
            }
            attempts++
        }
        // Fallback — use timestamp-based
        val raw = System.currentTimeMillis().toString().takeLast(8)
        return "${raw.substring(0, 4)} ${raw.substring(4)}"
    }

    private fun generateUsername(displayName: String): String {
        val base = displayName.lowercase().replace(Regex("[^a-z0-9]"), "").take(8)
        val suffix = Random.nextInt(100, 999)
        return if (base.isNotBlank()) "zixo_${base}$suffix" else "zixo_user$suffix"
    }

    private fun documentToProfile(doc: com.google.firebase.firestore.DocumentSnapshot): UserProfile {
        return UserProfile(
            uid = doc.id,
            displayName = doc.getString("displayName") ?: "",
            username = doc.getString("username") ?: "",
            zixoNumber = doc.getString("zixoNumber") ?: "",
            phoneNumber = doc.getString("phoneNumber") ?: "",
            bio = doc.getString("bio") ?: "",
            avatarUrl = doc.getString("avatarUrl") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L,
            isOnline = doc.getBoolean("isOnline") ?: false,
            lastSeen = doc.getLong("lastSeen") ?: 0L
        )
    }

    private suspend fun loadCachedProfile() {
        try {
            dataStore.getCachedProfile().collect { cached ->
                if (cached["uid"].isNullOrBlank()) {
                    _authState.value = AuthState.Unauthenticated
                } else {
                    val profile = UserProfile(
                        uid = cached["uid"] ?: "",
                        displayName = cached["displayName"] ?: "",
                        username = cached["username"] ?: "",
                        zixoNumber = cached["zixoNumber"] ?: "",
                        phoneNumber = cached["phoneNumber"] ?: "",
                        bio = cached["bio"] ?: "",
                        avatarUrl = cached["avatarUrl"] ?: ""
                    )
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun UserProfile.toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "username" to username,
        "zixoNumber" to zixoNumber,
        "phoneNumber" to phoneNumber,
        "bio" to bio,
        "avatarUrl" to avatarUrl,
        "createdAt" to createdAt,
        "isOnline" to isOnline,
        "lastSeen" to lastSeen
    )
}
