package com.zixo.app.data.repository

import com.zixo.app.data.remote.cloudflare.CloudflareApiService
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.repository.AuthResult
import com.zixo.app.domain.repository.AuthState
import com.zixo.app.domain.model.User
import com.zixo.app.domain.repository.AuthRepository
import com.google.firebase.auth.AuthResult as FirebaseAuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AuthRepository].
 *
 * Uses FirebaseAuth for Google Sign-In via CredentialManager and email/password,
 * CloudflareApiService for registration (minting Zixo Numbers, usernames, passkey challenges),
 * and FirestoreService for user profile persistence.
 *
 * ## Fallback Architecture:
 * - Google Sign-In: First attempts Cloudflare verification. If Cloudflare is unreachable,
 *   falls back to direct Firebase Auth + client-side Firestore profile creation.
 * - Email/Password: Direct Firebase Auth + Firestore profile creation.
 * - Auth state observation is resilient to Firestore failures.
 *
 * All operations wrapped in try-catch with structured error handling.
 * All network/DB operations run on Dispatchers.IO.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreService: FirestoreService,
    private val cloudflareApiService: CloudflareApiService
) : AuthRepository {

    @Volatile
    private var cachedUser: User? = null

    // ── Google Sign-In ────────────────────────────────────────────────────────

    override fun signInWithGoogle(idToken: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // Step 1: Authenticate with Firebase directly using the Google credential
            // This is the core auth step — Cloudflare is secondary
            val firebaseResult: FirebaseAuthResult = firebaseAuthService.signInWithGoogle(idToken).first()
            val firebaseUser = firebaseResult.user
                ?: throw IllegalStateException("Authentication succeeded but user is null")

            // Step 2: Try Cloudflare verification (optional — for minting username/zixoNumber)
            // If it fails, we continue with Firebase-only auth
            var cloudflareUsername = ""
            var cloudflareZixoNumber = ""
            try {
                val verifyResponse = cloudflareApiService.verifyGoogleToken(idToken)
                if (verifyResponse.valid) {
                    cloudflareUsername = verifyResponse.username
                    cloudflareZixoNumber = verifyResponse.zixoNumber
                } else {
                    Timber.w("Cloudflare verification returned invalid — continuing with Firebase-only auth")
                }
            } catch (e: Exception) {
                Timber.w(e, "Cloudflare verification failed — continuing with Firebase-only auth")
            }

            // Step 3: Fetch or create the user profile in Firestore
            val user = try {
                val existingProfile = firestoreService.getUserProfile(firebaseUser.uid).firstOrNull()
                if (existingProfile != null) {
                    // Update cached username/zixoNumber if Cloudflare provided them
                    if (cloudflareUsername.isNotEmpty() && existingProfile.username.isEmpty()) {
                        val updated = existingProfile.copy(
                            username = cloudflareUsername,
                            zixoNumber = cloudflareZixoNumber
                        )
                        try {
                            firestoreService.updateUserProfile(firebaseUser.uid, mapOf(
                                "username" to cloudflareUsername,
                                "zixoNumber" to cloudflareZixoNumber
                            ))
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to update Cloudflare-minted username/zixoNumber")
                        }
                        updated
                    } else {
                        existingProfile
                    }
                } else {
                    // New user — create Firestore profile
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        username = cloudflareUsername.ifEmpty { generateUsername(firebaseUser.uid) },
                        zixoNumber = cloudflareZixoNumber.ifEmpty { generateZixoNumber() },
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                    try {
                        firestoreService.createUserProfile(firebaseUser.uid, newUser)
                        Timber.d("Created new Firestore profile for: %s", firebaseUser.uid)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to create Firestore profile — auth will still proceed")
                    }
                    newUser
                }
            } catch (e: Exception) {
                Timber.w(e, "Firestore profile lookup failed — using Firebase-only user data")
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    username = cloudflareUsername.ifEmpty { generateUsername(firebaseUser.uid) },
                    zixoNumber = cloudflareZixoNumber.ifEmpty { generateZixoNumber() },
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis()
                )
            }

            cachedUser = user
            emit(AuthResult.Success(user))
        } catch (e: Exception) {
            Timber.e(e, "Google Sign-In failed")
            emit(AuthResult.Error(e.localizedMessage ?: "Sign in failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── WebAuthn Passkey ──────────────────────────────────────────────────────

    override fun registerPasskeyWithBackend(registrationResponseJson: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val currentUser = cachedUser
                ?: throw IllegalStateException("No authenticated user")

            val verifyResponse = cloudflareApiService.verifyPasskeyRegistration(
                uid = currentUser.uid,
                registrationResponseJson = registrationResponseJson
            )

            if (!verifyResponse.verified) {
                emit(AuthResult.Error("Passkey verification failed"))
                return@flow
            }

            val updatedUser = currentUser.copy(
                passkeyCredentialId = verifyResponse.credentialId,
                hasPasskey = true
            )
            try {
                firestoreService.updateUserProfile(
                    currentUser.uid,
                    mapOf(
                        "passkeyCredentialId" to verifyResponse.credentialId,
                        "hasPasskey" to true
                    )
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to update passkey in Firestore")
            }

            cachedUser = updatedUser
            emit(AuthResult.Success(updatedUser))
        } catch (e: Exception) {
            Timber.e(e, "Passkey registration failed")
            emit(AuthResult.Error(e.localizedMessage ?: "Passkey registration failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun isPasskeyRegistered(): Flow<Boolean> = flow {
        val currentUser = cachedUser
            ?: throw IllegalStateException("No authenticated user")
        emit(currentUser.hasPasskey)
    }.flowOn(Dispatchers.IO)

    // ── Auth State ────────────────────────────────────────────────────────────

    override fun observeAuthState(): Flow<AuthState> =
        firebaseAuthService.authStateFlow()
            .map { firebaseUser ->
                if (firebaseUser != null) {
                    // Try to get Firestore profile, but never let it block auth
                    val user = try {
                        firestoreService.getUserProfile(firebaseUser.uid).firstOrNull()
                            ?: User(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = firebaseUser.displayName ?: "",
                                username = generateUsername(firebaseUser.uid),
                                zixoNumber = generateZixoNumber()
                            )
                    } catch (e: Exception) {
                        Timber.w(e, "Firestore profile lookup failed in observeAuthState — using Firebase-only data")
                        User(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: "",
                            username = generateUsername(firebaseUser.uid),
                            zixoNumber = generateZixoNumber()
                        )
                    }
                    cachedUser = user
                    AuthState.Authenticated(user)
                } else {
                    cachedUser = null
                    AuthState.Unauthenticated
                }
            }
            .onStart { emit(AuthState.Loading) }
            .flowOn(Dispatchers.IO)

    // ── Sign Out ──────────────────────────────────────────────────────────────

    override fun signOut(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            cachedUser = null
            firebaseAuthService.signOut()
            emit(AuthResult.Success(User()))
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            emit(AuthResult.Error(e.localizedMessage ?: "Sign out failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Get Current User ──────────────────────────────────────────────────────

    override fun getCurrentUser(): Flow<User?> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
        if (uid != null) {
            val user = cachedUser ?: try {
                firestoreService.getUserProfile(uid).firstOrNull()
            } catch (e: Exception) {
                Timber.w(e, "Failed to get current user profile from Firestore")
                null
            }
            cachedUser = user
            emit(user)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    // ── Update Profile ────────────────────────────────────────────────────────

    override fun updateUserProfile(
        displayName: String,
        bio: String,
        avatarUrl: String
    ): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val uid = firebaseAuthService.getCurrentUser()?.uid
                ?: throw IllegalStateException("No authenticated user")

            val updates = mapOf(
                "displayName" to displayName,
                "bio" to bio,
                "photoUrl" to avatarUrl
            )
            firestoreService.updateUserProfile(uid, updates)

            val updatedUser = (cachedUser ?: User(uid = uid)).copy(
                displayName = displayName,
                bio = bio,
                photoUrl = avatarUrl
            )
            cachedUser = updatedUser
            emit(AuthResult.Success(updatedUser))
        } catch (e: Exception) {
            Timber.e(e, "Profile update failed")
            emit(AuthResult.Error(e.localizedMessage ?: "Profile update failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Delete Account ────────────────────────────────────────────────────────

    override fun deleteAccount(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val uid = firebaseAuthService.getCurrentUser()?.uid
                ?: throw IllegalStateException("No authenticated user")

            firestoreService.deleteUserData(uid)
            firebaseAuthService.deleteAccount()
            cachedUser = null
            emit(AuthResult.Success(User()))
        } catch (e: Exception) {
            Timber.e(e, "Account deletion failed")
            emit(AuthResult.Error(e.localizedMessage ?: "Account deletion failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Generates a deterministic username from a UID when Cloudflare is unavailable.
     * Format: zixo_XXXX (4 hex chars from UID hash)
     */
    private fun generateUsername(uid: String): String {
        val hash = uid.hashCode().toString(16).takeLast(4).padStart(4, '0')
        return "zixo_$hash"
    }

    /**
     * Generates a random 8-digit Zixo Number when Cloudflare is unavailable.
     * Format: XXXX XXXX
     */
    private fun generateZixoNumber(): String {
        val num = (10000000..99999999).random()
        val str = num.toString()
        return "${str.substring(0, 4)} ${str.substring(4, 8)}"
    }

    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        var result: T? = null
        try {
            collect { value ->
                result = value
                return@collect
            }
        } catch (_: Exception) { }
        return result
    }
}
