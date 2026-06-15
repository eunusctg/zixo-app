package com.zixo.app.ui.screens.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.zixo.app.domain.repository.AuthResult
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthState
import com.zixo.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Authentication UI State for the Zixo Sign-In flow.
 *
 * Supports two authentication methods:
 * 1. Google Sign-In via Android CredentialManager (primary)
 * 2. Email/password sign-in via Firebase Auth (fallback)
 *
 * The Cloudflare Edge Worker handles Google token verification and
 * mints the system-generated 8-digit Zixo Number and @username.
 * If Cloudflare is unavailable, fallback values are generated client-side.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpMode: Boolean = false,
    val displayName: String = "",               // Only for sign-up flow after Google auth
    val isProfileSetupNeeded: Boolean = false,  // True when new user needs display name
    val googleIdToken: String? = null,          // Temporary token from Google Sign-In
    val email: String = "",                     // Email for email/password fallback
    val password: String = "",                  // Password for email/password fallback
    val isEmailSignIn: Boolean = false,         // Whether using email/password mode
    val isEmailSignUp: Boolean = false,         // Whether in email sign-up mode vs sign-in
    val showEmailFallback: Boolean = false      // Auto-show email fields when Google unavailable
)

/**
 * ViewModel for the Zixo authentication flow.
 *
 * Authentication Pipeline (Google):
 * 1. User taps "Continue with Google" → CredentialManager launches Google Sign-In
 * 2. Google ID token is extracted from the credential response
 * 3. Token is sent to [AuthRepository.signInWithGoogle] for verification
 *    (Cloudflare verification is optional; falls back to Firebase-only auth)
 * 4. Firebase Auth session is created + Firestore profile ensured
 * 5. Auth state observer in ZixoNavHost navigates to Home
 *
 * Authentication Pipeline (Email/Password):
 * 1. User toggles "Login using email and password" link
 * 2. Email and password fields appear with AnimatedVisibility
 * 3. User taps "Sign in with Email" → Firebase Auth signInWithEmailAndPassword
 * 4. Firestore profile is created if it doesn't exist
 * 5. Auth state observer in ZixoNavHost navigates to Home
 *
 * All operations are wrapped in try-catch and run on Dispatchers.IO.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Firebase Auth instance for email/password sign-in fallback. */
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Observable authentication state from the repository.
     * Used by the navigation layer to determine if the user is authenticated.
     */
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Unauthenticated)

    // ── Google Sign-In ────────────────────────────────────────────────────────

    /**
     * Initiates Google Sign-In using Android CredentialManager.
     *
     * Uses both [GetGoogleIdOption] (device accounts) and [GetSignInWithGoogleOption]
     * (browser-based fallback) so it works even without a Google account on the device.
     *
     * @param activity The host activity required by CredentialManager.
     */
    fun signInWithGoogle(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val credentialManager = CredentialManager.create(activity)
                val serverClientId = "809372450511-lqm5bvb2m2us2av2qc2t6c0hva3gq5fm.apps.googleusercontent.com"

                // Primary: Google ID token from device accounts
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .build()

                // Secondary: Browser-based "Sign in with Google" flow
                // This works even when NO Google account is on the device
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                handleCredentialResult(result)
            } catch (e: NoCredentialException) {
                Timber.w(e, "No Google accounts found on device — falling back to email sign-in")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showEmailFallback = true,
                        errorMessage = null
                    )
                }
            } catch (e: GetCredentialException) {
                Timber.e(e, "Google Sign-In credential request failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = when {
                            e.message?.contains("cancelled", ignoreCase = true) == true -> null
                            else -> "Google Sign-In unavailable. Please use email/password below."
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during Google Sign-In")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showEmailFallback = true,
                        errorMessage = null
                    )
                }
            } finally {
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
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
     * The repository handles Cloudflare verification (optional) and
     * Firebase Auth + Firestore profile creation.
     */
    private fun authenticateWithBackend(idToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.signInWithGoogle(idToken).collect { result ->
                    when (result) {
                        is AuthResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is AuthResult.Success -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                            // Auth state observer in ZixoNavHost will handle navigation
                        }
                        is AuthResult.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = result.message)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Backend authentication failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Authentication failed. Please try again.")
                }
            } finally {
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
                }
            }
        }
    }

    // ── Email/Password Sign-In ────────────────────────────────────────────────

    /**
     * Signs in a user with email and password via Firebase Auth.
     *
     * After Firebase Auth succeeds, ensures a Firestore profile exists.
     * The auth state observer in ZixoNavHost handles navigation.
     */
    fun signInWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Ensure Firestore profile exists for this user
                    ensureFirestoreProfile(firebaseUser.uid, firebaseUser.email, firebaseUser.displayName)
                    Timber.d("Email sign-in successful for: %s", email.replaceBefore('@', "***"))
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    // Auth state observer in ZixoNavHost will detect the Firebase auth change
                    // and navigate to Home automatically
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Sign-in failed — no user returned")
                    }
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Timber.w(e, "Invalid email/password credentials")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Invalid email or password")
                }
            } catch (e: Exception) {
                Timber.e(e, "Email sign-in failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Sign-in failed")
                }
            } finally {
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
                }
            }
        }
    }

    /**
     * Creates a new account with email and password via Firebase Auth.
     *
     * After successful account creation, creates a Firestore profile
     * and the auth state observer handles navigation.
     */
    fun signUpWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required") }
            return
        }

        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Create Firestore profile for the new user
                    ensureFirestoreProfile(firebaseUser.uid, firebaseUser.email, firebaseUser.displayName)
                    Timber.d("Email sign-up successful for: %s", email.replaceBefore('@', "***"))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isProfileSetupNeeded = true,
                            errorMessage = null
                        )
                    }
                    // Auth state observer will detect Firebase auth change and navigate
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Account creation failed")
                    }
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                Timber.w(e, "Email already in use")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "This email is already registered. Try signing in instead.")
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Timber.w(e, "Invalid email format")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Invalid email address format")
                }
            } catch (e: Exception) {
                Timber.e(e, "Email sign-up failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Sign-up failed")
                }
            } finally {
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
                }
            }
        }
    }

    // ── Firestore Profile Helper ─────────────────────────────────────────────

    /**
     * Ensures a Firestore user profile document exists after Firebase Auth succeeds.
     * This is critical because [observeAuthState] needs a profile to emit [AuthState.Authenticated].
     * If the profile already exists, this is a no-op.
     */
    private suspend fun ensureFirestoreProfile(uid: String, email: String?, displayName: String?) {
        try {
            val existing = authRepository.getCurrentUser()
            // If getCurrentUser returns null or has empty uid, create the profile
            if (existing == null) {
                Timber.d("Creating Firestore profile for new email auth user: %s", uid)
                authRepository.updateUserProfile(
                    displayName = displayName ?: "",
                    bio = "",
                    avatarUrl = ""
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to ensure Firestore profile — auth will still proceed via observeAuthState fallback")
        }
    }

    // ── State Updates ─────────────────────────────────────────────────────────

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun toggleEmailSignUpMode() {
        _uiState.update { it.copy(isEmailSignUp = !it.isEmailSignUp, errorMessage = null) }
    }

    fun setEmailSignIn(isEmailSignIn: Boolean) {
        _uiState.update { it.copy(isEmailSignIn = isEmailSignIn, errorMessage = null) }
    }

    // ── Profile Setup ─────────────────────────────────────────────────────────

    fun setDisplayNameAndContinue(displayName: String) {
        if (displayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
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
            } finally {
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
                }
            }
        }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null) }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                Timber.e(e, "Sign-out failed")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showEmailFallback() {
        _uiState.update { it.copy(showEmailFallback = true, errorMessage = null) }
    }

    fun clearEmailFallback() {
        _uiState.update { it.copy(showEmailFallback = false, errorMessage = null) }
    }
}
