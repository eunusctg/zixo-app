package com.zixo.app.ui.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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
            val googleIdToken = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                ?: throw Exception("No ID token in credential")

            val authResult = authRepository.signInWithGoogle(googleIdToken)
            if (authResult.isFailure) {
                _errorMessage.value = authResult.exceptionOrNull()?.message ?: "Sign-in failed"
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential exception", e)
            _errorMessage.value = when {
                e.message?.contains("cancelled") == true -> null
                else -> "Google Sign-In failed. Please try again."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in error", e)
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
