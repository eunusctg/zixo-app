package com.zixo.app.ui.screens.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
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
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.zixo.app.domain.repository.AuthResult
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

// ════════════════════════════════════════════════════════════════
// Authentication UI State
// ════════════════════════════════════════════════════════════════

/**
 * Immutable state snapshot for the Zixo authentication screen.
 *
 * Drives every pixel on [AuthScreen] through a single source of truth.
 * No UI logic lives in the composable — all transitions, error banners,
 * and progressive-disclosure toggles flow from this object.
 *
 * @property isLoading              True while any async auth operation is in flight.
 * @property errorMessage           Human-readable error; null when idle.
 * @property isSignUpMode           Whether the user is in the "Create Account" flow.
 * @property displayName            Temporary display name during post-sign-up profile setup.
 * @property isProfileSetupNeeded   True when a new user must provide a display name.
 * @property googleIdToken          Transient Google ID token before backend verification.
 * @property email                  Current email input (email/password fallback).
 * @property password               Current password input (email/password fallback).
 * @property isEmailSignIn          Whether the user has toggled the email sign-in form open.
 * @property isEmailSignUp          Whether the email form is in sign-up mode vs. sign-in.
 * @property showEmailFallback      Auto-triggered when Google Sign-In is unavailable on the device.
 * @property isGmsAvailable         False on non-GMS devices (Huawei, emulators without GMS).
 * @property emailValidationError   Inline validation error for the email field; null when valid.
 * @property passwordValidationError Inline validation error for the password field; null when valid.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpMode: Boolean = false,
    val displayName: String = "",
    val isProfileSetupNeeded: Boolean = false,
    val googleIdToken: String? = null,
    val email: String = "",
    val password: String = "",
    val isEmailSignIn: Boolean = false,
    val isEmailSignUp: Boolean = false,
    val showEmailFallback: Boolean = false,
    val isGmsAvailable: Boolean = true,
    val emailValidationError: String? = null,
    val passwordValidationError: String? = null
)

// ════════════════════════════════════════════════════════════════
// Email Validation Regex
// ════════════════════════════════════════════════════════════════

/**
 * RFC-5322-ish email pattern used for client-side pre-flight checks.
 * We intentionally keep this slightly permissive — the Firebase backend
 * performs the authoritative validation, but this prevents obvious typos
 * from ever hitting the network socket.
 */
private val EMAIL_PATTERN = Pattern.compile(
    "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
)

// ════════════════════════════════════════════════════════════════
// AuthViewModel — Multi-Device Resilient Authentication
// ════════════════════════════════════════════════════════════════

/**
 * Production-grade ViewModel for the Zixo authentication pipeline.
 *
 * ## Architecture Principles
 *
 * 1. **Never crash.** Every external call (CredentialManager, Firebase, Cloudflare)
 *    is wrapped in a defensive try-catch. Missing Google Play Services, absent
 *    Google accounts, and network failures all degrade gracefully to the email
 *    fallback path — the user is never stuck on a broken screen.
 *
 * 2. **Validate before network.** Email syntax and password length checks run
 *    on [Dispatchers.IO] _before_ the credentials touch any network socket.
 *    This avoids wasted round-trips and gives instant inline feedback.
 *
 * 3. **Single state stream.** [uiState] and [authState] are the only two
 *    StateFlows the UI layer reads. No LiveData, no callback flags, no
 *    event buses.
 *
 * ## Authentication Pipelines
 *
 * **Google Sign-In (primary):**
 * 1. User taps "Continue with Google" → [signInWithGoogle]
 * 2. CredentialManager launches the system sheet (or browser fallback)
 * 3. Google ID token extracted → [authenticateWithBackend]
 * 4. Cloudflare verification (optional) → Firebase Auth → Firestore profile
 * 5. [authState] emits `Authenticated` → NavHost navigates to Home
 *
 * **Email/Password (fallback):**
 * 1. User taps "Login using email and password" → form slides open
 * 2. [validateEmailAsync] + [validatePasswordAsync] on Dispatchers.IO
 * 3. [signInWithEmail] or [signUpWithEmail] → Firebase Auth
 * 4. Firestore profile ensured → [authState] emits `Authenticated`
 *
 * **GMS-absent devices (Huawei, some emulators):**
 * - [checkGmsAvailability] runs on init
 * - If GMS is missing, `isGmsAvailable = false` + `showEmailFallback = true`
 * - The Google button is visually dimmed and the email form auto-expands
 * - An inline notice card explains the situation to the user
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Firebase Auth instance — lazy so it never initialises before Hilt is ready. */
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Observable authentication state from the repository.
     *
     * The navigation layer (ZixoNavHost / MainActivity) collects this as a
     * lifecycle-aware [StateFlow] using `.collectAsStateWithLifecycle()` and
     * triggers atomic navigation the instant it flips to [AuthState.Authenticated].
     */
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Unauthenticated)

    // ── GMS Availability Check ────────────────────────────────────────────

    /**
     * Probes whether Google Play Services are available and up-to-date.
     *
     * Must be called once from the UI layer (inside a `LaunchedEffect`)
     * because it needs an [Activity] context to call
     * `GoogleApiAvailability.isGooglePlayServicesAvailable()`.
     *
     * On failure, sets [AuthUiState.isGmsAvailable] to false and auto-triggers
     * the email fallback so the user is never stranded on a dead Google button.
     */
    fun checkGmsAvailability(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val availability = com.google.android.gms.common.GoogleApiAvailability
                    .getInstance()
                    .isGooglePlayServicesAvailable(activity)

                val isAvailable = availability == com.google.android.gms.common.ConnectionResult.SUCCESS

                _uiState.update { current ->
                    current.copy(
                        isGmsAvailable = isAvailable,
                        showEmailFallback = current.showEmailFallback || !isAvailable
                    )
                }

                if (!isAvailable) {
                    Timber.w("GMS unavailable (code=%d) — email fallback activated", availability)
                }
            } catch (e: Exception) {
                // Non-GMS devices (Huawei, some emulators) may not even have
                // GoogleApiAvailability on the classpath in extreme cases.
                Timber.w(e, "GMS check failed — assuming unavailable, email fallback activated")
                _uiState.update { current ->
                    current.copy(isGmsAvailable = false, showEmailFallback = true)
                }
            }
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────

    /**
     * Initiates Google Sign-In using Android CredentialManager.
     *
     * **Resilience strategy:**
     * - Wraps `CredentialManager.create()` in a try-catch; on non-GMS devices
     *   this call itself can throw — we intercept and fall back to email.
     * - Uses both [GetGoogleIdOption] (device accounts) and
     *   [GetSignInWithGoogleOption] (browser-based) so the flow works even
     *   when no Google account is registered on the device.
     * - Catches [NoCredentialException] → email fallback (no error message).
     * - Catches [GetCredentialCancellationException] → silent (user backed out).
     * - Catches any other [GetCredentialException] → email fallback with toast.
     * - Catches generic [Exception] → email fallback (defensive last resort).
     *
     * @param activity The host Activity required by CredentialManager's bottom sheet.
     */
    fun signInWithGoogle(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Create CredentialManager — can throw on non-GMS devices
                val credentialManager = safeCreateCredentialManager(activity)
                    ?: return@launch // fallback already triggered inside the helper

                // Step 2: Build the credential request with dual options
                val serverClientId =
                    "809372450511-lqm5bvb2m2us2av2qc2t6c0hva3gq5fm.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .build()

                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                // Step 3: Launch the system credential sheet
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                // Step 4: Extract the Google ID token
                handleCredentialResult(result)
            } catch (e: NoCredentialException) {
                // No Google accounts on the device — fall back to email silently
                Timber.w(e, "No Google accounts found on device — falling back to email sign-in")
                _uiState.update {
                    it.copy(isLoading = false, showEmailFallback = true, errorMessage = null)
                }
            } catch (e: GetCredentialCancellationException) {
                // User closed the bottom sheet — not an error, just go idle
                Timber.d("User cancelled Google Sign-In sheet")
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (e: GetCredentialException) {
                // Any other credential framework error — switch to email fallback
                Timber.e(e, "Google Sign-In credential request failed — falling back to email")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showEmailFallback = true,
                        errorMessage = "Google Sign-In unavailable. Please use email/password instead."
                    )
                }
            } catch (e: Exception) {
                // Defensive catch-all — the app must NEVER crash here
                Timber.e(e, "Unexpected error during Google Sign-In — falling back to email")
                _uiState.update {
                    it.copy(isLoading = false, showEmailFallback = true, errorMessage = null)
                }
            } finally {
                // Safety net: if somehow isLoading is still true after all paths
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
                }
            }
        }
    }

    /**
     * Safely creates a [CredentialManager] instance.
     *
     * On devices without Google Play Services, `CredentialManager.create()`
     * can throw. This helper catches that and triggers the email fallback
     * path instead of letting the exception propagate to crash the app.
     *
     * @return The CredentialManager, or null if creation failed (fallback triggered).
     */
    private fun safeCreateCredentialManager(activity: Activity): CredentialManager? {
        return try {
            CredentialManager.create(activity)
        } catch (e: Exception) {
            Timber.e(e, "CredentialManager.create() failed — device may lack GMS")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isGmsAvailable = false,
                    showEmailFallback = true,
                    errorMessage = null
                )
            }
            null
        }
    }

    /**
     * Processes the credential response from CredentialManager.
     *
     * Extracts the Google ID token from the credential and forwards it
     * to [authenticateWithBackend] for Cloudflare verification + Firebase Auth.
     * Any parsing failures trigger the email fallback path.
     */
    private fun handleCredentialResult(result: GetCredentialResponse) {
        val credential = result.credential

        when {
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    _uiState.update { it.copy(googleIdToken = idToken) }
                    authenticateWithBackend(idToken)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse Google ID token credential — falling back to email")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showEmailFallback = true,
                            errorMessage = "Could not read Google Sign-In data. Please use email."
                        )
                    }
                }
            }
            else -> {
                Timber.w("Unexpected credential type: %s — falling back to email", credential.type)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showEmailFallback = true,
                        errorMessage = "Unsupported sign-in method. Please use email/password."
                    )
                }
            }
        }
    }

    /**
     * Forwards the Google ID token to the backend for verification.
     *
     * The repository handles:
     * 1. Firebase Auth credential exchange (mandatory)
     * 2. Cloudflare verification (optional — mints username + Zixo Number)
     * 3. Firestore profile creation (best-effort)
     *
     * If Cloudflare is unreachable, the repository falls back to client-side
     * username/number generation and the auth still succeeds.
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
                            // Navigation is handled by the authState observer in ZixoNavHost
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

    // ── Email/Password Validation ─────────────────────────────────────────

    /**
     * Validates the current email string on [Dispatchers.IO].
     *
     * Rules:
     * - Must not be empty
     * - Must match [EMAIL_PATTERN]
     *
     * Sets [AuthUiState.emailValidationError] on failure, clears it on success.
     *
     * @return true if the email is syntactically valid.
     */
    private suspend fun validateEmailAsync(): Boolean {
        val email = _uiState.value.email.trim()

        val error = when {
            email.isEmpty() -> "Email address is required"
            !EMAIL_PATTERN.matcher(email).matches() -> "Please enter a valid email address"
            else -> null
        }

        _uiState.update { it.copy(emailValidationError = error) }
        return error == null
    }

    /**
     * Validates the current password string on [Dispatchers.IO].
     *
     * Rules:
     * - Must not be empty
     * - Sign-in: minimum 1 character (Firebase handles the actual check)
     * - Sign-up: minimum 8 characters
     *
     * Sets [AuthUiState.passwordValidationError] on failure, clears it on success.
     *
     * @return true if the password passes local validation.
     */
    private suspend fun validatePasswordAsync(): Boolean {
        val password = _uiState.value.password
        val isSignUp = _uiState.value.isEmailSignUp

        val error = when {
            password.isEmpty() -> "Password is required"
            isSignUp && password.length < 8 -> "Password must be at least 8 characters"
            isSignUp && password.length > 128 -> "Password is too long"
            else -> null
        }

        _uiState.update { it.copy(passwordValidationError = error) }
        return error == null
    }

    // ── Email/Password Sign-In ────────────────────────────────────────────

    /**
     * Signs in an existing user with email and password via Firebase Auth.
     *
     * **Pipeline:**
     * 1. Pre-flight validation on [Dispatchers.IO] (email format + password non-empty)
     * 2. `FirebaseAuth.signInWithEmailAndPassword()` on [Dispatchers.IO]
     * 3. [ensureFirestoreProfile] called on success
     * 4. Auth state observer in NavHost detects the Firebase session change
     *    and atomically navigates to Home
     *
     * Catches:
     * - [FirebaseAuthInvalidCredentialsException] → "Invalid email or password"
     * - Generic [Exception] → localized message or "Sign-in failed"
     */
    fun signInWithEmail() {
        viewModelScope.launch(Dispatchers.IO) {
            // Pre-flight validation — never hit the network with obviously bad input
            val emailValid = validateEmailAsync()
            val passwordValid = validatePasswordAsync()
            if (!emailValid || !passwordValid) return@launch

            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    ensureFirestoreProfile(firebaseUser.uid, firebaseUser.email, firebaseUser.displayName)
                    Timber.d("Email sign-in successful for: %s", email.replaceBefore('@', "***"))
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    // Auth state observer in ZixoNavHost will detect the Firebase
                    // auth change and navigate to Home automatically
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
     * Creates a new Firebase Auth account with email and password.
     *
     * **Pipeline:**
     * 1. Pre-flight validation on [Dispatchers.IO] (email format + password >= 8 chars)
     * 2. `FirebaseAuth.createUserWithEmailAndPassword()` on [Dispatchers.IO]
     * 3. [ensureFirestoreProfile] called on success
     * 4. Sets [AuthUiState.isProfileSetupNeeded] to true for display name entry
     * 5. Auth state observer in NavHost detects the new session and navigates
     *
     * Catches:
     * - [FirebaseAuthUserCollisionException] → "Email already registered"
     * - [FirebaseAuthInvalidCredentialsException] → "Invalid email format"
     * - [FirebaseAuthWeakPasswordException] → "Password is too weak"
     * - Generic [Exception] → localized message or "Account creation failed"
     */
    fun signUpWithEmail() {
        viewModelScope.launch(Dispatchers.IO) {
            // Pre-flight validation
            val emailValid = validateEmailAsync()
            val passwordValid = validatePasswordAsync()
            if (!emailValid || !passwordValid) return@launch

            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
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
                        it.copy(isLoading = false, errorMessage = "Account creation failed — no user returned")
                    }
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                Timber.w(e, "Email already in use")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "This email is already registered. Try signing in instead."
                    )
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Timber.w(e, "Invalid email format")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Invalid email address format")
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                Timber.w(e, "Weak password")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Password is too weak. Use at least 8 characters with a mix of letters and numbers.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Email sign-up failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Account creation failed")
                }
            } finally {
                _uiState.update { current ->
                    if (current.isLoading) current.copy(isLoading = false) else current
                }
            }
        }
    }

    // ── Firestore Profile Helper ──────────────────────────────────────────

    /**
     * Ensures a Firestore user profile document exists after Firebase Auth succeeds.
     *
     * This is critical because [authRepository.observeAuthState] needs a profile
     * to emit [AuthState.Authenticated]. If the profile already exists, this is
     * a no-op. If Firestore is temporarily unreachable, the auth still proceeds —
     * the observer falls back to constructing a minimal [User] from Firebase data.
     *
     * @param uid         The Firebase Auth UID.
     * @param email       The user's email (nullable for phone-auth users).
     * @param displayName The display name (may be empty for new email sign-ups).
     */
    private suspend fun ensureFirestoreProfile(uid: String, email: String?, displayName: String?) {
        try {
            authRepository.getCurrentUser().collect { existingUser ->
                if (existingUser == null) {
                    Timber.d("Creating Firestore profile for new email auth user: %s", uid)
                    try {
                        authRepository.updateUserProfile(
                            displayName = displayName ?: "",
                            bio = "",
                            avatarUrl = ""
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to create Firestore profile via updateUserProfile")
                    }
                }
                return@collect // Only need the first emission
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to ensure Firestore profile — auth will still proceed via observeAuthState fallback")
        }
    }

    // ── State Mutations ───────────────────────────────────────────────────

    /** Updates the email field and clears any inline validation error. */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailValidationError = null, errorMessage = null) }
    }

    /** Updates the password field and clears any inline validation error. */
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordValidationError = null, errorMessage = null) }
    }

    /** Toggles between sign-in and sign-up mode within the email form. */
    fun toggleEmailSignUpMode() {
        _uiState.update {
            it.copy(isEmailSignUp = !it.isEmailSignUp, errorMessage = null,
                emailValidationError = null, passwordValidationError = null)
        }
    }

    /** Sets whether the email sign-in form is visible. */
    fun setEmailSignIn(isEmailSignIn: Boolean) {
        _uiState.update { it.copy(isEmailSignIn = isEmailSignIn, errorMessage = null) }
    }

    /** Shows the email fallback form (called when Google Sign-In is unavailable). */
    fun showEmailFallback() {
        _uiState.update { it.copy(showEmailFallback = true, errorMessage = null) }
    }

    /** Hides the email fallback form (user explicitly closed it). */
    fun clearEmailFallback() {
        _uiState.update { it.copy(showEmailFallback = false, errorMessage = null) }
    }

    /** Clears the top-level error message. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Profile Setup ─────────────────────────────────────────────────────

    /** Updates the temporary display name field during profile setup. */
    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null) }
    }

    /**
     * Saves the display name to Firestore after a new user signs up.
     *
     * After successful save, clears [AuthUiState.isProfileSetupNeeded]
     * so the profile setup dialog dismisses.
     */
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

    // ── Sign Out ──────────────────────────────────────────────────────────

    /** Signs out the current user and clears all cached state. */
    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                Timber.e(e, "Sign-out failed")
            }
        }
    }
}
