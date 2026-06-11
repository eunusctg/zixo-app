package com.zexo.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    uid: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    LaunchedEffect(uid) {
        viewModel.loadProfile(uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Gradient cover image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GradientProfileStart, GradientProfileEnd)
                            ),
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                            )
                ) {
                    // Decorative circles
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-30).dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-20).dp, y = 30.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                }

                // Avatar overlapping the gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-56).dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Avatar with online indicator
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(ZexoSurfaceLight)
                                .then(
                                    if (user?.avatar?.isNotBlank() == true) Modifier
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user?.avatar?.isNotBlank() == true) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user?.displayName?.take(1)?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp
                                )
                            }
                        }
                        // Online indicator
                        if (uiState.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ZexoBackground)
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(ZexoOnline)
                                )
                            }
                        }
                    }
                }

                // Name and info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-40).dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display name
                    Text(
                        text = user?.displayName ?: "Unknown",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ZexoTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Username
                    if (user?.username?.isNotBlank() == true) {
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ZexoPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Zixo number
                    if (user?.zixoNumber?.isNotBlank() == true) {
                        Text(
                            text = user.zixoNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZexoTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bio
                    if (user?.bio?.isNotBlank() == true) {
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZexoTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileActionButton(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "Message",
                            onClick = {
                                user?.uid?.let { otherUid ->
                                    viewModel.startChat(otherUid) { chatId ->
                                        navController.navigate(Screen.Chat.createRoute(chatId))
                                    }
                                }
                            }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.Call,
                            label = "Audio",
                            onClick = { /* TODO: Start audio call */ }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            onClick = { /* TODO: Start video call */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Contact info section
                    ProfileInfoSection(user, uiState.isOnline)
                }
            }
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ZexoPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = ZexoPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ZexoTextSecondary
        )
    }
}

@Composable
private fun ProfileInfoSection(
    user: com.zexo.app.data.model.User?,
    isOnline: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZexoSurface)
            .padding(20.dp)
    ) {
        Text(
            text = "Contact Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ZexoTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        if (user?.email?.isNotBlank() == true) {
            ProfileInfoRow(label = "Email", value = user.email)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Phone
        if (user?.phone?.isNotBlank() == true) {
            ProfileInfoRow(label = "Phone", value = user.phone)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Zixo Number
        if (user?.zixoNumber?.isNotBlank() == true) {
            ProfileInfoRow(label = "Zixo Number", value = user.zixoNumber)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Status
        ProfileInfoRow(
            label = "Status",
            value = if (isOnline) "Online" else "Offline"
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ZexoTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ZexoTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
