package com.zixo.app.data.repository

import android.credentials.CreatePublicKeyCredentialResponse
import com.zixo.app.data.remote.cloudflare.CloudflareApiService
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.AuthResult
import com.zixo.app.domain.model.AuthState
import com.zixo.app.domain.model.User
import com.zixo.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AuthRepository].
 *
 * Uses FirebaseAuth for Google Sign-In via CredentialManager,
 * CloudflareApiService for registration (minting Zixo Numbers, usernames, passkey challenges),
 * and FirestoreService for user profile persistence.
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
            // Verify the Google Sign-In token with Cloudflare backend
            val verifyResponse = cloudflareApiService.verifyGoogleToken(idToken)
            if (!verifyResponse.valid) {
                emit(AuthResult.Error("Google Sign-In verification failed"))
                return@flow
            }

            // Authenticate with Firebase using the verified credential
            val authResult = firebaseAuthService.signInWithGoogle(idToken)
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Authentication succeeded but user is null")

            // Fetch or create the user profile in Firestore
            val user = firestoreService.getUserProfile(firebaseUser.uid).let { profileFlow ->
                profileFlow.firstOrNull() ?: run {
                    // New user — Cloudflare backend has already minted username + zixoNumber
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: verifyResponse.displayName,
                        username = verifyResponse.username,
                        zixoNumber = verifyResponse.zixoNumber,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                    firestoreService.createUserProfile(firebaseUser.uid, newUser)
                    newUser
                }
            }

            cachedUser = user
            emit(AuthResult.Success(user))
        } catch (e: Exception) {
            Timber.e(e, "Google Sign-In failed")
            emit(AuthResult.Error(e.localizedMessage ?: "Sign in failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── WebAuthn Passkey ──────────────────────────────────────────────────────

    override fun registerPasskeyWithBackend(response: CreatePublicKeyCredentialResponse): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val currentUser = cachedUser
                ?: throw IllegalStateException("No authenticated user")

            // Verify the passkey registration with Cloudflare backend
            val verifyResponse = cloudflareApiService.verifyPasskeyRegistration(
                credentialId = response.credentialId,
                authenticatorData = response.authenticatorData,
                clientDataJSON = response.clientDataJSON,
                signature = response.signature
            )

            if (!verifyResponse.verified) {
                emit(AuthResult.Error("Passkey verification failed"))
                return@flow
            }

            // Update the user profile with passkey info
            val updatedUser = currentUser.copy(
                passkeyCredentialId = verifyResponse.credentialId,
                hasPasskey = true
            )
            firestoreService.updateUserProfile(
                currentUser.uid,
                mapOf(
                    "passkeyCredentialId" to verifyResponse.credentialId,
                    "hasPasskey" to true
                )
            )

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
                    val user = firestoreService.getUserProfile(firebaseUser.uid).firstOrNull()
                        ?: User(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: ""
                        )
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
            val user = cachedUser ?: firestoreService.getUserProfile(uid).firstOrNull()
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

    // ── Private Helper ────────────────────────────────────────────────────────

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
