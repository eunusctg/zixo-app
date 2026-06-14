package com.zixo.app.data.remote.firebase

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Sign in with email and password.
     * Emits the [AuthResult] on success or throws on failure.
     */
    fun signInWithEmail(email: String, password: String): Flow<AuthResult> = flow {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        emit(result)
    }

    /**
     * Sign up with email, password, and display name.
     * Creates the user, updates the display name profile, and emits the [AuthResult].
     */
    fun signUpWithEmail(email: String, password: String, displayName: String): Flow<AuthResult> =
        flow {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            result.user?.updateProfile(profileUpdate)?.await()

            emit(result)
        }

    /**
     * Sign out the current user.
     */
    suspend fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Delete the currently signed-in account.
     * Throws [IllegalStateException] if no user is signed in.
     */
    suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No authenticated user to delete")
        user.delete().await()
    }

    /**
     * Returns the currently authenticated [FirebaseUser], or null if none.
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Emits the current [FirebaseUser] on every auth state change.
     * Starts with the current user at collection time.
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)

        // Emit the initial state immediately
        trySend(firebaseAuth.currentUser)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    /**
     * Send a password-reset email to the given address.
     * Emits [Unit] on success.
     */
    fun sendPasswordResetEmail(email: String): Flow<Unit> = flow {
        firebaseAuth.sendPasswordResetEmail(email).await()
        emit(Unit)
    }
}
