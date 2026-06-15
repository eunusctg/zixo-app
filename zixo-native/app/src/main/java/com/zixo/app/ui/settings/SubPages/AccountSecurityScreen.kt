package com.zixo.app.ui.settings.SubPages

import android.credentials.CreatePublicKeyCredentialResponse
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.AppSettingsState
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Account & Security Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "Account & Security" page for passkey enrollment,
 * two-step verification, security notifications, and active devices.
 *
 * Features:
 * - Credential Manager integration for passkey creation
 * - Micro-animation alert on successful passkey registration
 * - Two-step verification (PIN + email for reset)
 * - Security notifications toggle
 * - Active devices section (placeholder)
 */
@Composable
fun AccountSecurityScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val isPasskeyRegistered by viewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Local state for two-step verification ──
    var showPinSetup by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 32.dp
            ),
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

            // ── Passkey / Biometrics Section ─────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PASSKEY & BIOMETRICS",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Create Passkey button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPasskeyRegistered) NeonMint.copy(alpha = 0.15f)
                                else NeonMint
                            )
                            .clickable(
                                enabled = !isPasskeyRegistered,
                                onClick = {
                                    // CredentialManager integration
                                    // The requestJson is obtained from the Cloudflare backend
                                    // For now, using a placeholder that the backend would provide
                                    viewModel.createPasskey(
                                        context = context,
                                        requestJson = """{"publicKey":{"rp":{"name":"Zixo"},"user":{"name":"user","displayName":"User"},"challenge":"placeholder","pubKeyCredParams":[{"type":"public-key","alg":-7}],"timeout":60000,"attestation":"none"}}"""
                                    )
                                }
                            )
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (settingsState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeonMint,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (isPasskeyRegistered) NeonMint else Color(0xFF0B1519),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPasskeyRegistered) "Passkey Registered" else "Create Passkey",
                            color = if (isPasskeyRegistered) NeonMint else Color(0xFF0B1519),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Success animation
                    AnimatedVisibility(
                        visible = isPasskeyRegistered,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Biometrics/Passkey Active",
                                color = NeonMint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Error message
                    if (!settingsState.errorMessage.isNullOrBlank() && !isPasskeyRegistered) {
                        Text(
                            text = settingsState.errorMessage!!,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // ── Two-Step Verification ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TWO-STEP VERIFICATION",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Set/change PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPinSetup = !showPinSetup }
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
                                text = if (settingsState.isTwoStepEnabled) "Change PIN" else "Set PIN",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (settingsState.isTwoStepEnabled)
                                    "PIN is currently set"
                                else
                                    "Require PIN for additional verification",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // PIN setup fields
                    AnimatedVisibility(visible = showPinSetup) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            GlassOutlinedTextField(
                                value = pin,
                                onValueChange = { pin = it },
                                label = { Text("PIN") },
                                placeholder = { Text("Enter 6-digit PIN") },
                                keyboardType = KeyboardType.Number,
                                maxLength = 6,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            GlassOutlinedTextField(
                                value = confirmPin,
                                onValueChange = { confirmPin = it },
                                label = { Text("Confirm PIN") },
                                placeholder = { Text("Re-enter PIN") },
                                keyboardType = KeyboardType.Number,
                                maxLength = 6,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            GlassOutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = { Text("Email for Reset") },
                                placeholder = { Text("your@email.com") },
                                keyboardType = KeyboardType.Email,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonMint)
                                    .clickable {
                                        if (pin.length == 6 && pin == confirmPin) {
                                            viewModel.updateTwoStep(true)
                                            showPinSetup = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Save PIN",
                                    color = Color(0xFF0B1519),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── Security Notifications ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SECURITY",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

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

            // ── Active Devices (Placeholder) ─────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ACTIVE DEVICES",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Active Devices",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "View and manage devices signed into your account",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Navigate",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
