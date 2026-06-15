package com.zixo.app.ui.screens.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zixo.app.domain.model.AuthResult
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Authentication UI State for the Zixo Sign-In flow.
 *
 * Zixo uses Google Sign-In via Android CredentialManager — no email/password
 * fields are required. The Cloudflare Edge Worker handles verification and
 * mints the system-generated 8-digit Zixo Number and @username.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpMode: Boolean = false,
    val displayName: String = "",           // Only for sign-up flow after Google auth
    val isProfileSetupNeeded: Boolean = false, // True when new user needs display name
    val googleIdToken: String? = null       // Temporary token from Google Sign-In
)

/**
 * ViewModel for the Zixo authentication flow.
 *
 * Authentication Pipeline:
 * 1. User taps "Continue with Google" → CredentialManager launches Google Sign-In
 * 2. Google ID token is extracted from the credential response
 * 3. Token is sent to [AuthRepository.signInWithGoogle] for verification
 * 4. Cloudflare Edge Worker verifies the token and mints Zixo Number + username
 * 5. Firebase Auth session is created
 * 6. New users may need to set a display name (isProfileSetupNeeded)
 *
 * All operations are wrapped in try-catch and run on Dispatchers.IO.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Observable authentication state from the repository.
     * Used by the navigation layer to determine if the user is authenticated.
     */
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()

    /**
     * Initiates Google Sign-In using Android CredentialManager.
     *
     * This method creates a [GetCredentialRequest] with GoogleIdToken credential
     * options and passes it to the system CredentialManager. The resulting
     * Google ID token is then forwarded to [AuthRepository.signInWithGoogle]
     * for server-side verification via Cloudflare Edge Workers.
     *
     * @param activity The host activity required by CredentialManager.
     */
    fun signInWithGoogle(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption = com.google.android.libraries.identity.googleid.GoogleIdTokenCredentialOptions.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("809372450511-lqm5bvb2m2us2av2qc2t6c0hva3gq5fm.apps.googleusercontent.com")
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                handleCredentialResult(result)
            } catch (e: GetCredentialException) {
                Timber.e(e, "Google Sign-In credential request failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Sign-in was cancelled or failed"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during Google Sign-In")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An unexpected error occurred. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Processes the credential response from CredentialManager.
     *
     * Extracts the Google ID token from the credential and forwards it
     * to the authentication repository for server-side verification.
     */
    private fun handleCredentialResult(result: GetCredentialResponse) {
        val credential = result.credential

        when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    _uiState.update { it.copy(googleIdToken = idToken) }
                    authenticateWithBackend(idToken)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse Google ID token credential")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to process Google Sign-In data"
                        )
                    }
                }
            }
            else -> {
                Timber.w("Unexpected credential type: ${credential.type}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unsupported sign-in method"
                    )
                }
            }
        }
    }

    /**
     * Forwards the Google ID token to the backend for verification.
     *
     * The token is verified by the Cloudflare Edge Worker, which also
     * handles new user registration (minting Zixo Number + username).
     */
    private fun authenticateWithBackend(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is AuthResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the display name for a new user after Google Sign-In.
     *
     * This is only needed when the user is signing up for the first time.
     * The display name is persisted to the Firebase Firestore user profile.
     */
    fun setDisplayNameAndContinue(displayName: String) {
        if (displayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                authRepository.updateUserProfile(
                    displayName = displayName.trim(),
                    bio = "",
                    avatarUrl = ""
                ).collect { result ->
                    when (result) {
                        is AuthResult.Loading -> { /* already loading */ }
                        is AuthResult.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, isProfileSetupNeeded = false, errorMessage = null)
                            }
                        }
                        is AuthResult.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = result.message)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update display name")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to save display name")
                }
            }
        }
    }

    /**
     * Updates the display name input field in the profile setup form.
     */
    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null) }
    }

    /**
     * Clears the credential state and signs the user out.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (_: Exception) {
                // Auth state flow will handle the transition to Unauthenticated
            }
        }
    }

    /**
     * Clears any error message displayed to the user.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
