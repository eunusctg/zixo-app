package com.zixo.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.AuthState
import com.zixo.app.ui.theme.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onAuthComplete: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Navigate when authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Back button
            IconButton(
                onClick = {
                    authViewModel.clearError()
                    onBackToLogin()
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ZixoTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ZixoTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Register with your email to get started",
                fontSize = 15.sp,
                color = ZixoTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

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

            // Display Name field
            OutlinedTextField(
                value = displayName,
                onValueChange = { newName ->
                    if (newName.length <= 30) displayName = newName
                    authViewModel.clearError()
                },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = "Name", tint = ZixoTextTertiary)
                },
                supportingText = {
                    Text(
                        text = "${displayName.length}/30",
                        color = if (displayName.length >= 30) ZixoError else ZixoTextTertiary,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZixoAccent,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = ZixoAccent,
                    focusedTextColor = ZixoTextPrimary,
                    unfocusedTextColor = ZixoTextPrimary,
                    focusedLabelColor = ZixoAccent,
                    unfocusedLabelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { newEmail ->
                    email = newEmail
                    authViewModel.clearError()
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = "Email", tint = ZixoTextTertiary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZixoAccent,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = ZixoAccent,
                    focusedTextColor = ZixoTextPrimary,
                    unfocusedTextColor = ZixoTextPrimary,
                    focusedLabelColor = ZixoAccent,
                    unfocusedLabelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { newPassword ->
                    password = newPassword
                    authViewModel.clearError()
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Password", tint = ZixoTextTertiary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = ZixoTextTertiary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                supportingText = {
                    Text(
                        text = "At least 6 characters",
                        color = if (password.isNotEmpty() && password.length < 6) ZixoError else ZixoTextTertiary,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZixoAccent,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = ZixoAccent,
                    focusedTextColor = ZixoTextPrimary,
                    unfocusedTextColor = ZixoTextPrimary,
                    focusedLabelColor = ZixoAccent,
                    unfocusedLabelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password field
            val passwordsMatch = confirmPassword.isEmpty() || confirmPassword == password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { newConfirm ->
                    confirmPassword = newConfirm
                    authViewModel.clearError()
                },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Confirm Password", tint = ZixoTextTertiary)
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = ZixoTextTertiary
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Text(text = "Passwords do not match", color = ZixoError, fontSize = 11.sp)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZixoAccent,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = ZixoAccent,
                    focusedTextColor = ZixoTextPrimary,
                    unfocusedTextColor = ZixoTextPrimary,
                    focusedLabelColor = ZixoAccent,
                    unfocusedLabelColor = ZixoTextSecondary,
                    errorBorderColor = ZixoError
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Register button
            Button(
                onClick = {
                    if (!isLoading && passwordsMatch) {
                        MainScope().launch {
                            authViewModel.signUpWithEmail(email.trim(), password, displayName.trim())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZixoAccent,
                    contentColor = Color.Black
                ),
                enabled = !isLoading && passwordsMatch && password.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (isLoading) "Creating account..." else "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to login link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = ZixoTextSecondary,
                    fontSize = 14.sp
                )
                TextButton(onClick = {
                    authViewModel.clearError()
                    onBackToLogin()
                }) {
                    Text(
                        text = "Sign In",
                        color = ZixoAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
