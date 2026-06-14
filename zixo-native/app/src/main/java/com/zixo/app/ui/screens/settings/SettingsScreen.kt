package com.zixo.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Cache
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zixo.app.ui.navigation.ZixoRoutes
import com.zixo.app.domain.model.AutoDownloadMedia
import com.zixo.app.domain.model.DefaultCallType
import com.zixo.app.domain.model.FontSize
import com.zixo.app.domain.model.LastSeenVisibility
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.SegmentedPicker
import com.zixo.app.ui.components.SwitchItem
import com.zixo.app.ui.components.ZixoNumberBadge
import com.zixo.app.ui.components.ZixoTopBar

// ──────────────────────────────────────────────────────────────────────────────
// Color constants matching the project design system
// ──────────────────────────────────────────────────────────────────────────────

private val BackgroundGradientStart = Color(0xFF0B1519)
private val BackgroundGradientEnd = Color(0xFF111E24)
private val CardBackground = Color(0xFF1A2A32)
private val AccentGreen = Color(0xFF00E676)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF90A4AE)
private val DestructiveBackground = Color(0xFF4A1515)
private val DestructiveText = Color(0xFFFF5252)

// ──────────────────────────────────────────────────────────────────────────────
// Spacing constants
// ──────────────────────────────────────────────────────────────────────────────

private val ItemVerticalSpacing = 4.dp
private val SectionVerticalSpacing = 16.dp
private val CardCornerRadius = 12.dp
private val CardHorizontalPadding = 16.dp

// ──────────────────────────────────────────────────────────────────────────────
// Main Settings Screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
    )

    Scaffold(
        topBar = {
            ZixoTopBar(
                title = "Settings",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)
            ) {
                // ── A. Header / User Profile Section ──────────────────────
                item { UserProfileSection(uiState = uiState, onEditProfile = { navController.navigate(ZixoRoutes.EDIT_PROFILE) }) }
                item { Spacer(modifier = Modifier.height(SectionVerticalSpacing)) }

                // ── B. Appearance Section ─────────────────────────────────
                item { SectionHeader(title = "Appearance") }
                item { AppearanceSection(uiState = uiState, viewModel = viewModel, onWallpaperClick = { navController.navigate(ZixoRoutes.CHAT_WALLPAPER) }) }
                item { Spacer(modifier = Modifier.height(SectionVerticalSpacing)) }

                // ── C. Privacy & Security Section ─────────────────────────
                item { SectionHeader(title = "Privacy & Security") }
                item { PrivacySecuritySection(uiState = uiState, viewModel = viewModel, onEncryptionKeyClick = { navController.navigate(ZixoRoutes.ENCRYPTION_KEY) }, onBlockedContactsClick = { navController.navigate(ZixoRoutes.BLOCKED_CONTACTS) }) }
                item { Spacer(modifier = Modifier.height(SectionVerticalSpacing)) }

                // ── D. Notifications Section ──────────────────────────────
                item { SectionHeader(title = "Notifications") }
                item { NotificationsSection(uiState = uiState, viewModel = viewModel, onNotificationToneClick = { navController.navigate(ZixoRoutes.NOTIFICATION_TONE) }) }
                item { Spacer(modifier = Modifier.height(SectionVerticalSpacing)) }

                // ── E. Data & Storage Section ─────────────────────────────
                item { SectionHeader(title = "Data & Storage") }
                item { DataStorageSection(uiState = uiState, viewModel = viewModel) }
                item { Spacer(modifier = Modifier.height(SectionVerticalSpacing)) }

                // ── F. Call Settings Section ──────────────────────────────
                item { SectionHeader(title = "Call Settings") }
                item { CallSettingsSection(uiState = uiState, viewModel = viewModel) }
                item { Spacer(modifier = Modifier.height(SectionVerticalSpacing)) }

                // ── G. About & Lifecycle Management Section ───────────────
                item { SectionHeader(title = "About") }
                item { AboutSection(uiState = uiState, onAdvancedNetworkClick = { navController.navigate(ZixoRoutes.ADVANCED_NETWORK) }, onEnterpriseSecurityClick = { navController.navigate(ZixoRoutes.ADVANCED_SECURITY) }, onAdvancedDataClick = { navController.navigate(ZixoRoutes.ADVANCED_DATA) }, onLogoutClick = { viewModel.showLogoutDialog() }, onDeleteAccountClick = { viewModel.showDeleteDialog() }) }
            }

            // ── Logout Confirmation Dialog ───────────────────────────────
            if (uiState.showLogoutDialog) {
                LogoutConfirmationDialog(
                    isLoggingOut = uiState.isLoggingOut,
                    onConfirm = { viewModel.logout() },
                    onDismiss = { viewModel.dismissLogoutDialog() }
                )
            }

            // ── Delete Account Double-Confirmation Dialog ────────────────
            if (uiState.showDeleteDialog) {
                DeleteAccountDialog(
                    isDeleting = uiState.isDeletingAccount,
                    onConfirm = { viewModel.deleteAccount() },
                    onDismiss = { viewModel.dismissDeleteDialog() }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// A. User Profile Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserProfileSection(
    uiState: SettingsUiState,
    onEditProfile: () -> Unit
) {
    val user = uiState.currentUser

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardHorizontalPadding, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar - clickable to edit profile
            AvatarComponent(
                imageUrl = user.photoUrl,
                name = user.displayName,
                size = 80.dp,
                isOnline = user.isOnline,
                modifier = Modifier.clickable(onClick = onEditProfile)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display Name
            Text(
                text = user.displayName.ifBlank { "Unknown User" },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Username
            if (user.username.isNotBlank()) {
                Text(
                    text = "@${user.username}",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            // Bio/Status
            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = user.bio,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Zixo Number Badge
            if (user.zixoNumber.isNotBlank()) {
                ZixoNumberBadge(zixoNumber = user.zixoNumber)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QR Code Shortcut Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBackground.copy(alpha = 0.5f))
                    .clickable { /* Navigate to QR code screen */ }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "QR Code",
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "My QR Code",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// B. Appearance Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onWallpaperClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)) {
        // Theme Selector
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = 14.dp)) {
                Text(
                    text = "Theme",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedPicker(
                    options = listOf("Dark", "AMOLED", "System"),
                    selectedIndex = uiState.themeMode.toSegmentIndex(),
                    onOptionSelected = { index ->
                        viewModel.setThemeMode(ThemeMode.fromSegmentIndex(index))
                    }
                )
            }
        }

        // Chat Wallpaper
        NavigationItem(
            title = "Chat Wallpaper",
            icon = Icons.Filled.Wallpaper,
            onClick = onWallpaperClick
        )

        // Font Size
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = 14.dp)) {
                Text(
                    text = "Font Size",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedPicker(
                    options = listOf("Small", "Medium", "Large"),
                    selectedIndex = uiState.fontSize.toSegmentIndex(),
                    onOptionSelected = { index ->
                        viewModel.setFontSize(FontSize.fromSegmentIndex(index))
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// C. Privacy & Security Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrivacySecuritySection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onEncryptionKeyClick: () -> Unit,
    onBlockedContactsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)) {
        // Last Seen Visibility
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = 14.dp)) {
                Text(
                    text = "Last Seen Visibility",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedPicker(
                    options = listOf("Everyone", "Contacts", "Nobody"),
                    selectedIndex = uiState.lastSeenVisibility.toSegmentIndex(),
                    onOptionSelected = { index ->
                        viewModel.setLastSeenVisibility(LastSeenVisibility.fromSegmentIndex(index))
                    }
                )
            }
        }

        // Online Status
        SwitchItem(
            title = "Online Status",
            checked = uiState.onlineStatusEnabled,
            onCheckedChange = { viewModel.setOnlineStatusEnabled(it) },
            icon = Icons.Filled.RemoveRedEye
        )

        // Read Receipts
        SwitchItem(
            title = "Read Receipts",
            checked = uiState.readReceiptsEnabled,
            onCheckedChange = { viewModel.setReadReceiptsEnabled(it) },
            icon = Icons.Filled.VisibilityOff
        )

        // Screen Lock
        SwitchItem(
            title = "Screen Lock",
            checked = uiState.screenLockEnabled,
            onCheckedChange = { viewModel.setScreenLockEnabled(it) },
            icon = Icons.Filled.Lock
        )

        // Encryption Key
        NavigationItem(
            title = "Encryption Key",
            icon = Icons.Filled.EnhancedEncryption,
            onClick = onEncryptionKeyClick
        )

        // Blocked Contacts
        NavigationItem(
            title = "Blocked Contacts",
            icon = Icons.Filled.Block,
            onClick = onBlockedContactsClick
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// D. Notifications Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNotificationToneClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)) {
        // Message Preview
        SwitchItem(
            title = "Message Preview",
            checked = uiState.messagePreviewEnabled,
            onCheckedChange = { viewModel.setMessagePreviewEnabled(it) },
            icon = Icons.Filled.Notifications
        )

        // Notification Tone
        NavigationItem(
            title = "Notification Tone",
            icon = Icons.Filled.Notifications,
            onClick = onNotificationToneClick,
            subtitle = if (uiState.notificationTone.isNotBlank()) uiState.notificationTone else "Default"
        )

        // Do Not Disturb
        SwitchItem(
            title = "Do Not Disturb",
            checked = uiState.dndEnabled,
            onCheckedChange = { viewModel.setDndEnabled(it) },
            icon = Icons.Filled.Notifications
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// E. Data & Storage Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DataStorageSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)) {
        // Storage Usage
        NavigationItem(
            title = "Storage Usage",
            icon = Icons.Filled.Storage,
            onClick = { /* Navigate to storage detail */ },
            subtitle = uiState.storageInfo?.let { "${String.format("%.1f", it.totalMB)} MB" } ?: "Calculating…"
        )

        // Auto-Download Media
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = 14.dp)) {
                Text(
                    text = "Auto-Download Media",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedPicker(
                    options = listOf("Wi-Fi Only", "Cellular", "Never"),
                    selectedIndex = uiState.autoDownloadMedia.toSegmentIndex(),
                    onOptionSelected = { index ->
                        viewModel.setAutoDownloadMedia(AutoDownloadMedia.fromSegmentIndex(index))
                    }
                )
            }
        }

        // Clear Cache
        NavigationItem(
            title = if (uiState.isClearingCache) "Clearing Cache…" else "Clear Cache",
            icon = Icons.Filled.Cache,
            onClick = { viewModel.clearCache() }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// F. Call Settings Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CallSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)) {
        // Default Call Type
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = 14.dp)) {
                Text(
                    text = "Default Call Type",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedPicker(
                    options = listOf("Ask Every Time", "LiveKit SIP", "WebRTC Video"),
                    selectedIndex = uiState.defaultCallType.toSegmentIndex(),
                    onOptionSelected = { index ->
                        viewModel.setDefaultCallType(DefaultCallType.fromSegmentIndex(index))
                    }
                )
            }
        }

        // Noise Suppression
        SwitchItem(
            title = "Noise Suppression",
            checked = uiState.noiseSuppressionEnabled,
            onCheckedChange = { viewModel.setNoiseSuppressionEnabled(it) },
            icon = Icons.Filled.MicOff
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// G. About & Lifecycle Management Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(
    uiState: SettingsUiState,
    onAdvancedNetworkClick: () -> Unit,
    onEnterpriseSecurityClick: () -> Unit,
    onAdvancedDataClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemVerticalSpacing)) {
        // App Version
        NavigationItem(
            title = "App Version",
            icon = Icons.Filled.Info,
            onClick = { /* No-op, informational */ },
            subtitle = uiState.appVersion
        )

        // Mission Statement
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Secure, private, and seamless communication for everyone.",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardHorizontalPadding, vertical = 14.dp)
            )
        }

        // Advanced Network & Calling
        NavigationItem(
            title = "Advanced Network & Calling",
            icon = Icons.Filled.Call,
            onClick = onAdvancedNetworkClick
        )

        // Enterprise Security
        NavigationItem(
            title = "Enterprise Security",
            icon = Icons.Filled.Security,
            onClick = onEnterpriseSecurityClick
        )

        // Enhanced Data & Cloud Sync
        NavigationItem(
            title = "Enhanced Data & Cloud Sync",
            icon = Icons.Filled.CloudQueue,
            onClick = onAdvancedDataClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Log Out Button
        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(CardCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = DestructiveBackground,
                contentColor = DestructiveText
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = if (uiState.isLoggingOut) "Logging Out…" else "Log Out",
                color = DestructiveText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Delete Account Button
        TextButton(
            onClick = onDeleteAccountClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardCornerRadius)
        ) {
            Text(
                text = if (uiState.isDeletingAccount) "Deleting Account…" else "Delete Account",
                color = DestructiveText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Confirmation Dialogs
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LogoutConfirmationDialog(
    isLoggingOut: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoggingOut) onDismiss() },
        title = {
            Text(
                text = "Log Out",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to log out of your Zixo account?",
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoggingOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DestructiveBackground,
                    contentColor = DestructiveText
                )
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DestructiveText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Log Out", color = DestructiveText, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoggingOut
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = "Delete Account",
                color = DestructiveText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This action is permanent and cannot be undone.",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All your data including messages, call history, contacts, and account information will be permanently deleted from Zixo servers. You will not be able to recover this account or any associated data.",
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DestructiveBackground,
                    contentColor = DestructiveText
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DestructiveText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete Permanently", color = DestructiveText, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBackground,
        titleContentColor = DestructiveText,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Reusable card wrapper for section items that need custom content
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Enum ↔ Segmented Index mapping extensions
// ──────────────────────────────────────────────────────────────────────────────

private fun ThemeMode.toSegmentIndex(): Int = when (this) {
    ThemeMode.DARK -> 0
    ThemeMode.AMOLED -> 1
    ThemeMode.SYSTEM -> 2
}

private fun ThemeMode.Companion.fromSegmentIndex(index: Int): ThemeMode = when (index) {
    0 -> ThemeMode.DARK
    1 -> ThemeMode.AMOLED
    2 -> ThemeMode.SYSTEM
    else -> ThemeMode.SYSTEM
}

private fun FontSize.toSegmentIndex(): Int = when (this) {
    FontSize.SMALL -> 0
    FontSize.MEDIUM -> 1
    FontSize.LARGE -> 2
}

private fun FontSize.Companion.fromSegmentIndex(index: Int): FontSize = when (index) {
    0 -> FontSize.SMALL
    1 -> FontSize.MEDIUM
    2 -> FontSize.LARGE
    else -> FontSize.MEDIUM
}

private fun LastSeenVisibility.toSegmentIndex(): Int = when (this) {
    LastSeenVisibility.EVERYONE -> 0
    LastSeenVisibility.CONTACTS -> 1
    LastSeenVisibility.NOBODY -> 2
}

private fun LastSeenVisibility.Companion.fromSegmentIndex(index: Int): LastSeenVisibility = when (index) {
    0 -> LastSeenVisibility.EVERYONE
    1 -> LastSeenVisibility.CONTACTS
    2 -> LastSeenVisibility.NOBODY
    else -> LastSeenVisibility.EVERYONE
}

private fun AutoDownloadMedia.toSegmentIndex(): Int = when (this) {
    AutoDownloadMedia.WIFI_ONLY -> 0
    AutoDownloadMedia.CELLULAR -> 1
    AutoDownloadMedia.NEVER -> 2
}

private fun AutoDownloadMedia.Companion.fromSegmentIndex(index: Int): AutoDownloadMedia = when (index) {
    0 -> AutoDownloadMedia.WIFI_ONLY
    1 -> AutoDownloadMedia.CELLULAR
    2 -> AutoDownloadMedia.NEVER
    else -> AutoDownloadMedia.WIFI_ONLY
}

private fun DefaultCallType.toSegmentIndex(): Int = when (this) {
    DefaultCallType.ASK_EVERY_TIME -> 0
    DefaultCallType.LIVEKIT_SIP -> 1
    DefaultCallType.WEBRTC_VIDEO -> 2
}

private fun DefaultCallType.Companion.fromSegmentIndex(index: Int): DefaultCallType = when (index) {
    0 -> DefaultCallType.ASK_EVERY_TIME
    1 -> DefaultCallType.LIVEKIT_SIP
    2 -> DefaultCallType.WEBRTC_VIDEO
    else -> DefaultCallType.ASK_EVERY_TIME
}
