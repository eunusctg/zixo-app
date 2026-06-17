package com.zixo.app.ui.screens.auth

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.remember
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

/** Inline field error color. */
private val FieldErrorColor = Color(0xFFFF5252)

// ════════════════════════════════════════════════════════════════
// AuthScreen — Premium Liquid Glass Authentication
// ════════════════════════════════════════════════════════════════

/**
 * Kinetic Auth Page — iOS Liquid Glass Design.
 *
 * ## Visual Specifications:
 * - **Core backdrop**: Vertical gradient from `#07191C` (midnight slate teal)
 *   to `#050C0E` (dark charcoal), matching the Zixo brand dark theme.
 * - **Soft blurred radial blobs**: Two semi-transparent radial gradients
 *   (NeonMint at 8% alpha, deep teal at 12% alpha) blurred for depth.
 * - **Brand icon**: Strict squircle filled with Neon Green, housing a
 *   centered solid block-serif `"Z"`.
 * - **Title**: Bold white `"Zixo"` at `38.sp`.
 * - **Subtitle**: Muted `"Secure messaging & calls"` at `15.sp`.
 *
 * ## Progressive Disclosure:
 * - **Default state**: Frosted `"Continue with Google"` button with glass styling.
 *   The button is dimmed on non-GMS devices and shows a disabled visual state.
 * - **OR divider**: Thin horizontal lines flanking centered `"OR"` text.
 * - **Toggle link**: `"Login using email and password"` text button.
 * - **Tapped**: Toggles `uiState.isEmailFormVisible`, which animates the
 *   email/password form via `AnimatedVisibility` with
 *   `expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn()`.
 *
 * ## Sign-In vs Sign-Up:
 * - When `isSignUpMode = false`: email + password fields, "Sign in with Email" button.
 * - When `isSignUpMode = true`: email + password + display name fields,
 *   "Create Account" button. The display name is collected *inline* so there
 *   is no separate profile-setup dialog after sign-up — this removes the
 *   previous race condition where the dialog appeared briefly before
 *   navigation to Home dismissed it.
 *
 * ## Navigation:
 * - Post-login navigation is handled entirely by `ZixoNavHost`'s
 *   `LaunchedEffect(authState)` observer — this screen never calls
 *   navigation controllers directly, avoiding duplicate nav events.
 *
 * ## Layout hygiene:
 * - SnackbarHost is placed in the outer Box as an overlay (NOT inside the
 *   scrollable Column) so the snackbar is always visible above the keyboard.
 * - The scrollable Column uses `Arrangement.Top` with top padding — NOT
 *   `Arrangement.Center` — because `Center + scroll` causes content to
 *   be pushed off-screen on small devices.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── GMS availability check (runs once on first composition) ──────────
    LaunchedEffect(Unit) {
        activity?.let { viewModel.checkGmsAvailability(it) }
    }

    // ── One-shot UI events (errors, info) → snackbar ─────────────────────
    // Channel-based so events are consumed exactly once and survive
    // recomposition + configuration changes safely.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is AuthUiEvent.ShowError -> event.message
                is AuthUiEvent.ShowInfo -> event.message
            }
            snackbarHostState.showSnackbar(message)
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── Soft blurred radial blobs for depth ──────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            // Mint radial blob — upper-left
            // NOTE: Use offset() — NOT padding() — to push decorative blobs
            // off-screen. Compose's padding() throws IllegalArgumentException
            // for negative values, which crashed the auth screen on launch.
            // offset() accepts negative values and moves the element without
            // touching its internal layout bounds.
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = 60.dp)
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
                    .offset(x = 40.dp, y = 220.dp)
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
        // MAIN CONTENT COLUMN — Scrollable, top-aligned
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Top alignment — using `Center` with `verticalScroll` causes
            // content to be pushed off-screen on small devices.
            verticalArrangement = Arrangement.Top
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
                    if (uiState.isGmsAvailable && activity != null && !uiState.isGoogleLoading) {
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
                    disabledContainerColor = GlassButtonBackground,
                    disabledContentColor = TextSecondary
                ),
                border = BorderStroke(1.dp, GlassButtonBorder),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = uiState.isGmsAvailable && !uiState.isGoogleLoading
            ) {
                if (uiState.isGoogleLoading) {
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
                onClick = { viewModel.toggleEmailForm() }
            ) {
                Text(
                    text = if (uiState.isEmailFormVisible) "Hide email login"
                           else "Login using email and password",
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
                visible = uiState.showGmsNotice,
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
                            color = NeonMint
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

            if (uiState.showGmsNotice) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ══════════════════════════════════════════════════════════
            // PROGRESSIVE DISCLOSURE: Email + Password (+ Display Name) Fields
            // AnimatedVisibility with spring(stiffness = StiffnessLow)
            // for smooth, responsive physical motion on any form factor.
            // ══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = uiState.isEmailFormVisible,
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
                    // ── Display Name Field (sign-up mode only) ─────────
                    AnimatedVisibility(
                        visible = uiState.isSignUpMode,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                isError = uiState.displayNameError != null,
                                supportingText = uiState.displayNameError?.let {
                                    { Text(it, color = FieldErrorColor, fontSize = 12.sp) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = authFieldColors(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

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
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError?.let {
                            { Text(it, color = FieldErrorColor, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
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
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let {
                            { Text(it, color = FieldErrorColor, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.isSignUpMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!uiState.isSignUpMode) {
                                    keyboardController?.hide()
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
                            if (uiState.isSignUpMode) {
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
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        enabled = !uiState.isEmailLoading
                    ) {
                        if (uiState.isEmailLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AmoledBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.isSignUpMode) "Create Account"
                                       else "Sign in with Email",
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
                        onClick = { viewModel.toggleSignUpMode() }
                    ) {
                        Text(
                            text = if (uiState.isSignUpMode) {
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

            // Bottom spacing — accounts for keyboard + nav bar
            Spacer(modifier = Modifier.height(48.dp))
        }

        // ══════════════════════════════════════════════════════════════════
        // SNACKBAR HOST — Overlay on the outer Box (NOT inside the Column)
        // Placing it inside the scrollable Column caused the snackbar to be
        // rendered off-screen. Here it overlays the whole screen and is
        // always visible above the keyboard.
        // ══════════════════════════════════════════════════════════════════
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Helper: Auth field colors
// ════════════════════════════════════════════════════════════════

/**
 * Shared color configuration for all auth OutlinedTextFields.
 * Centralising this keeps the field styling consistent and makes
 * future visual tweaks a one-line change.
 */
@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
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
)

// ════════════════════════════════════════════════════════════════
// Helper: Find the host Activity from a Context
// ════════════════════════════════════════════════════════════════

/**
 * Walks up the [Context] wrapper chain to find the underlying [Activity].
 *
 * `LocalContext.current` in a Compose hierarchy is usually the Activity
 * itself, but it can also be a `ContextWrapper` (e.g. themed context).
 * This helper unwraps safely and returns null if no Activity is found
 * (e.g. in some preview/test scenarios).
 */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

