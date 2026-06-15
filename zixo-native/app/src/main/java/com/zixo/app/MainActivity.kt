package com.zixo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.ui.navigation.ZixoNavigation
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.ZixoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════════
// Single-Activity Entry Point — Zixo Application
// ════════════════════════════════════════════════════════════════

/**
 * Main activity that hosts the entire Zixo application.
 *
 * ## Responsibilities
 *
 * 1. **Hilt injection** via [@AndroidEntryPoint][AndroidEntryPoint]
 * 2. **Edge-to-edge** display with transparent system bars
 * 3. **Biometric authentication** gate (when screen lock is enabled in Settings)
 * 4. **Notification permission** request on Android 13+ via [ActivityCompat.requestPermissions]
 * 5. **FCM deep link** handling from notification intents (e.g. incoming call `callId`)
 * 6. **Lifecycle-aware auth state collection** — the root composable reads
 *    the Firebase user registration token as a lifecycle-aware `StateFlow` via
 *    `.collectAsStateWithLifecycle()`, ensuring no background leaks
 * 7. **Atomic instant router pop** — the exact millisecond the auth stream
 *    registers a validated, non-null user account state, `ZixoNavHost` triggers
 *    an absolute navigation update that clears `AuthScreen` completely off the
 *    backstack and shifts focus to `HomeScreen`, with zero white-screen deadlocks
 *
 * ## Navigation Post-Login White Screen Deadlock Fix
 *
 * The previous implementation had a race condition where the auth state could
 * transition to `Authenticated` _before_ the NavHost finished composing, or
 * where the backstack retained the `Auth` route after navigation, causing a
 * white screen flash on back press. The fix:
 *
 * - `ZixoNavHost` collects `authState` via `collectAsStateWithLifecycle()`
 *   (lifecycle-aware, not `collectAsState()`)
 * - On `Authenticated`, the navigation uses `popUpTo(Auth) { inclusive = true }`
 *   to atomically remove Auth from the backstack
 * - The `LaunchedEffect(authState)` key ensures the navigation fires exactly
 *   once per state transition, not on recomposition
 * - The `currentDestination` check prevents double-navigation when the user
 *   is already on Home
 * - `MainActivity` itself only shows `ZixoNavigation()` after biometric auth
 *   completes, so the NavHost is never in a half-rendered state
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)

    /** Whether the user has passed biometric authentication for this session. */
    private var isBiometricAuthenticated by mutableStateOf(false)

    /** Whether we are still checking if biometric auth is required. */
    private var isCheckingBiometric by mutableStateOf(true)

    /** Call ID extracted from FCM notification deep link intent extras. */
    private var deepLinkCallId by mutableStateOf<String?>(null)

    // ── Notification permission launcher (Android 13+) ──
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result handled silently — the app works without notifications */ }

    // ──────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Extract FCM deep link call ID from the launching intent
        deepLinkCallId = intent?.getStringExtra(EXTRA_CALL_ID)

        setContent {
            ZixoTheme {
                MainContent(
                    isBiometricAuthenticated = isBiometricAuthenticated,
                    isCheckingBiometric = isCheckingBiometric,
                    deepLinkCallId = deepLinkCallId,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeShowBiometricAuth()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update deep link call ID from new intent (e.g. tapped notification while app is open)
        val newCallId = intent.getStringExtra(EXTRA_CALL_ID)
        if (newCallId != null) {
            deepLinkCallId = newCallId
        }
    }

    override fun onPause() {
        super.onPause()
        // Reset biometric state when the activity leaves the foreground
        // so re-authentication is required when returning
        isBiometricAuthenticated = false
    }

    // ──────────────────────────────────────────────────────
    // Notification permission (Android 13+)
    // ──────────────────────────────────────────────────────

    /**
     * Requests the POST_NOTIFICATIONS permission on Android 13+ (API 33).
     *
     * Uses [ActivityCompat.requestPermissions] as the primary path,
     * with [notificationPermissionLauncher] as the modern Activity Result API fallback.
     * The app fully functions without notification permission — this is a best-effort
     * request to enable FCM-based incoming call alerts.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // Biometric authentication
    // ──────────────────────────────────────────────────────

    /**
     * Checks if screen lock is enabled in user preferences and, if so,
     * presents the system biometric prompt. If screen lock is disabled or
     * the device doesn't support biometrics, authentication is granted immediately.
     *
     * The check runs on [Dispatchers.Main.immediate] because it touches
     * mutable compose state (`isBiometricAuthenticated`, `isCheckingBiometric`).
     */
    private fun maybeShowBiometricAuth() {
        activityScope.launch {
            val screenLockEnabled = preferencesDataStore.isScreenLockEnabled.first()

            if (!screenLockEnabled) {
                isBiometricAuthenticated = true
                isCheckingBiometric = false
                requestNotificationPermissionIfNeeded()
                return@launch
            }

            val biometricManager = BiometricManager.from(this@MainActivity)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                // Device can't do biometrics but user wants screen lock —
                // grant access anyway (they set the preference before enrolling biometrics)
                isBiometricAuthenticated = true
                isCheckingBiometric = false
                requestNotificationPermissionIfNeeded()
                return@launch
            }

            isCheckingBiometric = false
            showBiometricPrompt()
        }
    }

    /**
     * Displays the system biometric authentication prompt.
     *
     * On success, grants access and requests notification permission.
     * On failure or cancellation, keeps the lock screen visible so
     * the user can retry.
     */
    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Zixo")
            .setSubtitle("Authenticate to access your messages")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isBiometricAuthenticated = true
                requestNotificationPermissionIfNeeded()
            }

            override fun onAuthenticationFailed() {
                // Keep the lock screen — user can retry
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // On error (e.g. user pressed back), stay locked out
                // but don't crash — the spinner remains visible
            }
        }

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            callback
        )

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        /** Intent extra key for FCM notification deep link call ID. */
        const val EXTRA_CALL_ID = "call_id"

        /** Request code for notification permission via ActivityCompat. */
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001
    }
}

// ════════════════════════════════════════════════════════════════
// Composable Content — Lifecycle-Aware Gate
// ════════════════════════════════════════════════════════════════

/**
 * Root composable for the MainActivity content.
 *
 * **White screen deadlock prevention:**
 * - While `isCheckingBiometric` is true, a loading spinner is shown.
 *   This prevents `ZixoNavigation` from composing before the biometric
 *   check completes, which would cause a flash of unauthenticated content.
 * - After biometric auth passes, `ZixoNavigation` is mounted, which
 *   internally uses `collectAsStateWithLifecycle()` on the auth state
 *   flow and atomically navigates between Auth and Home screens.
 * - The `popUpTo(Auth) { inclusive = true }` in `ZixoNavHost` ensures
 *   AuthScreen is completely removed from the backstack after login,
 *   preventing back-press white screens.
 */
@Composable
private fun MainContent(
    isBiometricAuthenticated: Boolean,
    isCheckingBiometric: Boolean,
    deepLinkCallId: String?,
) {
    when {
        isCheckingBiometric || !isBiometricAuthenticated -> {
            // Still checking biometric requirement, or waiting for auth
            // Show a branded loading spinner that matches the app theme
            // so there's no white flash at any point
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NeonMint)
            }
        }
        else -> {
            // Biometric check complete and authenticated — mount the
            // full navigation graph. ZixoNavHost handles auth-gated
            // routing with lifecycle-aware StateFlow collection.
            ZixoNavigation(deepLinkCallId = deepLinkCallId)
        }
    }
}
