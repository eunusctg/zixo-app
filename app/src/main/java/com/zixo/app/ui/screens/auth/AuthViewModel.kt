package com.zixo.app.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginMode: Boolean = true,
    val isSignUpSuccess: Boolean = false,
    val isPasswordResetSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
        private const val GOOGLE_WEB_CLIENT_ID = "809372450511-7lpbcas0a6nrntljvs5sf4v9nnusrpol.apps.googleusercontent.com"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<ZixoUser?> = authRepository.currentUser

    fun onEmailChange(email: String) { _uiState.value = _uiState.value.copy(email = email, error = null) }
    fun onPasswordChange(password: String) { _uiState.value = _uiState.value.copy(password = password, error = null) }
    fun onDisplayNameChange(name: String) { _uiState.value = _uiState.value.copy(displayName = name, error = null) }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            error = null,
            isPasswordResetSent = false
        )
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank()) { _uiState.value = state.copy(error = "Email is required"); return }
        if (state.password.isBlank()) { _uiState.value = state.copy(error = "Password is required"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signIn(state.email, state.password)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.displayName.isBlank()) { _uiState.value = state.copy(error = "Display name is required"); return }
        if (state.email.isBlank()) { _uiState.value = state.copy(error = "Email is required"); return }
        if (state.password.length < 6) { _uiState.value = state.copy(error = "Password must be at least 6 characters"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signUp(state.email, state.password, state.displayName)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message,
                isSignUpSuccess = result.isSuccess
            )
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                // Extract Google ID token using the proper API
                val googleIdToken = when (val credential = result.credential) {
                    is GoogleIdTokenCredential -> credential.idToken
                    else -> {
                        // Fallback: try extracting from custom data
                        try {
                            credential.data.getString("com.google.android.libraries.identity.googleid.BEARER_ID_TOKEN")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to extract Google ID token", e)
                            null
                        }
                    }
                }

                if (googleIdToken.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to get Google ID token. Please try again."
                    )
                    return@launch
                }

                val authResult = authRepository.signInWithGoogle(googleIdToken)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = authResult.exceptionOrNull()?.message
                )
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google sign-in exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google sign-in was cancelled or failed"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Google sign-in failed"
                )
            }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.email.isBlank()) { _uiState.value = state.copy(error = "Enter your email to reset password"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.resetPassword(state.email)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message,
                isPasswordResetSent = result.isSuccess
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
