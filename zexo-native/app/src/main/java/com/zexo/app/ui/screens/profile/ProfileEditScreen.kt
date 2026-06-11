package com.zexo.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavHostController,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load profile on first composition
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // Navigate back on save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            navController.popBackStack()
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // TODO: Upload avatar to Firebase Storage and update profile
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ZexoTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ZexoPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = ZexoPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZexoBackground
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZexoPrimary, strokeWidth = 2.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ZexoBackground)
                    .verticalScroll(rememberScrollState())
            ) {
                // Gradient header with avatar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(GradientProfileStart, GradientProfileEnd)
                                ),
                                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                            )
                    )

                    // Avatar with camera button
                    Box(
                        modifier = Modifier
                            .padding(top = 84.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(ZexoSurfaceLight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.avatar.isNotBlank()) {
                                AsyncImage(
                                    model = uiState.avatar,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = uiState.displayName.take(1).uppercase().ifBlank { "?" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp
                                )
                            }
                        }

                        // Camera change button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ZexoPrimary)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change avatar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Zixo Number (read-only)
                if (uiState.zixoNumber.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Zixo Number",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZexoTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ZexoSurface,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = uiState.zixoNumber,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ZexoPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Read-only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZexoTextSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Editable fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Display Name
                    ProfileEditField(
                        label = "Display Name",
                        value = uiState.displayName,
                        onValueChange = { viewModel.updateDisplayName(it) },
                        placeholder = "Enter your display name",
                        singleLine = true
                    )

                    // Username
                    ProfileEditField(
                        label = "Username",
                        value = uiState.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        placeholder = "@username",
                        singleLine = true,
                        prefix = "@"
                    )

                    // Bio
                    ProfileEditField(
                        label = "Bio",
                        value = uiState.bio,
                        onValueChange = { viewModel.updateBio(it) },
                        placeholder = "Tell something about yourself...",
                        singleLine = false,
                        maxLines = 4
                    )

                    // Phone
                    ProfileEditField(
                        label = "Phone",
                        value = uiState.phone,
                        onValueChange = { viewModel.updatePhone(it) },
                        placeholder = "+1 234 567 8900",
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save button
                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZexoPrimary,
                        disabledContainerColor = ZexoPrimary.copy(alpha = 0.5f)
                    ),
                    enabled = !uiState.isSaving && uiState.displayName.isNotBlank()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Error message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = ZexoRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    prefix: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ZexoTextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = ZexoTextSecondary.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            },
            prefix = if (prefix != null) {
                {
                    Text(
                        text = prefix,
                        color = ZexoPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else null,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZexoPrimary,
                unfocusedBorderColor = ZexoSurfaceLight,
                focusedContainerColor = ZexoSurface,
                unfocusedContainerColor = ZexoSurface,
                focusedTextColor = ZexoTextPrimary,
                unfocusedTextColor = ZexoTextPrimary,
                focusedLabelColor = ZexoPrimary,
                unfocusedLabelColor = ZexoTextSecondary,
                cursorColor = ZexoPrimary
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
