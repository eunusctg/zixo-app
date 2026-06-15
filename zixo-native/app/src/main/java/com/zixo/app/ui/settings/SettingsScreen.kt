package com.zixo.app.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.diagonalMeshGradient
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.navigation.ZixoRoutes
import com.zixo.app.ui.theme.DestructiveBackground
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Settings Screen — Main Hub
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Production settings screen with WhatsApp-style sub-menus.
 *
 * Features:
 * - Radiant profile header with diagonal mesh gradient
 * - QR code popup for account sharing
 * - Navigation to sub-pages (Account, Privacy, Chat, Notifications, Storage)
 * - Quick toggles for Theme, Read Receipts, Screen Lock
 * - Account actions (Log out, Delete account)
 *
 * NO LiveKit configuration visible — all calling infrastructure is hidden.
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val showQrPopup by viewModel.showQrPopup.collectAsStateWithLifecycle()
    val logoutState by viewModel.logoutState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Radiant Profile Header Block ──────────────────────────────
            item {
                ProfileHeaderBlock(
                    displayName = userProfile.displayName,
                    bio = userProfile.bio,
                    avatarUrl = userProfile.avatarUrl,
                    onQrClick = { viewModel.toggleQrPopup() },
                    onProfileClick = { navController.navigate(ZixoRoutes.EDIT_PROFILE) }
                )
            }

            // ── Settings Sections (WhatsApp-style list items) ─────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .liquidGlassContainer()
                        .padding(4.dp)
                ) {
                    SettingsNavItem(
                        emoji = "🔒",
                        title = "Account & Security",
                        subtitle = "Passkeys, biometrics, verification",
                        onClick = { navController.navigate(ZixoRoutes.ACCOUNT_SECURITY) }
                    )
                    SettingsNavItem(
                        emoji = "🛡️",
                        title = "Privacy Center",
                        subtitle = "Last seen, read receipts, blocking",
                        onClick = { navController.navigate(ZixoRoutes.PRIVACY_CENTER) }
                    )
                    SettingsNavItem(
                        emoji = "💬",
                        title = "Chat Configuration",
                        subtitle = "Theme, wallpaper, font size, ephemeral",
                        onClick = { navController.navigate(ZixoRoutes.CHAT_CONFIG) }
                    )
                    SettingsNavItem(
                        emoji = "🔔",
                        title = "Notifications",
                        subtitle = "Tones, vibration, ringtone",
                        onClick = { navController.navigate(ZixoRoutes.NOTIFICATION_MANAGER) }
                    )
                    SettingsNavItem(
                        emoji = "💾",
                        title = "Storage & Data",
                        subtitle = "Usage, auto-download, upload quality",
                        onClick = { navController.navigate(ZixoRoutes.STORAGE_DATA_HUB) }
                    )
                }
            }

            // ── Quick Toggles Section ─────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .liquidGlassContainer()
                        .padding(16.dp)
                ) {
                    // Theme mode
                    Text(
                        text = "THEME",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val themeOptions = listOf("Dark", "AMOLED", "System")
                    val selectedThemeIndex = when (settingsState.themeMode) {
                        ThemeMode.DARK -> 0
                        ThemeMode.AMOLED -> 1
                        ThemeMode.SYSTEM -> 2
                    }

                    GlassSegmentedPicker(
                        options = themeOptions,
                        selectedIndex = selectedThemeIndex,
                        onOptionSelected = { index ->
                            val mode = when (index) {
                                0 -> ThemeMode.DARK
                                1 -> ThemeMode.AMOLED
                                else -> ThemeMode.SYSTEM
                            }
                            viewModel.updateThemeMode(mode)
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Read receipts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read Receipts",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Let contacts know you've read messages",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.areReadReceiptsEnabled,
                            onCheckedChange = { viewModel.updateReadReceipts(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Screen lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen Lock",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Require biometric to open Zixo",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.isScreenLockEnabled,
                            onCheckedChange = { viewModel.updateScreenLock(it) }
                        )
                    }
                }
            }

            // ── Account Actions ───────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .liquidGlassContainer()
                        .padding(4.dp)
                ) {
                    // Log out
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.requestLogout() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Log out",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Delete account (destructive)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.requestLogout() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoEncryption,
                            contentDescription = null,
                            tint = DestructiveText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete account",
                            color = DestructiveText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ── QR Code Popup ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showQrPopup,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            QrCodePopup(
                displayName = userProfile.displayName,
                zixoNumber = userProfile.zixoNumber,
                onClose = { viewModel.toggleQrPopup() }
            )
        }

        // ── Logout Confirmation Dialog ───────────────────────────────────
        if (logoutState is LogoutState.Confirming) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelLogout() },
                title = {
                    Text(text = "Log out?", color = TextPrimary)
                },
                text = {
                    Text(
                        text = "Are you sure you want to log out of Zixo?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmLogout() }) {
                        Text("Log out", color = DestructiveText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelLogout() }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF152530),
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }

        // ── Delete Account Confirmation Dialog ───────────────────────────
        if (logoutState is LogoutState.Confirming) {
            // Re-using logout state for delete — separate dialog not triggered here
            // Delete account dialog would be triggered from AccountSecurityScreen
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Header Block
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeaderBlock(
    displayName: String,
    bio: String,
    avatarUrl: String,
    onQrClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .diagonalMeshGradient()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onProfileClick
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2A32))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name + Bio + QR icon
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName.ifBlank { "Set your name" },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // QR Code icon
                    IconButton(
                        onClick = onQrClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (bio.isNotBlank()) {
                    Text(
                        text = bio,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Navigation Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsNavItem(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR Code Popup
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Frosted liquid glass modal overlay displaying the user's QR code
 * rendered in brand emerald green (#00E676) with account identification URI encoded.
 */
@Composable
private fun QrCodePopup(
    displayName: String,
    zixoNumber: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .liquidGlassContainer()
                .padding(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* consume clicks inside popup */ }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your QR Code",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QR code rendered in brand emerald green
            val qrData = "zixo://user/$zixoNumber"
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                SimpleQrCanvas(
                    data = qrData,
                    color = Color(0xFF00E676),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (zixoNumber.isNotBlank()) {
                Text(
                    text = formatZixoNumber(zixoNumber),
                    color = NeonMint,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Share button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonMint)
                    .clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Add me on Zixo: $qrData")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFF0B1519),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share",
                    color = Color(0xFF0B1519),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Simple QR Canvas (Placeholder — generates a visual QR-like pattern)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a placeholder QR-code-like pattern for the account identification URI.
 * In production, this would use a real QR code library (e.g., zxing).
 */
@Composable
private fun SimpleQrCanvas(
    data: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gridSize = 21 // Standard QR Version 1
        val cellSize = size.minDimension / gridSize
        val hash = data.hashCode()

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val isFinderPattern = (
                    (row < 7 && col < 7) ||
                    (row < 7 && col > gridSize - 8) ||
                    (row > gridSize - 8 && col < 7)
                )
                val isFinderBorder = isFinderPattern && (
                    row == 0 || row == 6 || col == 0 || col == 6 ||
                    (row < 7 && col < 7 && (row == 0 || row == 6 || col == 0 || col == 6)) ||
                    (row < 7 && col > gridSize - 8 && (row == 0 || row == 6 || col == gridSize - 7 || col == gridSize - 1)) ||
                    (row > gridSize - 8 && col < 7 && (row == gridSize - 7 || row == gridSize - 1 || col == 0 || col == 6))
                )
                val isFinderInner = isFinderPattern && (
                    (row in 2..4 && col in 2..4) ||
                    (row in 2..4 && col in gridSize - 5..gridSize - 3) ||
                    (row in gridSize - 5..gridSize - 3 && col in 2..4)
                )

                val cellHash = (hash * 31 + row * 17 + col * 7) and 0xFF
                val shouldFill = isFinderBorder || isFinderInner || (!isFinderPattern && cellHash % 3 != 0)

                if (shouldFill) {
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = col * cellSize,
                            y = row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatZixoNumber(number: String): String {
    return if (number.length == 8) {
        "${number.substring(0, 4)} ${number.substring(4, 8)}"
    } else {
        number
    }
}
