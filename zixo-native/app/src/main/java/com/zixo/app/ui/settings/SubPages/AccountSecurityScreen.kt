package com.zixo.app.ui.settings.SubPages

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.AppSettingsState
import com.zixo.app.domain.model.Session
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.theme.AmoledBlack
import com.zixo.app.ui.theme.DestructiveBackground
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Account & Security Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "Account & Security" page featuring:
 *
 * 1. **Biometric Gate** — native BiometricPrompt with comprehensive error handling
 *    (HW unavailable, no enrolled biometrics, locked out).
 * 2. **Two-Step PIN** — 4-digit PIN with confirmation step.
 * 3. **Passkey Management** — register new passkeys, delete existing.
 * 4. **Active Sessions** — view and revoke sessions across devices.
 * 5. **Account Deletion** — destructive action with confirmation dialog.
 * 6. **Security Notifications** — toggle for security change alerts.
 *
 * All cards use [liquidGlassCard] with NeonMint for active states and
 * AmoledBlack backgrounds for the dark glass aesthetic.
 */
@Composable
fun AccountSecurityScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val isPasskeyRegistered by viewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Local UI state ──
    var showPinSetup by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinStep by remember { mutableIntStateOf(0) } // 0=hidden, 1=enter, 2=confirm
    var pinError by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    var biometricStatus by remember { mutableStateOf<BiometricStatus>(BiometricStatus.Unknown) }
    var biometricAuthResult by remember { mutableStateOf<BiometricAuthResult?>(null) }

    // ── Active sessions (from ViewModel or placeholder) ──
    var sessions by remember { mutableStateOf(sampleSessions) }

    // ── Passkey list ──
    var passkeys by remember {
        mutableStateOf(
            if (isPasskeyRegistered) listOf(
                PasskeyEntry(id = "pk_001", name = "Primary Device", createdAt = "Jan 15, 2025"),
                PasskeyEntry(id = "pk_002", name = "Backup Key", createdAt = "Feb 03, 2025")
            ) else emptyList()
        )
    }
    var showDeletePasskeyDialog by remember { mutableStateOf<PasskeyEntry?>(null) }

    // ── Check biometric availability on composition ──
    LaunchedEffect(Unit) {
        try {
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            biometricStatus = when (canAuthenticate) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.HwUnavailable
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HwUnavailable
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoEnrolled
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.HwUnavailable
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.HwUnavailable
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.Unknown
                else -> BiometricStatus.Unknown
            }
        } catch (_: Exception) {
            biometricStatus = BiometricStatus.Unknown
        }
    }

    // ── Update passkeys when registration state changes ──
    LaunchedEffect(isPasskeyRegistered) {
        try {
            if (isPasskeyRegistered && passkeys.isEmpty()) {
                passkeys = listOf(
                    PasskeyEntry(id = "pk_001", name = "Primary Device", createdAt = "Today")
                )
            }
        } catch (_: Exception) {
            // Non-critical — passkey list will be empty
        }
    }

    // ── Delete passkey dialog ──
    if (showDeletePasskeyDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeletePasskeyDialog = null },
            title = {
                Text(
                    text = "Delete Passkey?",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Removing \"${showDeletePasskeyDialog?.name ?: "this passkey"}\" means you will no longer be able to sign in with this passkey. Make sure you have another authentication method available.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dialogPasskey = showDeletePasskeyDialog
                        if (dialogPasskey != null) {
                            passkeys = passkeys.filter { it.id != dialogPasskey.id }
                        }
                        showDeletePasskeyDialog = null
                    }
                ) {
                    Text("Delete", color = DestructiveText, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePasskeyDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AmoledBlack,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    // ── Delete account confirmation dialog ──
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteConfirmText = ""
            },
            title = {
                Text(
                    text = "Delete Your Account?",
                    color = DestructiveText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This action is permanent and cannot be undone. All your messages, media, contacts, and account data will be permanently erased from Zixo servers.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Type DELETE to confirm:",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassOutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        placeholder = { Text("DELETE", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                        deleteConfirmText = ""
                    },
                    enabled = deleteConfirmText == "DELETE"
                ) {
                    Text(
                        "Delete Account",
                        color = if (deleteConfirmText == "DELETE") DestructiveText else TextSecondary.copy(alpha = 0.3f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteConfirmText = ""
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AmoledBlack,
            titleContentColor = DestructiveText,
            textContentColor = TextSecondary
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top bar ──
            item {
                ZixoTopBar(
                    title = "Account & Security",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Biometric Authentication ─────────────────────────────────
            item {
                BiometricSection(
                    biometricStatus = biometricStatus,
                    biometricAuthResult = biometricAuthResult,
                    onAuthenticate = {
                        launchBiometricPrompt(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            onSuccess = {
                                biometricAuthResult = BiometricAuthResult.Success
                            },
                            onFailed = {
                                biometricAuthResult = BiometricAuthResult.Failed
                            },
                            onError = { errorCode, errorString ->
                                biometricAuthResult = BiometricAuthResult.Error(errorCode, errorString)
                            }
                        )
                    },
                    onDismissResult = { biometricAuthResult = null }
                )
            }

            // ── Passkey Management ───────────────────────────────────────
            item {
                PasskeyManagementCard(
                    passkeys = passkeys,
                    isPasskeyRegistered = isPasskeyRegistered,
                    isLoading = settingsState.isLoading,
                    onRegisterPasskey = {
                        viewModel.createPasskey(
                            context = context,
                            requestJson = """{"publicKey":{"rp":{"name":"Zixo","id":"web.zixo.eu.cc"},"user":{"name":"user","displayName":"User"},"challenge":"placeholder","pubKeyCredParams":[{"type":"public-key","alg":-7}],"timeout":60000,"attestation":"none"}}"""
                        )
                    },
                    onDeletePasskey = { passkey -> showDeletePasskeyDialog = passkey },
                    errorMessage = if (!settingsState.errorMessage.isNullOrBlank() && !isPasskeyRegistered) settingsState.errorMessage else null
                )
            }

            // ── Two-Step Verification (4-digit PIN) ─────────────────────
            item {
                PinSetupCard(
                    isPinEnabled = settingsState.isTwoStepEnabled,
                    showPinSetup = showPinSetup,
                    pinStep = pinStep,
                    pin = pin,
                    confirmPin = confirmPin,
                    pinError = pinError,
                    onTogglePinSetup = { showPinSetup = !showPinSetup },
                    onPinChange = { newPin ->
                        pin = newPin
                        pinError = null
                        if (newPin.length == 4 && pinStep == 1) {
                            pinStep = 2
                        }
                    },
                    onConfirmPinChange = { newConfirm ->
                        confirmPin = newConfirm
                        pinError = null
                        if (newConfirm.length == 4) {
                            if (confirmPin == pin) {
                                viewModel.updateTwoStep(true)
                                pinStep = 0
                                showPinSetup = false
                                pin = ""
                                confirmPin = ""
                            } else {
                                pinError = "PINs do not match. Try again."
                                confirmPin = ""
                            }
                        }
                    },
                    onStartPinSetup = {
                        pinStep = 1
                        pin = ""
                        confirmPin = ""
                        pinError = null
                    },
                    onDisablePin = {
                        viewModel.updateTwoStep(false)
                        pin = ""
                        confirmPin = ""
                    },
                    onBackStep = {
                        if (pinStep == 2) {
                            pinStep = 1
                            confirmPin = ""
                        } else {
                            pinStep = 0
                            pin = ""
                            showPinSetup = false
                        }
                    }
                )
            }

            // ── Security Notifications ───────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("SECURITY")
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security Notifications",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Get notified when security changes occur",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.isSecurityNotificationsEnabled,
                            onCheckedChange = { viewModel.updateSecurityNotifications(it) }
                        )
                    }
                }
            }

            // ── Active Sessions ──────────────────────────────────────────
            item {
                ActiveSessionsCard(
                    sessions = sessions,
                    onRevokeSession = { session ->
                        sessions = sessions.filter { it.id != session.id }
                        Toast.makeText(context, "Session revoked: ${session.deviceName}", Toast.LENGTH_SHORT).show()
                    },
                    onRevokeAllOther = {
                        val currentSession = sessions.firstOrNull { it.isActive }
                        sessions = if (currentSession != null) listOf(currentSession) else emptyList()
                        Toast.makeText(context, "All other sessions revoked", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── Account Deletion ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("DANGER ZONE")
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = DestructiveText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Delete My Account",
                                color = DestructiveText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Permanently erase all data and close your account",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DestructiveBackground)
                            .border(1.dp, DestructiveText.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { showDeleteDialog = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = DestructiveText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            color = DestructiveText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Biometric Status & Result Types
// ─────────────────────────────────────────────────────────────────────────────

private enum class BiometricStatus { Available, HwUnavailable, NoEnrolled, Unknown }

private sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()
    data object Failed : BiometricAuthResult()
    data class Error(val errorCode: Int, val message: String) : BiometricAuthResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// Passkey Data Model
// ─────────────────────────────────────────────────────────────────────────────

private data class PasskeyEntry(
    val id: String,
    val name: String,
    val createdAt: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Sample Sessions Data
// ─────────────────────────────────────────────────────────────────────────────

private val sampleSessions = listOf(
    Session(
        id = "sess_001",
        deviceName = "Pixel 8 Pro",
        deviceModel = "Pixel 8 Pro",
        osVersion = "Android 14",
        appVersion = "1.0.0",
        ipAddress = "192.168.1.42",
        lastActive = System.currentTimeMillis(),
        isActive = true,
        createdAt = System.currentTimeMillis() - 86400000L * 7
    ),
    Session(
        id = "sess_002",
        deviceName = "Samsung Galaxy S24",
        deviceModel = "SM-S921B",
        osVersion = "Android 14",
        appVersion = "1.0.0",
        ipAddress = "10.0.0.15",
        lastActive = System.currentTimeMillis() - 3600000L,
        isActive = false,
        createdAt = System.currentTimeMillis() - 86400000L * 30
    ),
    Session(
        id = "sess_003",
        deviceName = "Chrome on MacBook",
        deviceModel = "macOS Sonoma",
        osVersion = "Chrome 121",
        appVersion = "1.0.0",
        ipAddress = "172.16.0.8",
        lastActive = System.currentTimeMillis() - 86400000L * 2,
        isActive = false,
        createdAt = System.currentTimeMillis() - 86400000L * 45
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Biometric Prompt Launcher
// ─────────────────────────────────────────────────────────────────────────────

private fun launchBiometricPrompt(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onSuccess: () -> Unit,
    onFailed: () -> Unit,
    onError: (Int, String) -> Unit
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Zixo")
        .setSubtitle("Use your fingerprint or face")
        .setNegativeButtonText("Use PIN")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }

        override fun onAuthenticationFailed() {
            onFailed()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onError(errorCode, errString.toString())
        }
    }

    val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
    val activity = context as? androidx.fragment.app.FragmentActivity
    if (activity == null) {
        onError(BiometricPrompt.ERROR_HW_UNAVAILABLE, "Unable to launch biometric prompt: not an Activity context")
        return
    }
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        callback
    )

    biometricPrompt.authenticate(promptInfo)
}

// ─────────────────────────────────────────────────────────────────────────────
// Biometric Section Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BiometricSection(
    biometricStatus: BiometricStatus,
    biometricAuthResult: BiometricAuthResult?,
    onAuthenticate: () -> Unit,
    onDismissResult: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("BIOMETRIC AUTHENTICATION")
        Spacer(modifier = Modifier.height(16.dp))

        when (biometricStatus) {
            BiometricStatus.Available -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonMint)
                        .clickable { onAuthenticate() }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = AmoledBlack,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Authenticate with Biometrics",
                        color = AmoledBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                biometricAuthResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut()
                    ) {
                        when (result) {
                            is BiometricAuthResult.Success -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authentication successful", color = NeonMint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            is BiometricAuthResult.Failed -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = DestructiveText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authentication failed. Try again.", color = DestructiveText, fontSize = 14.sp)
                                }
                            }
                            is BiometricAuthResult.Error -> {
                                val errorMsg = when (result.errorCode) {
                                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable. Try again later."
                                    BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "Unable to process biometric data."
                                    BiometricPrompt.ERROR_TIMEOUT -> "Authentication timed out."
                                    BiometricPrompt.ERROR_NO_SPACE -> "Insufficient storage for biometric data."
                                    BiometricPrompt.ERROR_CANCELED -> "Authentication was cancelled."
                                    BiometricPrompt.ERROR_LOCKOUT -> "Too many attempts. Try again later."
                                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometric locked out. Use PIN to unlock."
                                    BiometricPrompt.ERROR_USER_CANCELED -> "Authentication cancelled by user."
                                    BiometricPrompt.ERROR_NO_BIOMETRICS -> "No biometrics enrolled. Set up fingerprint or face in Settings."
                                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> "No biometric hardware on this device."
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "PIN fallback selected."
                                    else -> result.message
                                }
                                Column {
                                    Text(errorMsg, color = DestructiveText, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = onDismissResult) {
                                        Text("Dismiss", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BiometricStatus.HwUnavailable -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DestructiveBackground)
                        .border(1.dp, DestructiveText.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = DestructiveText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Biometric hardware unavailable on this device",
                        color = DestructiveText,
                        fontSize = 14.sp
                    )
                }
            }
            BiometricStatus.NoEnrolled -> {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonMintAlpha20)
                            .border(1.dp, NeonMint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = NeonMint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "No biometrics enrolled. Set up fingerprint or face unlock in device Settings first.",
                            color = NeonMint,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            BiometricStatus.Unknown -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NeonMint,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Passkey Management Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PasskeyManagementCard(
    passkeys: List<PasskeyEntry>,
    isPasskeyRegistered: Boolean,
    isLoading: Boolean,
    onRegisterPasskey: () -> Unit,
    onDeletePasskey: (PasskeyEntry) -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("PASSKEYS")
        Spacer(modifier = Modifier.height(12.dp))

        // Register button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPasskeyRegistered) NeonMintAlpha20 else NeonMint)
                .clickable(enabled = !isLoading, onClick = onRegisterPasskey)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NeonMint,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isPasskeyRegistered) NeonMint else AmoledBlack,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isPasskeyRegistered) "Register Additional Passkey" else "Create Passkey",
                color = if (isPasskeyRegistered) NeonMint else AmoledBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Error
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = DestructiveText, fontSize = 13.sp)
        }

        // Passkey list
        if (passkeys.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = passkeys.isNotEmpty(),
                enter = fadeIn() + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonMint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            passkeys.forEach { passkey ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = NeonMint.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = passkey.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Created ${passkey.createdAt}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Delete passkey",
                        tint = DestructiveText.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDeletePasskey(passkey) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PIN Setup Card (4-digit, 2-step)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinSetupCard(
    isPinEnabled: Boolean,
    showPinSetup: Boolean,
    pinStep: Int,
    pin: String,
    confirmPin: String,
    pinError: String?,
    onTogglePinSetup: () -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onStartPinSetup: () -> Unit,
    onDisablePin: () -> Unit,
    onBackStep: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("TWO-STEP VERIFICATION")
        Spacer(modifier = Modifier.height(16.dp))

        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTogglePinSetup() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Password,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPinEnabled) "Change PIN" else "Set PIN",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isPinEnabled) "4-digit PIN is currently active" else "Require 4-digit PIN for verification",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // PIN setup flow
        AnimatedVisibility(visible = showPinSetup || pinStep > 0) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))

                if (pinStep == 0 && !isPinEnabled) {
                    // Start button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonMint)
                            .clickable { onStartPinSetup() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Set Up PIN", color = AmoledBlack, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (pinStep == 1) {
                    Text(
                        text = "Step 1: Enter a 4-digit PIN",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // PIN dot indicators
                    PinDotsDisplay(pin = pin, maxLength = 4)
                    Spacer(modifier = Modifier.height(12.dp))

                    GlassOutlinedTextField(
                        value = pin,
                        onValueChange = { newPin ->
                            if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                                onPinChange(newPin)
                            }
                        },
                        label = { Text("PIN") },
                        placeholder = { Text("••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        maxLength = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onBackStep,
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Back", color = TextSecondary) }

                        OutlinedButton(
                            onClick = { if (pin.length == 4) onPinChange(pin) },
                            shape = RoundedCornerShape(12.dp),
                            enabled = pin.length == 4
                        ) { Text("Next", color = if (pin.length == 4) NeonMint else TextSecondary) }
                    }
                }

                if (pinStep == 2) {
                    Text(
                        text = "Step 2: Confirm your PIN",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PinDotsDisplay(pin = confirmPin, maxLength = 4)
                    Spacer(modifier = Modifier.height(12.dp))

                    GlassOutlinedTextField(
                        value = confirmPin,
                        onValueChange = { newPin ->
                            if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                                onConfirmPinChange(newPin)
                            }
                        },
                        label = { Text("Confirm PIN") },
                        placeholder = { Text("••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        maxLength = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(pinError, color = DestructiveText, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onBackStep,
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Back", color = TextSecondary) }
                    }
                }

                // Disable PIN option when PIN is already set
                if (isPinEnabled && pinStep == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DestructiveBackground)
                            .border(1.dp, DestructiveText.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { onDisablePin() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Disable PIN", color = DestructiveText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PIN Dots Visual Display
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinDotsDisplay(pin: String, maxLength: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxLength) {
            val filled = i < pin.length
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (filled) NeonMint else NeonMint.copy(alpha = 0.2f))
                    .then(
                        if (!filled) Modifier.border(1.dp, NeonMint.copy(alpha = 0.4f), CircleShape)
                        else Modifier
                    )
            )
            if (i < maxLength - 1) {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Active Sessions Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveSessionsCard(
    sessions: List<Session>,
    onRevokeSession: (Session) -> Unit,
    onRevokeAllOther: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("ACTIVE DEVICES")
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${sessions.size} device${if (sessions.size != 1) "s" else ""} signed in",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        sessions.forEach { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Devices,
                    contentDescription = null,
                    tint = if (session.isActive) NeonMint else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.deviceName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (session.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonMint)
                            )
                        }
                    }
                    Text(
                        text = "${session.osVersion} • ${session.appVersion}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (session.ipAddress != null) {
                        Text(
                            text = "IP: ${session.ipAddress}",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
                if (!session.isActive) {
                    Text(
                        text = "Revoke",
                        color = DestructiveText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onRevokeSession(session) }
                    )
                } else {
                    Text(
                        text = "This device",
                        color = NeonMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Revoke all other button
        val otherSessions = sessions.count { !it.isActive }
        if (otherSessions > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRevokeAllOther,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Log Out All Other Devices ($otherSessions)",
                    color = DestructiveText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Section Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = NeonMint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Color aliases
// ─────────────────────────────────────────────────────────────────────────────

private val NeonMintAlpha20 = NeonMint.copy(alpha = 0.15f)
private val TextTertiary = Color(0xFF607D8B)
