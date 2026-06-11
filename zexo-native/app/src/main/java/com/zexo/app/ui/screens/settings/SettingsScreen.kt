package com.zexo.app.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.zexo.app.data.model.User
import com.zexo.app.data.model.UserSettings
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Provide app context to ViewModel for DataStore access
    LaunchedEffect(Unit) {
        viewModel.setAppContext(context)
    }

    // Navigate to auth on logout success
    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Logout confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout? You'll need to sign in again to access your account.",
                    color = ZexoTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Logout", color = ZexoRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = ZexoTextSecondary)
                }
            },
            containerColor = ZexoSurface,
            titleContentColor = ZexoTextPrimary,
            textContentColor = ZexoTextSecondary
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
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
                // ── Profile Card ────────────────────────────────────────
                ProfileCard(
                    user = uiState.currentUser,
                    onClick = {
                        uiState.currentUser?.uid?.let { uid ->
                            navController.navigate(Screen.ProfileEdit.route)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Account Section ─────────────────────────────────────
                SettingsSectionHeader(title = "Account")
                SettingsCard {
                    SettingsItemClickable(
                        icon = Icons.Default.Person,
                        title = "Profile",
                        subtitle = "Edit your profile information",
                        onClick = {
                            uiState.currentUser?.uid?.let { uid ->
                                navController.navigate(Screen.ProfileEdit.route)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsItemClickable(
                        icon = Icons.Default.Lock,
                        title = "Privacy",
                        subtitle = "Control your privacy settings",
                        onClick = { /* Scroll to privacy section */ }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Lock",
                        subtitle = "Require biometric to open app",
                        checked = uiState.settings.biometricLock,
                        onCheckedChange = { viewModel.setBiometricLock(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Appearance Section ──────────────────────────────────
                SettingsSectionHeader(title = "Appearance")
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = if (uiState.settings.darkTheme) "Dark mode is on" else "Light mode is on",
                        checked = uiState.settings.darkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Notifications Section ───────────────────────────────
                SettingsSectionHeader(title = "Notifications")
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = if (uiState.settings.notifications) "You'll receive notifications" else "Notifications are disabled",
                        checked = uiState.settings.notifications,
                        onCheckedChange = { viewModel.setNotifications(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Privacy Section ─────────────────────────────────────
                SettingsSectionHeader(title = "Privacy")
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Default.Visibility,
                        title = "Online Status",
                        subtitle = if (uiState.settings.showOnlineStatus) "Others can see when you're online" else "Your online status is hidden",
                        checked = uiState.settings.showOnlineStatus,
                        onCheckedChange = { viewModel.setShowOnlineStatus(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.Schedule,
                        title = "Last Seen",
                        subtitle = if (uiState.settings.showLastSeen) "Others can see your last seen time" else "Your last seen is hidden",
                        checked = uiState.settings.showLastSeen,
                        onCheckedChange = { viewModel.setShowLastSeen(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.DoneAll,
                        title = "Read Receipts",
                        subtitle = if (uiState.settings.readReceipts) "Others can see when you read messages" else "Read receipts are disabled",
                        checked = uiState.settings.readReceipts,
                        onCheckedChange = { viewModel.setReadReceipts(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.Chat,
                        title = "Typing Indicators",
                        subtitle = if (uiState.settings.typingIndicators) "Others can see when you're typing" else "Typing indicators are hidden",
                        checked = uiState.settings.typingIndicators,
                        onCheckedChange = { viewModel.setTypingIndicators(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── About Section ───────────────────────────────────────
                SettingsSectionHeader(title = "About")
                SettingsCard {
                    SettingsInfoItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = uiState.appVersion
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Logout Button ───────────────────────────────────────
                LogoutButton(
                    isLoggingOut = uiState.isLoggingOut,
                    onClick = { showLogoutDialog = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Error Snack ─────────────────────────────────────────
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        containerColor = ZexoSurfaceLight,
                        contentColor = ZexoRed
                    ) {
                        Text(error, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Profile Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileCard(
    user: User?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ZexoSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GradientProfileStart, GradientProfileEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user?.avatar.isNullOrBlank()) {
                    Text(
                        text = user?.displayName?.take(1)?.uppercase() ?: "Z",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                } else {
                    AsyncImage(
                        model = user!!.avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name + Zixo number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.displayName ?: "Unknown",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user?.zixoNumber ?: "",
                    fontSize = 13.sp,
                    color = ZexoSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (!user?.username.isNullOrBlank()) {
                    Text(
                        text = user.username,
                        fontSize = 12.sp,
                        color = ZexoTextSecondary
                    )
                }
            }

            // Edit icon
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Edit",
                tint = ZexoTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Section header
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = ZexoPrimary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
//  Card container for settings items
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = ZexoSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Clickable settings item
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsItemClickable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ZexoPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ZexoTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ZexoTextSecondary
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

// ═══════════════════════════════════════════════════════════════════════
//  Toggle settings item
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ZexoPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ZexoTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ZexoTextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ZexoPrimary,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = ZexoSurfaceLight,
                uncheckedThumbColor = ZexoTextSecondary,
                checkedBorderColor = ZexoPrimary,
                uncheckedBorderColor = ZexoSurfaceLight
            ),
            modifier = Modifier.size(width = 48.dp, height = 28.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Info-only settings item (no interaction)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ZexoPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ZexoTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ZexoTextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Divider
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 52.dp),
        color = ZexoSurfaceLight,
        thickness = 0.5.dp
    )
}

// ═══════════════════════════════════════════════════════════════════════
//  Logout Button
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LogoutButton(
    isLoggingOut: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZexoRed.copy(alpha = 0.15f),
            contentColor = ZexoRed,
            disabledContainerColor = ZexoRed.copy(alpha = 0.08f)
        ),
        enabled = !isLoggingOut
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ZexoRed,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logging out...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        } else {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
