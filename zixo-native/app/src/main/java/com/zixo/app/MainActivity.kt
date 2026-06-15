package com.zixo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
// Single-Activity entry point for the Zixo application
// ════════════════════════════════════════════════════════════════

/**
 * Main activity that hosts the entire Zixo application.
 *
 * Responsibilities:
 * - **Hilt injection** via [@AndroidEntryPoint][AndroidEntryPoint]
 * - **Edge-to-edge** display with transparent system bars
 * - **Biometric authentication** gate (when screen lock is enabled)
 * - **Notification permission** request on Android 13+ via [ActivityCompat.requestPermissions]
 * - **FCM deep link** handling from notification intents (e.g. incoming call `callId`)
 * - Sets content with [ZixoTheme] → [ZixoNavigation]
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
     * Uses [ActivityCompat.requestPermissions] as the primary path,
     * with [notificationPermissionLauncher] as the modern Activity Result API fallback.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Primary: ActivityCompat.requestPermissions (as required by spec)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Silently handle notification permission result
        // The app functions correctly regardless of the notification permission state
    }

    // ──────────────────────────────────────────────────────
    // Biometric authentication
    // ──────────────────────────────────────────────────────

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
                isBiometricAuthenticated = true
                isCheckingBiometric = false
                requestNotificationPermissionIfNeeded()
                return@launch
            }

            isCheckingBiometric = false
            showBiometricPrompt()
        }
    }

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
// Composable content for MainActivity
// ════════════════════════════════════════════════════════════════

@Composable
private fun MainContent(
    isBiometricAuthenticated: Boolean,
    isCheckingBiometric: Boolean,
    deepLinkCallId: String?,
) {
    when {
        isCheckingBiometric || !isBiometricAuthenticated -> {
            // Still checking biometric requirement, or waiting for auth
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
            ZixoNavigation(deepLinkCallId = deepLinkCallId)
        }
    }
}
