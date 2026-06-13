package com.zixo.app.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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

    /**
     * Check if Google Play Services is available and up-to-date on the device.
     * Returns null if available, or an error message if not.
     */
    fun checkGooglePlayServices(context: Context): String? {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                return "Google Play Services needs to be updated for Google Sign-In. Please update it from the Play Store."
            }
            return "Google Play Services is not available on this device. Please use Email Sign-In instead."
        }
        return null
    }

    /** Google Sign-In using Credential Manager API (Android 14+) */
    suspend fun signInWithGoogle(activity: Activity) {
        try {
            _isLoading.value = true
            _errorMessage.value = null

            // First check Google Play Services
            val gpsError = checkGooglePlayServices(activity)
            if (gpsError != null) {
                _errorMessage.value = gpsError
                return
            }

            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
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
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No Google credential available", e)
            _errorMessage.value = "No Google account found on this device. Please sign in with Email instead, or add a Google account in your device Settings."
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential exception", e)
            _errorMessage.value = when {
                e.message?.contains("cancelled", ignoreCase = true) == true -> null
                e.message?.contains("No credential", ignoreCase = true) == true ->
                    "No Google account found on this device. Please sign in with Email instead, or add a Google account in Settings."
                e.message?.contains("blocked", ignoreCase = true) == true ->
                    "Google Sign-In is temporarily unavailable. Please try again or use Email Sign-In."
                else -> "Google Sign-In failed. Please use Email Sign-In instead."
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
                val exception = authResult.exceptionOrNull()
                _errorMessage.value = when {
                    exception?.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                        "Invalid email or password. Please check your credentials and try again."
                    exception?.message?.contains("user not found", ignoreCase = true) == true ->
                        "No account found with this email. Please register first."
                    exception?.message?.contains("too many", ignoreCase = true) == true ->
                        "Too many failed attempts. Please try again later."
                    exception?.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."
                    else -> exception?.message ?: "Sign-in failed"
                }
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
                val exception = authResult.exceptionOrNull()
                _errorMessage.value = when {
                    exception?.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true ->
                        "An account with this email already exists. Please sign in instead."
                    exception?.message?.contains("WEAK_PASSWORD", ignoreCase = true) == true ->
                        "Password is too weak. Please use a stronger password."
                    exception?.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."
                    else -> exception?.message ?: "Registration failed"
                }
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
