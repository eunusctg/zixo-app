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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.zixo.app.domain.repository.AuthResult
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

// ════════════════════════════════════════════════════════════════
// Authentication UI State — Single source of truth
// ════════════════════════════════════════════════════════════════

/**
 * Immutable state snapshot for the Zixo authentication screen.
 *
 * Every pixel on [AuthScreen] is driven by this single object. No UI
 * logic lives in the composable; all transitions, error banners, and
 * progressive-disclosure toggles flow from this object.
 *
 * @property email               Current email input.
 * @property password            Current password input.
 * @property displayName         Display name input (sign-up mode only).
 * @property isSignUpMode        True = create-account form, False = sign-in form.
 * @property isEmailFormVisible  Whether the email/password form is expanded.
 * @property isGmsAvailable      False on non-GMS devices (Huawei, some emulators).
 * @property showGmsNotice       True when Google Sign-In is unavailable and a
 *                               user-facing notice should be displayed.
 * @property isGoogleLoading     True while a Google Sign-In operation is in flight.
 * @property isEmailLoading      True while an email/password operation is in flight.
 * @property emailError          Inline validation error for the email field; null when valid.
 * @property passwordError       Inline validation error for the password field; null when valid.
 * @property displayNameError    Inline validation error for the display name field; null when valid.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isSignUpMode: Boolean = false,
    val isEmailFormVisible: Boolean = false,
    val isGmsAvailable: Boolean = true,
    val showGmsNotice: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isEmailLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
)

/**
 * One-shot UI events emitted to the AuthScreen.
 *
 * Unlike [AuthUiState], these are consumed exactly once via a
 * [Channel] — perfect for transient messages that should not
 * re-trigger on recomposition or screen rotation.
 */
sealed interface AuthUiEvent {
    /** Show a transient snackbar with an error message. */
    data class ShowError(val message: String) : AuthUiEvent
    /** Show a transient snackbar with an informational message. */
    data class ShowInfo(val message: String) : AuthUiEvent
}

// ════════════════════════════════════════════════════════════════
// Email Validation Regex
// ════════════════════════════════════════════════════════════════

/**
 * RFC-5322-ish email pattern used for client-side pre-flight checks.
 * Slightly permissive — Firebase backend performs the authoritative
 * validation — but this prevents obvious typos from hitting the network.
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
 * 1. **Never crash.** Every external call (CredentialManager, Firebase)
 *    is wrapped in a defensive try-catch. Missing Google Play Services,
 *    absent Google accounts, and network failures all degrade gracefully
 *    to the email fallback path — the user is never stuck.
 *
 * 2. **Validate before network.** Email syntax and password length checks
 *    run _before_ the credentials touch any network socket. This avoids
 *    wasted round-trips and gives instant inline feedback.
 *
 * 3. **Single source of truth.** [uiState] is the only state the UI reads.
 *    Transient messages flow through [events] as one-shot [AuthUiEvent]s,
 *    consumed via a [Channel] so they survive recomposition safely.
 *
 * 4. **Decouple loading flags.** [AuthUiState.isGoogleLoading] and
 *    [AuthUiState.isEmailLoading] are independent — the Google button and
 *    email button can show their own spinners without interfering.
 *
 * 5. **No race conditions.** After sign-up succeeds, Firebase auth state
 *    changes and the navigation observer in `ZixoNavHost` navigates to
 *    Home automatically. We do NOT set `isProfileSetupNeeded` after
 *    sign-up — the display name is collected *inline* in the sign-up form
 *    and saved before the Firebase auth state change fires. This removes
 *    the previous race where the profile-setup dialog appeared briefly
 *    before the user was navigated away.
 *
 * ## Authentication Pipelines
 *
 * **Google Sign-In (primary):**
 * 1. User taps "Continue with Google" → [signInWithGoogle]
 * 2. CredentialManager launches the system sheet (or browser fallback)
 * 3. Google ID token extracted → [authenticateWithBackend]
 * 4. Firebase Auth credential exchange → Firestore profile ensured
 * 5. [authState] emits `Authenticated` → NavHost navigates to Home
 *
 * **Email/Password (fallback):**
 * 1. User taps "Login using email and password" → form slides open
 * 2. Inline validation (email format + password length)
 * 3. [signInWithEmail] or [signUpWithEmail] → Firebase Auth
 * 4. (Sign-up only) Display name saved to Firestore profile
 * 5. [authState] emits `Authenticated` → NavHost navigates to Home
 *
 * **GMS-absent devices (Huawei, some emulators):**
 * - [checkGmsAvailability] runs on init
 * - If GMS is missing: `isGmsAvailable = false`, `showGmsNotice = true`,
 *   and `isEmailFormVisible = true` (form auto-expands)
 * - The Google button is disabled with an inline notice card
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Backed by a [Channel] with `Buffered` capacity so events are not
     * dropped if the UI is briefly off-screen when they fire.
     */
    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Firebase Auth instance — lazy so it never initialises before Hilt is ready. */
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Observable authentication state from the repository.
     *
     * The navigation layer (`ZixoNavHost`) collects this as a lifecycle-aware
     * [StateFlow] using `.collectAsStateWithLifecycle()` and triggers atomic
     * navigation the instant it flips to [AuthState.Authenticated].
     */
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    // ── GMS Availability Check ────────────────────────────────────────────

    /**
     * Probes whether Google Play Services are available and up-to-date.
     *
     * Must be called once from the UI layer (inside a `LaunchedEffect`)
     * because it needs an [Activity] context to call
     * `GoogleApiAvailability.isGooglePlayServicesAvailable()`.
     *
     * On failure: sets [AuthUiState.isGmsAvailable] to false, shows the
     * notice, and auto-expands the email form so the user is never
     * stranded on a dead Google button.
     */
    fun checkGmsAvailability(activity: Activity) {
        viewModelScope.launch {
            try {
                val availability = com.google.android.gms.common.GoogleApiAvailability
                    .getInstance()
                    .isGooglePlayServicesAvailable(activity)

                val isAvailable =
                    availability == com.google.android.gms.common.ConnectionResult.SUCCESS

                _uiState.update { current ->
                    current.copy(
                        isGmsAvailable = isAvailable,
                        showGmsNotice = !isAvailable,
                        isEmailFormVisible = current.isEmailFormVisible || !isAvailable
                    )
                }

                if (!isAvailable) {
                    Timber.w("GMS unavailable (code=%d) — email fallback activated", availability)
                }
            } catch (e: Exception) {
                // Non-GMS devices (Huawei, some emulators) may not even have
                // GoogleApiAvailability on the classpath in extreme cases.
                Timber.w(e, "GMS check failed — assuming unavailable, email fallback activated")
                _uiState.update {
                    it.copy(
                        isGmsAvailable = false,
                        showGmsNotice = true,
                        isEmailFormVisible = true
                    )
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
     * - Catches [NoCredentialException] → email fallback silently.
     * - Catches [GetCredentialCancellationException] → silent (user backed out).
     * - Catches any other [GetCredentialException] → email fallback + error.
     * - Catches generic [Exception] → email fallback (defensive last resort).
     *
     * @param activity The host Activity required by CredentialManager's bottom sheet.
     */
    fun signInWithGoogle(activity: Activity) {
        _uiState.update { it.copy(isGoogleLoading = true) }

        viewModelScope.launch {
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

                // Step 4: Extract the Google ID token and authenticate with backend
                handleCredentialResult(result)
            } catch (e: NoCredentialException) {
                // No Google accounts on the device — fall back to email silently
                Timber.w(e, "No Google accounts on device — falling back to email")
                _uiState.update {
                    it.copy(
                        isGoogleLoading = false,
                        isEmailFormVisible = true,
                        showGmsNotice = true
                    )
                }
                _events.send(AuthUiEvent.ShowInfo("No Google account found. Please use email instead."))
            } catch (e: GetCredentialCancellationException) {
                // User closed the bottom sheet — not an error, just go idle
                Timber.d("User cancelled Google Sign-In sheet")
                _uiState.update { it.copy(isGoogleLoading = false) }
            } catch (e: GetCredentialException) {
                // Any other credential framework error — switch to email fallback
                Timber.e(e, "Google Sign-In credential error — falling back to email")
                _uiState.update {
                    it.copy(
                        isGoogleLoading = false,
                        isEmailFormVisible = true,
                        showGmsNotice = true
                    )
                }
                _events.send(AuthUiEvent.ShowError("Google Sign-In unavailable. Please use email."))
            } catch (e: Exception) {
                // Defensive catch-all — the app must NEVER crash here
                Timber.e(e, "Unexpected error during Google Sign-In — falling back to email")
                _uiState.update {
                    it.copy(
                        isGoogleLoading = false,
                        isEmailFormVisible = true,
                        showGmsNotice = true
                    )
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
                    isGoogleLoading = false,
                    isGmsAvailable = false,
                    showGmsNotice = true,
                    isEmailFormVisible = true
                )
            }
            null
        }
    }

    /**
     * Processes the credential response from CredentialManager.
     *
     * Extracts the Google ID token from the credential and forwards it
     * to [authenticateWithBackend] for Firebase Auth + Firestore profile.
     * Any parsing failures trigger the email fallback path.
     */
    private suspend fun handleCredentialResult(result: GetCredentialResponse) {
        val credential = result.credential

        when {
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    authenticateWithBackend(idToken)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse Google ID token — falling back to email")
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            isEmailFormVisible = true,
                            showGmsNotice = true
                        )
                    }
                    _events.send(AuthUiEvent.ShowError("Could not read Google Sign-In data. Please use email."))
                }
            }
            else -> {
                Timber.w("Unexpected credential type: %s — falling back to email", credential.type)
                _uiState.update {
                    it.copy(
                        isGoogleLoading = false,
                        isEmailFormVisible = true,
                        showGmsNotice = true
                    )
                }
                _events.send(AuthUiEvent.ShowError("Unsupported sign-in method. Please use email."))
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
        viewModelScope.launch {
            try {
                authRepository.signInWithGoogle(idToken).collect { result ->
                    when (result) {
                        is AuthResult.Loading -> {
                            _uiState.update { it.copy(isGoogleLoading = true) }
                        }
                        is AuthResult.Success -> {
                            _uiState.update {
                                it.copy(isGoogleLoading = false, showGmsNotice = false)
                            }
                            // Navigation handled by authState observer in ZixoNavHost
                        }
                        is AuthResult.Error -> {
                            _uiState.update { it.copy(isGoogleLoading = false) }
                            _events.send(AuthUiEvent.ShowError(result.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Backend authentication failed")
                _uiState.update { it.copy(isGoogleLoading = false) }
                _events.send(AuthUiEvent.ShowError("Authentication failed. Please try again."))
            }
        }
    }

    // ── Email/Password Validation ─────────────────────────────────────────

    /**
     * Validates the current email string. Sets [AuthUiState.emailError]
     * on failure, clears it on success.
     *
     * @return true if the email is syntactically valid.
     */
    private fun validateEmail(): Boolean {
        val email = _uiState.value.email.trim()
        val error = when {
            email.isEmpty() -> "Email address is required"
            !EMAIL_PATTERN.matcher(email).matches() -> "Please enter a valid email address"
            else -> null
        }
        _uiState.update { it.copy(emailError = error) }
        return error == null
    }

    /**
     * Validates the current password string. Sets [AuthUiState.passwordError]
     * on failure, clears it on success.
     *
     * Sign-in: password must be non-empty (Firebase handles the actual check).
     * Sign-up: password must be at least 8 characters.
     *
     * @return true if the password passes local validation.
     */
    private fun validatePassword(): Boolean {
        val password = _uiState.value.password
        val isSignUp = _uiState.value.isSignUpMode
        val error = when {
            password.isEmpty() -> "Password is required"
            isSignUp && password.length < 8 -> "Password must be at least 8 characters"
            isSignUp && password.length > 128 -> "Password is too long"
            else -> null
        }
        _uiState.update { it.copy(passwordError = error) }
        return error == null
    }

    /**
     * Validates the display name (sign-up mode only). Sets
     * [AuthUiState.displayNameError] on failure, clears it on success.
     *
     * @return true if the display name is valid.
     */
    private fun validateDisplayName(): Boolean {
        val name = _uiState.value.displayName.trim()
        val error = when {
            name.isEmpty() -> "Display name is required"
            name.length > 32 -> "Display name must be 32 characters or fewer"
            else -> null
        }
        _uiState.update { it.copy(displayNameError = error) }
        return error == null
    }

    // ── Email/Password Sign-In ────────────────────────────────────────────

    /**
     * Signs in an existing user with email and password via Firebase Auth.
     *
     * **Pipeline:**
     * 1. Pre-flight validation (email format + password non-empty)
     * 2. `FirebaseAuth.signInWithEmailAndPassword()`
     * 3. Auth state observer in `ZixoNavHost` detects the Firebase session
     *    change and atomically navigates to Home
     *
     * Catches:
     * - [FirebaseAuthInvalidUserException] → "No account found with this email"
     * - [FirebaseAuthInvalidCredentialsException] → "Invalid email or password"
     * - Generic [Exception] → localized message or "Sign-in failed"
     */
    fun signInWithEmail() {
        // Pre-flight validation — never hit the network with obviously bad input
        val emailOk = validateEmail()
        val passwordOk = validatePassword()
        if (!emailOk || !passwordOk) return

        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        _uiState.update { it.copy(isEmailLoading = true) }

        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                Timber.d("Email sign-in successful for: %s", email.replaceBefore('@', "***"))
                _uiState.update { it.copy(isEmailLoading = false) }
                // Auth state observer in ZixoNavHost will detect the Firebase
                // auth change and navigate to Home automatically
            } catch (e: FirebaseAuthInvalidUserException) {
                Timber.w(e, "No account found for this email")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError("No account found with this email. Please sign up."))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Timber.w(e, "Invalid email/password credentials")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError("Invalid email or password. Please try again."))
            } catch (e: Exception) {
                Timber.e(e, "Email sign-in failed")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError(e.localizedMessage ?: "Sign-in failed. Please try again."))
            }
        }
    }

    /**
     * Creates a new Firebase Auth account with email and password.
     *
     * **Pipeline:**
     * 1. Pre-flight validation (email + password ≥ 8 chars + display name)
     * 2. `FirebaseAuth.createUserWithEmailAndPassword()`
     * 3. Update Firebase user profile with the display name
     * 4. Save display name to Firestore profile via [AuthRepository.updateUserProfile]
     * 5. Auth state observer in `ZixoNavHost` detects the new session and
     *    navigates to Home
     *
     * **Why this design avoids the previous race condition:**
     * The display name is collected *inline* in the sign-up form (not in a
     * separate dialog that appears after sign-up). By the time Firebase
     * emits the auth state change, the user profile is already complete.
     * There is no profile-setup dialog to dismiss and no race with the
     * navigation observer.
     *
     * Catches:
     * - [FirebaseAuthUserCollisionException] → "Email already registered"
     * - [FirebaseAuthInvalidCredentialsException] → "Invalid email format"
     * - [FirebaseAuthWeakPasswordException] → "Password is too weak"
     * - Generic [Exception] → localized message or "Account creation failed"
     */
    fun signUpWithEmail() {
        // Pre-flight validation
        val emailOk = validateEmail()
        val passwordOk = validatePassword()
        val nameOk = validateDisplayName()
        if (!emailOk || !passwordOk || !nameOk) return

        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val displayName = _uiState.value.displayName.trim()

        _uiState.update { it.copy(isEmailLoading = true) }

        viewModelScope.launch {
            try {
                // Step 1: Create the Firebase Auth user
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                    ?: throw IllegalStateException("Account creation succeeded but user is null")

                // Step 2: Update the Firebase user profile with the display name
                // (best-effort — Firestore is the source of truth for app state)
                try {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to update Firebase profile display name (non-fatal)")
                }

                // Step 3: Save the display name to Firestore via the repository
                // (the repository's observeAuthState will create the full profile)
                try {
                    authRepository.updateUserProfile(
                        displayName = displayName,
                        bio = "",
                        avatarUrl = ""
                    ).collect { result ->
                        if (result is AuthResult.Error) {
                            Timber.w("Profile save warning: %s", result.message)
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to save display name to Firestore (non-fatal)")
                }

                Timber.d("Email sign-up successful for: %s", email.replaceBefore('@', "***"))
                _uiState.update { it.copy(isEmailLoading = false) }
                // Auth state observer in ZixoNavHost will detect the Firebase
                // auth change and navigate to Home automatically
            } catch (e: FirebaseAuthUserCollisionException) {
                Timber.w(e, "Email already in use")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError("This email is already registered. Try signing in instead."))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Timber.w(e, "Invalid email format")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError("Invalid email address format."))
            } catch (e: FirebaseAuthWeakPasswordException) {
                Timber.w(e, "Weak password")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError("Password is too weak. Use at least 8 characters with a mix of letters and numbers."))
            } catch (e: Exception) {
                Timber.e(e, "Email sign-up failed")
                _uiState.update { it.copy(isEmailLoading = false) }
                _events.send(AuthUiEvent.ShowError(e.localizedMessage ?: "Account creation failed. Please try again."))
            }
        }
    }

    // ── State Mutations ───────────────────────────────────────────────────

    /** Updates the email field and clears any inline validation error. */
    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, emailError = null)
        }
    }

    /** Updates the password field and clears any inline validation error. */
    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null)
        }
    }

    /** Updates the display name field and clears any inline validation error. */
    fun onDisplayNameChange(name: String) {
        _uiState.update {
            it.copy(displayName = name, displayNameError = null)
        }
    }

    /**
     * Toggles between sign-in and sign-up mode within the email form.
     *
     * Clears the password (for security) and all validation errors when
     * switching modes. The email and display name are preserved so the
     * user doesn't lose what they typed.
     */
    fun toggleSignUpMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                password = "",
                emailError = null,
                passwordError = null,
                displayNameError = null
            )
        }
    }

    /** Toggles whether the email/password form is expanded. */
    fun toggleEmailForm() {
        _uiState.update { it.copy(isEmailFormVisible = !it.isEmailFormVisible) }
    }

    /** Hides the GMS unavailability notice after the user dismisses it. */
    fun dismissGmsNotice() {
        _uiState.update { it.copy(showGmsNotice = false) }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────

    /** Signs out the current user and clears all cached state. */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut().collect { /* consume */ }
            } catch (e: Exception) {
                Timber.e(e, "Sign-out failed")
            }
        }
    }
}
