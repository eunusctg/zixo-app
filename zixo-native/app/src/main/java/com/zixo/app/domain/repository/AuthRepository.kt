package com.zixo.app.domain.repository

import com.zixo.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Auth Repository Interface — Google Sign-In + WebAuthn Passkeys
 *
 * Handles user authentication via Google Sign-In through CredentialManager,
 * WebAuthn passkey registration/verification with Cloudflare backend,
 * and user profile management through Firestore.
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
 */

/** Result of an authentication operation. */
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
}

/** Current authentication state. */
sealed class AuthState {
    data class Authenticated(val user: User) : AuthState()
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
}

interface AuthRepository {

    /**
     * Signs in a user with a Google ID token obtained via CredentialManager.
     * Verifies the token with the Cloudflare backend, then creates/retrieves
     * the user profile from Firestore.
     *
     * @param idToken The Google Sign-In ID token.
     * @return A flow emitting [AuthResult] transitions (Loading → Success/Error).
     */
    fun signInWithGoogle(idToken: String): Flow<AuthResult>

    /**
     * Registers a WebAuthn passkey for the currently authenticated user.
     * The [response] is the result of the CredentialManager create flow,
     * which is verified against the Cloudflare backend.
     *
     * @param response The CreatePublicKeyCredentialResponse from CredentialManager.
     * @return A flow emitting [AuthResult] transitions.
     */
    fun registerPasskeyWithBackend(response: android.credentials.CreatePublicKeyCredentialResponse): Flow<AuthResult>

    /**
     * Checks whether the current user has registered a WebAuthn passkey.
     *
     * @return A flow emitting true if a passkey is registered.
     */
    fun isPasskeyRegistered(): Flow<Boolean>

    /**
     * Observes the current authentication state in real-time.
     * Emits [AuthState.Loading] on start, then [AuthState.Authenticated]
     * or [AuthState.Unauthenticated] based on Firebase Auth state.
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Signs out the current user from Firebase Auth and clears cached state.
     *
     * @return A flow emitting [AuthResult] transitions.
     */
    fun signOut(): Flow<AuthResult>

    /**
     * Returns the current authenticated user profile, or null.
     * For real-time updates, use [observeAuthState] instead.
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Updates the current user's profile fields in Firestore.
     * Username and ZixoNumber are read-only and cannot be updated.
     *
     * @param displayName The new display name.
     * @param bio The new bio/about text.
     * @param avatarUrl The new avatar URL.
     * @return A flow emitting [AuthResult] transitions.
     */
    fun updateUserProfile(displayName: String, bio: String, avatarUrl: String): Flow<AuthResult>

    /**
     * Permanently deletes the authenticated user's account,
     * including Firestore documents, Firebase Auth record, and local data.
     *
     * @return A flow emitting [AuthResult] transitions.
     */
    fun deleteAccount(): Flow<AuthResult>
}
