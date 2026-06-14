package com.zixo.app.data.repository

import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    data class Authenticated(val user: User) : AuthState()
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
}

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) {

    @Volatile
    private var cachedUser: User? = null

    /**
     * Observes Firebase authentication state and maps it to [AuthState].
     * Emits [AuthState.Loading] on start, then [AuthState.Authenticated] or
     * [AuthState.Unauthenticated] based on the current Firebase user.
     * When authenticated, the full user profile is fetched from Firestore.
     */
    fun authStateFlow(): Flow<AuthState> =
        firebaseAuthService.authStateFlow()
            .map { firebaseUser ->
                if (firebaseUser != null) {
                    val user = firestoreService.getUserProfile(firebaseUser.uid).first()
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

    /**
     * Signs in a user with email and password.
     * Emits [AuthResult.Loading], then [AuthResult.Success] or [AuthResult.Error].
     */
    fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val authResult = firebaseAuthService.signInWithEmail(email, password).first()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Authentication succeeded but user is null")
            val user = firestoreService.getUserProfile(firebaseUser.uid).first()
                ?: User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: ""
                )
            cachedUser = user
            emit(AuthResult.Success(user))
        } catch (e: Exception) {
            emit(AuthResult.Error(e.localizedMessage ?: "Sign in failed"))
        }
    }

    /**
     * Creates a new user account with email, password, and display name.
     * Creates both the Firebase Auth account and the Firestore user profile.
     * Emits [AuthResult.Loading], then [AuthResult.Success] or [AuthResult.Error].
     */
    fun signUp(email: String, password: String, displayName: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val authResult = firebaseAuthService.signUpWithEmail(email, password, displayName).first()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Account creation succeeded but user is null")
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                createdAt = System.currentTimeMillis()
            )
            firestoreService.createUserProfile(firebaseUser.uid, user).first()
            cachedUser = user
            emit(AuthResult.Success(user))
        } catch (e: Exception) {
            emit(AuthResult.Error(e.localizedMessage ?: "Sign up failed"))
        }
    }

    /**
     * Signs out the current user from Firebase Auth and clears the cached profile.
     */
    suspend fun signOut() {
        cachedUser = null
        firebaseAuthService.signOut()
    }

    /**
     * Deletes the current user's account including their Firestore data
     * and Firebase Authentication account.
     */
    suspend fun deleteAccount() {
        val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
        try {
            firestoreService.deleteUserData(uid).first()
            firebaseAuthService.deleteAccount()
        } finally {
            cachedUser = null
        }
    }

    /**
     * Returns the currently cached [User] profile, or null if not authenticated.
     * This is a synchronous accessor backed by the most recently fetched profile.
     * For real-time updates, use [authStateFlow] instead.
     */
    fun getCurrentUser(): User? = cachedUser

    /**
     * Sends a password reset email to the specified address.
     * Emits [Unit] once the email has been sent successfully.
     */
    fun sendPasswordReset(email: String): Flow<Unit> = flow {
        firebaseAuthService.sendPasswordResetEmail(email).first()
        emit(Unit)
    }

    /**
     * Requests a GDPR-style account data export report.
     * The report is prepared server-side and delivered to the user's registered email.
     */
    suspend fun requestAccountInfo() {
        val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
        firestoreService.requestAccountInfo(uid).first()
    }
}
