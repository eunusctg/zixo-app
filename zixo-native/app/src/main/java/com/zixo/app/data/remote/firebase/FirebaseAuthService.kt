package com.zixo.app.data.remote.firebase

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication Service — Pure FirebaseAuth + Google Sign-In via CredentialManager.
 *
 * ## Privacy Architecture — No Email-Based Auth:
 *
 * Email-based lookups are explicitly forbidden by the Zixo privacy architecture.
 * The following methods have been REMOVED:
 * - `signInWithEmail()` — Users sign in via Google Sign-In only
 * - `signUpWithEmail()` — Registration uses Google Sign-In + Cloudflare verification
 * - `sendPasswordResetEmail()` — No email-based recovery; passkeys are used instead
 *
 * All authentication flows use FirebaseAuth directly with Google credentials
 * obtained via Android CredentialManager. The email field exists in FirebaseAuth
 * internally but is NEVER exposed as a lookup or search mechanism.
 *
 * NO LiveKit references. All operations run on Dispatchers.IO via the calling repository.
 */
@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Sign in with a Google ID token obtained via CredentialManager.
     * Converts the ID token to a Firebase credential and authenticates.
     *
     * This is the ONLY supported sign-in method. No email/password, no anonymous auth.
     *
     * @param idToken The Google Sign-In ID token.
     * @return The Firebase [AuthResult].
     */
    fun signInWithGoogle(idToken: String): Flow<AuthResult> = flow {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        Timber.d("Google Sign-In successful for: %s", result.user?.uid)
        emit(result)
    }

    /**
     * Sign out the current user.
     */
    suspend fun signOut() {
        firebaseAuth.signOut()
        Timber.d("User signed out")
    }

    /**
     * Delete the currently signed-in account.
     * Throws [IllegalStateException] if no user is signed in.
     */
    suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No authenticated user to delete")
        user.delete().await()
        Timber.d("Account deleted: %s", user.uid)
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
        trySend(firebaseAuth.currentUser)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}
