package com.zixo.app.ui.auth

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.AuthState
import com.zixo.app.ui.theme.*

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onAuthComplete: () -> Unit,
    onEmailSignIn: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // Navigate when authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthComplete()
        }
    }

    // Animated background particles
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val blobOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )
    val blobOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )

    // Logo pulse animation
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
            .imePadding()
    ) {
        // Animated ambient blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (blobOffset1 * 100).dp - 50.dp, y = (blobOffset1 * 80).dp - 40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1A00E676), Color.Transparent)
                    )
                )
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (blobOffset2 * -80).dp + 100.dp, y = (blobOffset2 * 100).dp + 200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0D05C46B), Color.Transparent)
                    )
                )
                .align(Alignment.BottomEnd)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ZixoAccentDark, ZixoAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "Zixo",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ZixoTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure messaging & calls",
                fontSize = 16.sp,
                color = ZixoTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Error message
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ZixoErrorBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = ZixoError,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // Google Sign-In Button
            val activity = context as? Activity
            Button(
                onClick = {
                    if (!isLoading && activity != null) {
                        MainScope().launch {
                            authViewModel.signInWithGoogle(activity = activity)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZixoSurface,
                    contentColor = ZixoTextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ZixoAccent,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (isLoading) "Signing in..." else "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = GlassBorder,
                    thickness = 1.dp
                )
                Text(
                    text = "OR",
                    color = ZixoTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = GlassBorder,
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign in with Email Button
            OutlinedButton(
                onClick = {
                    if (!isLoading) {
                        authViewModel.clearError()
                        onEmailSignIn()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = ZixoAccent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZixoAccent),
                enabled = !isLoading
            ) {
                Icon(
                    Icons.Filled.Email,
                    contentDescription = "Email",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in with Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Terms
            Text(
                text = "By continuing, you agree to Zixo's\nTerms of Service & Privacy Policy",
                fontSize = 12.sp,
                color = ZixoTextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
