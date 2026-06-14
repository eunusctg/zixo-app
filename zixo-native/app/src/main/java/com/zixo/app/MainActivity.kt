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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.content.ContextCompat
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.ui.navigation.ZixoMainScaffold
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

// ──────────────────────────────────────────────
// Single-Activity entry point
// ──────────────────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)

    /** Whether the user has passed biometric authentication for this session. */
    private var isBiometricAuthenticated by mutableStateOf(false)

    /** Whether we are still checking if biometric auth is required. */
    private var isCheckingBiometric by mutableStateOf(true)

    // ── Notification permission launcher (Android 13+) ──
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result handled silently — the app works without notifications */ }

    // ──────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ZixoTheme {
                MainContent(
                    isBiometricAuthenticated = isBiometricAuthenticated,
                    isCheckingBiometric = isCheckingBiometric,
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
    }

    override fun onPause() {
        super.onPause()
        // Reset biometric state when the activity leaves the foreground
        // so re-authentication is required when returning
        isBiometricAuthenticated = false
    }

    // ──────────────────────────────────────────
    // Notification permission (Android 13+)
    // ──────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    // ──────────────────────────────────────────
    // Biometric authentication
    // ──────────────────────────────────────────

    private fun maybeShowBiometricAuth() {
        activityScope.launch {
            val screenLockEnabled = preferencesDataStore.isScreenLockEnabled.first()

            if (!screenLockEnabled) {
                // Screen lock is not required — grant access immediately
                isBiometricAuthenticated = true
                isCheckingBiometric = false
                requestNotificationPermissionIfNeeded()
                return@launch
            }

            // Screen lock is required — check if biometric hardware is available
            val biometricManager = BiometricManager.from(this@MainActivity)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                // Biometric not available or not enrolled — fall back to allowing access
                isBiometricAuthenticated = true
                isCheckingBiometric = false
                requestNotificationPermissionIfNeeded()
                return@launch
            }

            // Show the biometric prompt
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
                // The prompt can be triggered again by tapping the locked screen
            }
        }

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            callback
        )

        biometricPrompt.authenticate(promptInfo)
    }
}

// ──────────────────────────────────────────────
// Composable content for MainActivity
// ──────────────────────────────────────────────

@Composable
private fun MainContent(
    isBiometricAuthenticated: Boolean,
    isCheckingBiometric: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
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
                ZixoMainScaffold()
            }
        }
    }
}
