package com.zixo.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zixo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showPassword by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(currentUser) {
        if (currentUser != null) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ZIXO",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = ZixoPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Secure Messenger",
                fontSize = 16.sp,
                color = ZixoTextSecondary,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Button(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A2A32),
                    contentColor = ZixoText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                enabled = !uiState.isLoading
            ) {
                Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Continue with Google",
                    color = ZixoText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ZixoHighlight,
                    thickness = 1.dp
                )
                Text(
                    "  or  ",
                    color = ZixoTextSecondary,
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ZixoHighlight,
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = !uiState.isLoginMode,
                enter = fadeIn() + slideInVertically()
            ) {
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = viewModel::onDisplayNameChange,
                    label = { Text("Display Name", color = ZixoTextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZixoText,
                        unfocusedTextColor = ZixoText,
                        focusedBorderColor = ZixoPrimary,
                        unfocusedBorderColor = ZixoHighlight,
                        focusedLabelColor = ZixoPrimary,
                        cursorColor = ZixoPrimary,
                        focusedContainerColor = ZixoSurface,
                        unfocusedContainerColor = ZixoSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email", color = ZixoTextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ZixoText, unfocusedTextColor = ZixoText,
                    focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight,
                    focusedLabelColor = ZixoPrimary, cursorColor = ZixoPrimary,
                    focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password", color = ZixoTextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = ZixoTextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ZixoText, unfocusedTextColor = ZixoText,
                    focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight,
                    focusedLabelColor = ZixoPrimary, cursorColor = ZixoPrimary,
                    focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardActions = KeyboardActions(onDone = {
                    if (uiState.isLoginMode) viewModel.login() else viewModel.signUp()
                })
            )

            if (uiState.isLoginMode) {
                TextButton(onClick = { showForgotPassword = true }) {
                    Text("Forgot Password?", color = ZixoPrimary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (uiState.isLoginMode) viewModel.login() else viewModel.signUp() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZixoPrimary),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ZixoBg, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (uiState.isLoginMode) "Login" else "Sign Up",
                        color = ZixoBg,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                buildAnnotatedString {
                    append(if (uiState.isLoginMode) "Don't have an account? " else "Already have an account? ")
                    withStyle(SpanStyle(color = ZixoPrimary, fontWeight = FontWeight.Bold)) {
                        append(if (uiState.isLoginMode) "Sign Up" else "Login")
                    }
                },
                color = ZixoTextSecondary,
                modifier = Modifier.clickable { viewModel.toggleMode() }
            )

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(error, color = ZixoError, fontSize = 13.sp)
            }

            if (uiState.isPasswordResetSent) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Password reset email sent!", color = ZixoPrimary, fontSize = 13.sp)
            }
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Reset Password", color = ZixoText) },
            text = {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email", color = ZixoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZixoText, unfocusedTextColor = ZixoText,
                        focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight,
                        cursorColor = ZixoPrimary, focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resetPassword(); showForgotPassword = false }) {
                    Text("Send", color = ZixoPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel", color = ZixoTextSecondary)
                }
            },
            containerColor = ZixoSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
