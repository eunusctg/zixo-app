package com.zixo.app.ui.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.zixo.app.data.repository.AuthRepositoryImpl
import com.zixo.app.domain.model.AuthState
import com.zixo.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val authRepository: AuthRepositoryImpl
) {
    private val TAG = "AuthViewModel"

    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Google Sign-In using Credential Manager API (Android 14+) */
    suspend fun signInWithGoogle(activity: Activity) {
        try {
            _isLoading.value = true
            _errorMessage.value = null

            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential

            // Properly extract the Google ID token from the credential
            val googleIdToken: String = when {
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                }
                else -> {
                    throw Exception("Unexpected credential type: ${credential.type}")
                }
            }

            if (googleIdToken.isBlank()) {
                throw Exception("No ID token received from Google")
            }

            val authResult = authRepository.signInWithGoogle(googleIdToken)
            if (authResult.isFailure) {
                _errorMessage.value = authResult.exceptionOrNull()?.message ?: "Sign-in failed"
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential exception", e)
            _errorMessage.value = when {
                e.message?.contains("cancelled", ignoreCase = true) == true -> null
                e.message?.contains("No credential", ignoreCase = true) == true -> "No Google account found. Please add a Google account to your device."
                else -> "Google Sign-In failed. Please try again."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in error", e)
            _errorMessage.value = e.message ?: "An unexpected error occurred"
        } finally {
            _isLoading.value = false
        }
    }

    /** Email/Password Sign-In */
    suspend fun signInWithEmail(email: String, password: String) {
        try {
            _isLoading.value = true
            _errorMessage.value = null

            if (email.isBlank()) {
                _errorMessage.value = "Please enter your email"
                return
            }
            if (password.isBlank()) {
                _errorMessage.value = "Please enter your password"
                return
            }

            val authResult = authRepository.signInWithEmail(email, password)
            if (authResult.isFailure) {
                _errorMessage.value = authResult.exceptionOrNull()?.message ?: "Sign-in failed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in error", e)
            _errorMessage.value = e.message ?: "An unexpected error occurred"
        } finally {
            _isLoading.value = false
        }
    }

    /** Email/Password Registration */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        try {
            _isLoading.value = true
            _errorMessage.value = null

            if (email.isBlank()) {
                _errorMessage.value = "Please enter your email"
                return
            }
            if (password.isBlank()) {
                _errorMessage.value = "Please enter a password"
                return
            }
            if (password.length < 6) {
                _errorMessage.value = "Password must be at least 6 characters"
                return
            }
            if (displayName.isBlank()) {
                _errorMessage.value = "Please enter your display name"
                return
            }
            if (displayName.length > 30) {
                _errorMessage.value = "Display name must be 30 characters or less"
                return
            }

            val authResult = authRepository.signUpWithEmail(email, password, displayName)
            if (authResult.isFailure) {
                _errorMessage.value = authResult.exceptionOrNull()?.message ?: "Registration failed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up error", e)
            _errorMessage.value = e.message ?: "An unexpected error occurred"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signOut() {
        try {
            authRepository.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out error", e)
            _errorMessage.value = "Failed to sign out"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private const val WEB_CLIENT_ID = "809372450511-7lpbcas0a6nrntljvs5sf4v9nnusrpol.apps.googleusercontent.com"
    }
}
