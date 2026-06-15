package com.zixo.app.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.ui.theme.AmoledBlack
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ════════════════════════════════════════════════════════════════
// Color Constants — Auth Screen Visual Brand
// ════════════════════════════════════════════════════════════════

/** Core Midnight Slate gradient start — deep dark teal. */
private val GradientStart = Color(0xFF07191C)

/** Core Midnight Slate gradient end — near-black charcoal. */
private val GradientEnd = Color(0xFF050C0E)

/** Frosted glass button background — rgba(255,255,255,0.07). */
private val GlassButtonBackground = Color(0x12FFFFFF)

/** Frosted glass border — rgba(255,255,255,0.15). */
private val GlassButtonBorder = Color(0x26FFFFFF)

/** Muted subtitle text color. */
private val SubtitleMuted = Color(0xFFA1B0B3)

/** OR divider text color. */
private val OrDividerText = Color(0xFF6878A0)

/** Email/password toggle link color. */
private val ToggleLinkColor = Color(0xFF5B8DB8)

/** Fallback notice card background — frosted glass. */
private val FallbackCardBackground = Color(0x1A1A2A32)

/** Fallback notice card border. */
private val FallbackCardBorder = Color(0x33FFFFFF)

/** Neon accent for notice text. */
private val NoticeAccentColor = NeonMint

/** Inline field error color. */
private val FieldErrorColor = Color(0xFFFF5252)

/** Google button disabled overlay. */
private val DisabledOverlay = Color(0x40FFFFFF)

// ════════════════════════════════════════════════════════════════
// AuthScreen — Premium Liquid Glass Authentication
// ════════════════════════════════════════════════════════════════

/**
 * Kinetic Auth Page — iOS Liquid Glass Design Overhaul.
 *
 * ## Visual Specifications:
 * - **Core backdrop**: Vertical gradient from `#07191C` (midnight slate teal)
 *   to `#050C0E` (dark charcoal), matching the Zixo brand dark theme.
 * - **Soft blurred radial blobs**: Two semi-transparent radial gradients
 *   (NeonMint at 8% alpha, deep teal at 12% alpha) positioned in the upper
 *   third of the screen and blurred at 60–80 dp for deep visual dimensionality.
 * - **Brand icon**: Strict squircle (`RoundedCornerShape(38.dp)`) filled with
 *   Neon Green (`#00E676`), housing a centered solid black block-serif `"Z"`.
 * - **Title**: Bold white `"Zixo"` at `38.sp`.
 * - **Subtitle**: Muted `"Secure messaging & calls"` in `#A1B0B3` at `15.sp`.
 *
 * ## Hyper-Animated Progressive Disclosure:
 * - **Default state**: Frosted `"Continue with Google"` button with glass styling.
 *   The button is dimmed on non-GMS devices and shows a disabled visual state.
 * - **OR divider**: Thin horizontal lines flanking centered `"OR"` text.
 * - **Toggle link**: `"Login using email and password"` text button.
 * - **Tapped**: Toggles a localized state variable (`showEmailForm`), which
 *   wraps the email/password form in `AnimatedVisibility` with
 *   `expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn()`.
 *   This guarantees smooth, responsive physical motion on any screen form factor.
 * - **Auto-fallback**: If no Google account is detected, `showEmailFallback`
 *   from the ViewModel automatically expands the email form and shows an
 *   inline frosted notice banner explaining the situation.
 *
 * ## Adaptive Inline Notice Banner:
 * - A frosted glass card with neon accent text:
 *   "No Google account found on this device. Please sign in with Email instead,
 *   or add a Google account in your device Settings."
 * - Only appears when `showEmailFallback = true` (triggered by GMS absence
 *   or NoCredentialException).
 *
 * ## Navigation:
 * - Post-login navigation is handled entirely by `ZixoNavHost`'s
 *   `LaunchedEffect(authState)` observer — this screen never calls
 *   navigation controllers directly, avoiding duplicate nav events.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── GMS availability check (runs once on first composition) ──────────
    LaunchedEffect(Unit) {
        if (activity != null) {
            viewModel.checkGmsAvailability(activity)
        }
    }

    // ── Progressive disclosure state ─────────────────────────────────────
    // The email form is visible when EITHER:
    //   1. The user manually tapped the toggle link (isEmailSignIn = true), OR
    //   2. The system auto-triggered the fallback (showEmailFallback = true)
    var showEmailForm by remember { mutableStateOf(false) }

    // Sync the ViewModel's fallback flag into the local visibility state.
    // This ensures the form auto-opens when Google Sign-In is unavailable
    // but also respects the user's manual toggle.
    LaunchedEffect(uiState.showEmailFallback) {
        if (uiState.showEmailFallback) {
            showEmailForm = true
        }
    }

    // ── Error snackbar ───────────────────────────────────────────────────
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ROOT CONTAINER — Core Midnight Slate Gradient Backdrop
    // ══════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 2000f)
                )
            )
            .systemBarsPadding()
            .imePadding()
    ) {
        // ── Soft blurred radial blobs for depth ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp)
        ) {
            // Mint radial blob — upper-left
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = 40.dp)
                    .blur(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonMint.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Deep teal radial blob — upper-right
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = 160.dp)
                    .blur(60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A5C6E).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // MAIN CONTENT COLUMN — Scrollable
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ── Brand Icon: Squircle with "Z" ────────────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(38.dp))
                    .background(NeonMint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = AmoledBlack,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Title ────────────────────────────────────────────────
            Text(
                text = "Zixo",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── Subtitle ─────────────────────────────────────────────
            Text(
                text = "Secure messaging & calls",
                fontSize = 15.sp,
                color = SubtitleMuted,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ══════════════════════════════════════════════════════════
            // CONTINUE WITH GOOGLE — Frosted Liquid Glass Button
            // ══════════════════════════════════════════════════════════
            Button(
                onClick = {
                    if (uiState.isGmsAvailable && activity != null && !uiState.isLoading) {
                        keyboardController?.hide()
                        viewModel.signInWithGoogle(activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassButtonBackground,
                    contentColor = TextPrimary,
                    disabledContainerColor = GlassButtonBackground
                ),
                border = BorderStroke(1.dp, GlassButtonBorder),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = uiState.isGmsAvailable && !uiState.isLoading
            ) {
                if (uiState.isLoading && !uiState.isEmailSignIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NeonMint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isGmsAvailable) "Continue with Google"
                               else "Google Sign-In unavailable",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.isGmsAvailable) TextPrimary else TextSecondary
                    )
                }
            }

            // ── Non-GMS disabled overlay explanation ─────────────────
            if (!uiState.isGmsAvailable) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This device doesn't support Google Sign-In",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // OR DIVIDER
            // ══════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0x1AFFFFFF),
                    thickness = 1.dp
                )
                Text(
                    text = "  OR  ",
                    color = OrDividerText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0x1AFFFFFF),
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // EMAIL LOGIN TOGGLE LINK
            // ══════════════════════════════════════════════════════════
            TextButton(
                onClick = {
                    showEmailForm = !showEmailForm
                    if (showEmailForm) {
                        viewModel.setEmailSignIn(true)
                    }
                }
            ) {
                Text(
                    text = if (showEmailForm) "Hide email login" else "Login using email and password",
                    color = ToggleLinkColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ══════════════════════════════════════════════════════════
            // ADAPTIVE INLINE NOTICE BANNER
            // (Only shown when Google Sign-In is unavailable on the device)
            // ══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = uiState.showEmailFallback,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FallbackCardBackground)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(FallbackCardBorder, FallbackCardBorder.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "No Google account found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NoticeAccentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No Google account found on this device. " +
                                    "Please sign in with Email instead, or add a " +
                                    "Google account in your device Settings.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (uiState.showEmailFallback) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ══════════════════════════════════════════════════════════
            // PROGRESSIVE DISCLOSURE: Email + Password Fields
            // AnimatedVisibility with spring(stiffness = StiffnessLow)
            // for smooth, responsive physical motion on any form factor.
            // ══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = showEmailForm,
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Email Field ───────────────────────────────────
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email", color = TextSecondary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        isError = uiState.emailValidationError != null,
                        supportingText = uiState.emailValidationError?.let {
                            { Text(it, color = FieldErrorColor, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            errorBorderColor = FieldErrorColor,
                            focusedContainerColor = Color(0x0DFFFFFF),
                            unfocusedContainerColor = Color(0x08FFFFFF),
                            errorContainerColor = Color(0x0DFF5252),
                            cursorColor = NeonMint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            errorTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Password Field ────────────────────────────────
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password", color = TextSecondary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        isError = uiState.passwordValidationError != null,
                        supportingText = uiState.passwordValidationError?.let {
                            { Text(it, color = FieldErrorColor, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            errorBorderColor = FieldErrorColor,
                            focusedContainerColor = Color(0x0DFFFFFF),
                            unfocusedContainerColor = Color(0x08FFFFFF),
                            errorContainerColor = Color(0x0DFF5252),
                            cursorColor = NeonMint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            errorTextColor = TextPrimary
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (uiState.isEmailSignUp) {
                                    viewModel.signUpWithEmail()
                                } else {
                                    viewModel.signInWithEmail()
                                }
                            }
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ══════════════════════════════════════════════════
                    // SIGN IN / SIGN UP BUTTON — Glowing Neon Green
                    // ══════════════════════════════════════════════════
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (uiState.isEmailSignUp) {
                                viewModel.signUpWithEmail()
                            } else {
                                viewModel.signInWithEmail()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMint,
                            contentColor = AmoledBlack,
                            disabledContainerColor = NeonMint.copy(alpha = 0.4f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AmoledBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.isEmailSignUp) "Create Account" else "Sign in with Email",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmoledBlack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ══════════════════════════════════════════════════
                    // TOGGLE SIGN IN / SIGN UP
                    // ══════════════════════════════════════════════════
                    TextButton(
                        onClick = { viewModel.toggleEmailSignUpMode() }
                    ) {
                        Text(
                            text = if (uiState.isEmailSignUp) {
                                "Already have an account? Sign in"
                            } else {
                                "Don't have an account? Sign up"
                            },
                            color = ToggleLinkColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Profile Setup Dialog (new users after sign-up) ──────
            AnimatedVisibility(
                visible = uiState.isProfileSetupNeeded,
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose a display name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This is how others will see you on Zixo",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = { viewModel.onDisplayNameChange(it) },
                        label = { Text("Display name", color = TextSecondary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x0DFFFFFF),
                            unfocusedContainerColor = Color(0x08FFFFFF),
                            cursorColor = NeonMint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.setDisplayNameAndContinue(uiState.displayName)
                            }
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.setDisplayNameAndContinue(uiState.displayName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMint,
                            contentColor = AmoledBlack
                        ),
                        enabled = !uiState.isLoading && uiState.displayName.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AmoledBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Continue",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmoledBlack
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Snackbar ────────────────────────────────────────────
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
