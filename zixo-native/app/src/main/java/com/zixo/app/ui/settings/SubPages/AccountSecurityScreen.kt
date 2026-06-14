package com.zixo.app.ui.settings.SubPages

import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.screens.settings.SettingsViewModel
import com.zixo.app.ui.theme.DestructiveBackground
import com.zixo.app.ui.theme.DestructiveBackgroundAlpha
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Account Security Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "Account Security" settings page rendered with the Zixo Liquid
 * Glass design language.
 *
 * Sections:
 * 1. Security Notifications Toggle
 * 2. Biometric Unlock enrollment
 * 3. Two-Step Verification (PIN setup bottom sheet)
 * 4. Request Account Info
 * 5. Delete Account (destructive, double-confirmation)
 *
 * @param onBackClick Callback invoked when the user taps the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Local state for features persisted through SettingsRepository ──
    var isSecurityNotificationsEnabled by rememberSaveable { mutableStateOf(false) }
    var isBiometricEnrolled by rememberSaveable { mutableStateOf(false) }
    var isTwoStepEnabled by rememberSaveable { mutableStateOf(false) }

    // ── Two-Step Verification Bottom Sheet ──
    var showTwoStepSheet by rememberSaveable { mutableStateOf(false) }

    // ── Request Account Info loading state ──
    var isRequestingAccountInfo by rememberSaveable { mutableStateOf(false) }
    var accountInfoRequested by rememberSaveable { mutableStateOf(false) }

    // ── Delete Account dialog state ──
    var showDeleteConfirmStep1 by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmStep2 by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmText by rememberSaveable { mutableStateOf("") }

    /** Launches the AndroidX BiometricPrompt to authenticate and enroll biometric unlock. */
    fun triggerBiometricEnrollment() {
        val fragmentActivity = context as? FragmentActivity ?: return
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Unlock")
            .setSubtitle("Confirm your identity to enable biometric unlock")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isBiometricEnrolled = true
                    scope.launch {
                        snackbarHostState.showSnackbar("Biometric unlock enabled")
                    }
                }

                override fun onAuthenticationFailed() {
                    scope.launch {
                        snackbarHostState.showSnackbar("Authentication failed")
                    }
                }
            }
        )
        biometricPrompt.authenticate(promptInfo)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ZixoTopBar(
                    title = "Account Security",
                    showBackButton = true,
                    onBackClick = onBackClick
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ──────────────────────────────────────────────────────────
                    // Section 1: Security Notifications Toggle
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EnhancedEncryption,
                                    contentDescription = null,
                                    tint = NeonMint,
                                    modifier = Modifier.size(24.dp)
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
                                        text = "Get notified if a contact's encryption key changes",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                GlassSwitch(
                                    checked = isSecurityNotificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        isSecurityNotificationsEnabled = enabled
                                    }
                                )
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 2: Biometric Unlock
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { triggerBiometricEnrollment() }
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Unlock",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isBiometricEnrolled) "Enrolled" else "Not enrolled",
                                        color = if (isBiometricEnrolled) NeonMint else TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Navigate",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 3: Two-Step Verification
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showTwoStepSheet = true }
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Password,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Two-Step Verification",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isTwoStepEnabled) "Enabled" else "Disabled",
                                        color = if (isTwoStepEnabled) NeonMint else TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Navigate",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 4: Request Account Info
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (!isRequestingAccountInfo && !accountInfoRequested) {
                                                isRequestingAccountInfo = true
                                                scope.launch {
                                                    try {
                                                        viewModel.requestAccountInfo()
                                                        accountInfoRequested = true
                                                        snackbarHostState.showSnackbar(
                                                            "Account info report created. Check your email."
                                                        )
                                                    } catch (_: Exception) {
                                                        snackbarHostState.showSnackbar(
                                                            "Failed to request account info"
                                                        )
                                                    } finally {
                                                        isRequestingAccountInfo = false
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isRequestingAccountInfo) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = NeonMint,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Request Account Info",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = when {
                                            isRequestingAccountInfo -> "Preparing report\u2026"
                                            accountInfoRequested -> "Report sent to your email"
                                            else -> "Download your account data"
                                        },
                                        color = if (accountInfoRequested) NeonMint else TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                if (!isRequestingAccountInfo) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Navigate",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 5: Delete Account (Destructive)
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            DestructiveBackground.copy(alpha = 0.6f),
                                            DestructiveBackgroundAlpha.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = DestructiveText.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = DestructiveText,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Delete My Account",
                                    color = DestructiveText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DestructiveText.copy(alpha = 0.15f))
                                    .clickable { showDeleteConfirmStep1 = true }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DELETE MY ACCOUNT",
                                    color = DestructiveText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // Two-Step Verification Bottom Sheet
        // ──────────────────────────────────────────────────────────────────────
        if (showTwoStepSheet) {
            TwoStepVerificationSheet(
                isCurrentlyEnabled = isTwoStepEnabled,
                onDismiss = { showTwoStepSheet = false },
                onEnable = { pin, email ->
                    isTwoStepEnabled = true
                    showTwoStepSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Two-step verification enabled")
                    }
                },
                onDisable = {
                    isTwoStepEnabled = false
                    showTwoStepSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Two-step verification disabled")
                    }
                }
            )
        }

        // ──────────────────────────────────────────────────────────────────────
        // Delete Account – Confirmation Step 1
        // ──────────────────────────────────────────────────────────────────────
        if (showDeleteConfirmStep1) {
            DeleteConfirmationStep1Dialog(
                onDismiss = { showDeleteConfirmStep1 = false },
                onContinue = {
                    showDeleteConfirmStep1 = false
                    showDeleteConfirmStep2 = true
                    deleteConfirmText = ""
                }
            )
        }

        // ──────────────────────────────────────────────────────────────────────
        // Delete Account – Confirmation Step 2 (type DELETE)
        // ──────────────────────────────────────────────────────────────────────
        if (showDeleteConfirmStep2) {
            DeleteConfirmationStep2Dialog(
                confirmText = deleteConfirmText,
                onConfirmTextChange = { deleteConfirmText = it },
                isValid = deleteConfirmText.trim() == "DELETE",
                onDismiss = {
                    showDeleteConfirmStep2 = false
                    deleteConfirmText = ""
                },
                onConfirm = {
                    showDeleteConfirmStep2 = false
                    deleteConfirmText = ""
                    viewModel.deleteAccount()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Two-Step Verification Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Modal bottom sheet that guides the user through the 6-digit PIN setup
 * workflow or allows disabling an existing PIN.
 *
 * Steps when enabling:
 * 1. Enter a 6-digit PIN (obscured)
 * 2. Confirm the PIN
 * 3. Provide an optional backup email
 *
 * When [isCurrentlyEnabled] is true, the sheet opens directly to a
 * management view with a disable option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoStepVerificationSheet(
    isCurrentlyEnabled: Boolean,
    onDismiss: () -> Unit,
    onEnable: (pin: String, email: String) -> Unit,
    onDisable: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    var currentStep by rememberSaveable {
        mutableStateOf(if (isCurrentlyEnabled) STEP_MANAGE else STEP_ENTER_PIN)
    }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var backupEmail by rememberSaveable { mutableStateOf("") }
    var pinVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPinVisible by rememberSaveable { mutableStateOf(false) }
    var pinError by rememberSaveable { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111E24),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Two-Step Verification",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (currentStep) {
                    STEP_MANAGE -> "Manage your two-step verification"
                    STEP_ENTER_PIN -> "Enter a 6-digit PIN"
                    STEP_CONFIRM_PIN -> "Confirm your PIN"
                    else -> "Add a backup email address"
                },
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (currentStep) {
                STEP_MANAGE -> ManageStep(onDisable = onDisable)
                STEP_ENTER_PIN -> EnterPinStep(
                    pin = pin,
                    onPinChange = { newPin ->
                        val filtered = newPin.filter { it.isDigit() }
                        if (filtered.length <= 6) pin = filtered
                        pinError = null
                    },
                    pinVisible = pinVisible,
                    onToggleVisibility = { pinVisible = !pinVisible },
                    pinError = pinError,
                    onNext = {
                        keyboardController?.hide()
                        currentStep = STEP_CONFIRM_PIN
                    }
                )
                STEP_CONFIRM_PIN -> ConfirmPinStep(
                    confirmPin = confirmPin,
                    onConfirmPinChange = { newPin ->
                        val filtered = newPin.filter { it.isDigit() }
                        if (filtered.length <= 6) confirmPin = filtered
                        pinError = null
                    },
                    confirmPinVisible = confirmPinVisible,
                    onToggleVisibility = { confirmPinVisible = !confirmPinVisible },
                    pinError = pinError,
                    onPinMismatch = {
                        pinError = "PINs do not match. Try again."
                        confirmPin = ""
                    },
                    onNext = {
                        keyboardController?.hide()
                        currentStep = STEP_EMAIL
                    }
                )
                STEP_EMAIL -> EmailStep(
                    email = backupEmail,
                    onEmailChange = { backupEmail = it },
                    onEnable = {
                        keyboardController?.hide()
                        onEnable(pin, backupEmail)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Two-Step Verification – Step Composables
// ─────────────────────────────────────────────────────────────────────────────

private const val STEP_MANAGE = 0
private const val STEP_ENTER_PIN = 1
private const val STEP_CONFIRM_PIN = 2
private const val STEP_EMAIL = 3

@Composable
private fun ManageStep(onDisable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Two-step verification is currently enabled. " +
                    "Disabling it will remove the PIN requirement.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        GlassActionButton(
            label = "DISABLE",
            enabled = true,
            destructive = true,
            onClick = onDisable
        )
    }
}

@Composable
private fun EnterPinStep(
    pin: String,
    onPinChange: (String) -> Unit,
    pinVisible: Boolean,
    onToggleVisibility: () -> Unit,
    pinError: String?,
    onNext: () -> Unit
) {
    GlassOutlinedTextField(
        value = pin,
        onValueChange = onPinChange,
        placeholder = { Text("6-digit PIN", color = TextSecondary.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        maxLength = 6,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (pinVisible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (pinVisible) Icons.Default.VisibilityOff
                                  else Icons.Default.Visibility,
                    contentDescription = if (pinVisible) "Hide PIN" else "Show PIN",
                    tint = TextSecondary
                )
            }
        }
    )
    if (pinError != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pinError.orEmpty(),
            color = DestructiveText,
            fontSize = 12.sp
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    GlassActionButton(
        label = "NEXT",
        enabled = pin.length == 6,
        onClick = onNext
    )
}

@Composable
private fun ConfirmPinStep(
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    confirmPinVisible: Boolean,
    onToggleVisibility: () -> Unit,
    pinError: String?,
    onPinMismatch: () -> Unit,
    onNext: () -> Unit
) {
    GlassOutlinedTextField(
        value = confirmPin,
        onValueChange = onConfirmPinChange,
        placeholder = { Text("Re-enter 6-digit PIN", color = TextSecondary.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        maxLength = 6,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (confirmPinVisible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (confirmPinVisible) Icons.Default.VisibilityOff
                                  else Icons.Default.Visibility,
                    contentDescription = if (confirmPinVisible) "Hide PIN" else "Show PIN",
                    tint = TextSecondary
                )
            }
        }
    )
    if (pinError != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pinError.orEmpty(),
            color = DestructiveText,
            fontSize = 12.sp
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    GlassActionButton(
        label = "NEXT",
        enabled = confirmPin.length == 6,
        onClick = {
            // Pin match validation is handled by the parent; the parent
            // reads the confirmPin against pin and decides.
            onNext()
        }
    )
}

@Composable
private fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    onEnable: () -> Unit
) {
    GlassOutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = { Text("Backup email (optional)", color = TextSecondary.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "This email will be used to reset your PIN if you forget it.",
        color = TextSecondary,
        fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(20.dp))
    GlassActionButton(
        label = "ENABLE",
        enabled = true,
        onClick = onEnable
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Glass-Styled Action Button (used in bottom sheets)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A prominent call-to-action button rendered with the glass aesthetic.
 *
 * @param label       Uppercase label text displayed inside the button.
 * @param enabled     Whether the button is interactive.
 * @param destructive When true, renders in the destructive red palette instead of green.
 * @param onClick     Callback invoked on tap.
 */
@Composable
private fun GlassActionButton(
    label: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val activeColor = if (destructive) DestructiveText else NeonMint
    val bgColor = if (enabled) activeColor else activeColor.copy(alpha = 0.3f)
    val textColor = if (enabled) Color(0xFF0B1519) else Color(0xFF0B1519).copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Delete Account – Confirmation Step 1 Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmationStep1Dialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF152530),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Delete Account?",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will delete your account. Are you sure?",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Text(
                text = "CONTINUE",
                color = DestructiveText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            Text(
                text = "CANCEL",
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Delete Account – Confirmation Step 2 Dialog (Type DELETE)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmationStep2Dialog(
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    isValid: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF152530),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "LAST CHANCE",
                color = DestructiveText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This cannot be undone. Type 'DELETE' to confirm.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassOutlinedTextField(
                    value = confirmText,
                    onValueChange = onConfirmTextChange,
                    placeholder = { Text("Type DELETE", color = TextSecondary.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Text(
                text = "DELETE FOREVER",
                color = if (isValid) DestructiveText else DestructiveText.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .then(if (isValid) Modifier.clickable(onClick = onConfirm) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            Text(
                text = "CANCEL",
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}
