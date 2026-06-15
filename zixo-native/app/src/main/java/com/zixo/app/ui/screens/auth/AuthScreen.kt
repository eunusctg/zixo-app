package com.zixo.app.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.Capability
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.DestructiveBackground
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

/**
 * Zixo Authentication Screen — Google Sign-In via CredentialManager.
 *
 * This screen handles two flows:
 * 1. **Sign-In Flow**: User taps "Continue with Google" → CredentialManager
 *    launches the native Google Sign-In sheet → Token is verified by
 *    Cloudflare Edge Worker → Firebase Auth session is created.
 * 2. **Profile Setup Flow** (new users only): After successful Google Sign-In,
 *    new users are prompted to enter a display name. The system-generated
 *    8-digit Zixo Number and @username are minted by the Cloudflare backend.
 *
 * No email/password fields — authentication is exclusively via Google
 * CredentialManager with WebAuthn passkey support.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        AuthScreenContent(
            uiState = uiState,
            onGoogleSignIn = {
                val activity = context as? Activity
                if (activity != null) {
                    viewModel.signInWithGoogle(activity)
                }
            },
            onDisplayNameChange = viewModel::onDisplayNameChange,
            onProfileSetupSubmit = {
                viewModel.setDisplayNameAndContinue(uiState.displayName)
            },
            onClearError = viewModel::clearError
        )
    }
}

@Composable
private fun AuthScreenContent(
    uiState: AuthUiState,
    onGoogleSignIn: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onProfileSetupSubmit: () -> Unit,
    onClearError: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ── ZIXO Logo ──────────────────────────────
            Text(
                text = "ZIXO",
                color = NeonMint,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tagline ────────────────────────────────
            Text(
                text = "Secure. Private. Connected.",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Sub-tagline explaining zero-trust model ──
            Text(
                text = "Find contacts by Zixo Number only",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Profile Setup (new users) ───────────────
            AnimatedVisibility(
                visible = uiState.isProfileSetupNeeded,
                enter = fadeIn() + expandVertically() + slideInVertically(),
                exit = fadeOut() + shrinkVertically() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .liquidGlassContainer()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Up Your Profile",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Zixo Number and username have been generated automatically.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassOutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = onDisplayNameChange,
                        label = { Text("Display Name") },
                        placeholder = { Text("Enter your display name") },
                        maxLength = 50,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onProfileSetupSubmit()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onProfileSetupSubmit()
                        },
                        enabled = !uiState.isLoading && uiState.displayName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMint,
                            contentColor = BackgroundGradientStart,
                            disabledContainerColor = NeonMint.copy(alpha = 0.5f),
                            disabledContentColor = BackgroundGradientStart.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = BackgroundGradientStart,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Google Sign-In Button (main auth flow) ──
            AnimatedVisibility(
                visible = !uiState.isProfileSetupNeeded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Continue with Google Button ─────────
                    Button(
                        onClick = onGoogleSignIn,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMint,
                            contentColor = BackgroundGradientStart,
                            disabledContainerColor = NeonMint.copy(alpha = 0.5f),
                            disabledContentColor = BackgroundGradientStart.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = BackgroundGradientStart,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = BackgroundGradientStart
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Security Note ───────────────────────
                    Text(
                        text = "Your Zixo Number and @username are generated automatically.\nNo one can search you by name — only by your 8-digit number.",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // ── Error Message ──────────────────────────
            AnimatedVisibility(
                visible = !uiState.errorMessage.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.errorMessage?.let { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DestructiveBackground,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = message,
                            color = DestructiveText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
