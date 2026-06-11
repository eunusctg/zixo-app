package com.zexo.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavHostController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val totalUsers by viewModel.totalUsers.collectAsState()
    val activeUsers by viewModel.activeUsers.collectAsState()
    val totalChats by viewModel.totalChats.collectAsState()
    val totalCalls by viewModel.totalCalls.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val notificationText by viewModel.notificationText.collectAsState()
    val message by viewModel.message.collectAsState()

    // SnackBar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = ZexoBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = ZexoSurface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ZexoTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Panel",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZexoTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    // Admin badge
                    Surface(
                        color = ZexoPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ADMIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZexoPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ZexoBackground),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Stats Cards ────────────────────────────────────────
                item {
                    Text(
                        text = "Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Users",
                            value = totalUsers.toString(),
                            icon = Icons.Default.People,
                            color = ZexoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Active Now",
                            value = activeUsers.toString(),
                            icon = Icons.Default.Circle,
                            color = ZexoGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Chats",
                            value = totalChats.toString(),
                            icon = Icons.Default.Chat,
                            color = ZexoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Calls",
                            value = totalCalls.toString(),
                            icon = Icons.Default.Call,
                            color = ZexoSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Quick Actions ──────────────────────────────────────
                item {
                    Text(
                        text = "Quick Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoTextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Default.People,
                        title = "Manage Users",
                        subtitle = "View, edit and manage all users",
                        iconColor = ZexoPrimary
                    ) {
                        navController.navigate(Screen.AdminUsers.route)
                    }
                }

                item {
                    ActionCard(
                        icon = Icons.Default.Settings,
                        title = "App Settings",
                        subtitle = "Configure maintenance, updates & more",
                        iconColor = ZexoSecondary
                    ) {
                        navController.navigate(Screen.AdminSettings.route)
                    }
                }

                item {
                    ActionCard(
                        icon = Icons.Default.Web,
                        title = "Landing Page",
                        subtitle = "Edit hero section, features & CTA",
                        iconColor = ZexoOrange
                    ) {
                        navController.navigate(Screen.AdminLanding.route)
                    }
                }

                // ── Send Notification ──────────────────────────────────
                item {
                    Text(
                        text = "Send Notification",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoTextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    NotificationCard(
                        text = notificationText,
                        onTextChange = { viewModel.updateNotificationText(it) },
                        onSend = { viewModel.sendNotification() }
                    )
                }

                // ── View Reports ───────────────────────────────────────
                item {
                    ActionCard(
                        icon = Icons.Default.Analytics,
                        title = "View Reports",
                        subtitle = "Usage stats, growth & analytics",
                        iconColor = ZexoAccent
                    ) {
                        // TODO: Navigate to reports screen
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Stat Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ZexoSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = ZexoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ZexoTextPrimary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Action Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = ZexoSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = ZexoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ZexoTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Notification Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun NotificationCard(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZexoSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = ZexoOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Broadcast to all users",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ZexoTextSecondary
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Enter notification message…", color = ZexoTextSecondary)
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZexoPrimary,
                    unfocusedBorderColor = ZexoSurfaceLight,
                    focusedContainerColor = ZexoBackground,
                    unfocusedContainerColor = ZexoBackground,
                    focusedTextColor = ZexoTextPrimary,
                    unfocusedTextColor = ZexoTextPrimary,
                    cursorColor = ZexoPrimary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZexoOrange,
                        disabledContainerColor = ZexoSurfaceLight
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
