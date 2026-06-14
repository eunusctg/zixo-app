package com.zixo.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zixo.app.domain.model.User
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.diagonalMeshGradient
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.navigation.ZixoRoutes
import com.zixo.app.ui.screens.settings.SettingsViewModel
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.DestructiveBackgroundAlpha
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import com.zixo.app.ui.theme.TextTertiary

// ════════════════════════════════════════════════════════════════
// Settings Screen — iOS Liquid Glass Dashboard
// ════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showQrModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated glass background
        ZixoGlassBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = 32.dp
            )
        ) {
            // ──────────────────────────────────────────────────────
            // 1. Profile Header Card
            // ──────────────────────────────────────────────────────
            item {
                ProfileHeaderCard(
                    user = uiState.currentUser,
                    onQrClick = { showQrModal = true },
                    onEditProfileClick = {
                        navController.navigate(ZixoRoutes.EDIT_PROFILE)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // ──────────────────────────────────────────────────────
            // 2. Settings Section Cards
            // ──────────────────────────────────────────────────────
            item {
                SectionLabel(text = "SETTINGS")
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                GlassSettingsCard {
                    GlassNavigationRow(
                        title = "Account Security",
                        icon = Icons.Filled.Shield,
                        subtitle = if (uiState.screenLockEnabled) "Screen lock enabled" else "Set up security",
                        onClick = { navController.navigate(ZixoRoutes.ADVANCED_SECURITY) }
                    )
                    GlassDivider()
                    GlassNavigationRow(
                        title = "Privacy",
                        icon = Icons.Filled.Lock,
                        subtitle = "Last seen, read receipts",
                        onClick = { /* PrivacyCenterScreen */ }
                    )
                    GlassDivider()
                    GlassNavigationRow(
                        title = "Chats",
                        icon = Icons.Filled.Call,
                        subtitle = "Theme, wallpaper, font size",
                        onClick = { /* ChatConfigScreen */ }
                    )
                    GlassDivider()
                    GlassNavigationRow(
                        title = "Notifications",
                        icon = Icons.Filled.Notifications,
                        subtitle = if (uiState.dndEnabled) "Do Not Disturb active" else "Message & call tones",
                        onClick = { /* NotificationManagerScreen */ }
                    )
                    GlassDivider()
                    GlassNavigationRow(
                        title = "Storage & Data",
                        icon = Icons.Filled.Info,
                        subtitle = uiState.storageInfo?.let {
                            "${String.format("%.1f", it.totalMB)} MB used"
                        } ?: "Manage storage",
                        onClick = { navController.navigate(ZixoRoutes.ADVANCED_DATA) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // ──────────────────────────────────────────────────────
            // 3. App Info
            // ──────────────────────────────────────────────────────
            item {
                Text(
                    text = "Zixo v1.0.0",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ──────────────────────────────────────────────────────
            // 4. Logout Button
            // ──────────────────────────────────────────────────────
            item {
                LogoutGlassButton(
                    isLoading = uiState.isLoading,
                    onClick = { viewModel.showLogoutDialog() }
                )
            }
        }

        // ──────────────────────────────────────────────────────
        // QR Code Modal Overlay
        // ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showQrModal,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            QrCodeModalOverlay(
                user = uiState.currentUser,
                onDismiss = { showQrModal = false }
            )
        }

        // ──────────────────────────────────────────────────────
        // Logout Confirmation Dialog
        // ──────────────────────────────────────────────────────
        if (uiState.showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = { viewModel.logOut() },
                onDismiss = { viewModel.dismissLogoutDialog() }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Profile Header Card — diagonalMeshGradient with avatar & info
// ════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeaderCard(
    user: User?,
    onQrClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .diagonalMeshGradient()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AvatarComponent(
                imageUrl = user?.photoUrl,
                name = user?.displayName ?: "",
                size = 80.dp,
                isOnline = user?.isOnline == true,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Display Name
                Text(
                    text = user?.displayName?.ifBlank { "Unknown" } ?: "Unknown",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Username
                if (!user?.username.isNullOrBlank()) {
                    Text(
                        text = "@${user.username}",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bio
                if (!user?.bio.isNullOrBlank()) {
                    Text(
                        text = user.bio!!,
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Zixo Number
                if (!user?.zixoNumber.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user.formattedZixoNumber,
                            color = NeonMint,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // Right-side action icons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // QR Code button
                IconButton(
                    onClick = onQrClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "Show QR Code",
                        tint = NeonMint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Edit profile chevron
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Profile",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// QR Code Modal Overlay
// ════════════════════════════════════════════════════════════════

@Composable
private fun QrCodeModalOverlay(
    user: User?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // Frosted glass panel
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .liquidGlassContainer()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks so they don't dismiss
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "My QR Code",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Zixo Number under title
            if (!user?.zixoNumber.isNullOrBlank()) {
                Text(
                    text = user.formattedZixoNumber,
                    color = NeonMint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QR Code image
            if (!user?.zixoNumber.isNullOrBlank()) {
                QrCodeImage(
                    data = "zixo://user/${user.zixoNumber}",
                    size = 220.dp,
                    qrColor = NeonMint,
                    backgroundColor = Color.Transparent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Share button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonMint.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = NeonMint.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        val zixoUri = "zixo://user/${user?.zixoNumber ?: ""}"
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Add me on Zixo! $zixoUri")
                            type = "text/plain"
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share QR Code")
                        )
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = NeonMint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share",
                    color = NeonMint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Close",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// QR Code Image Composable (brand emerald green on frosted glass)
// ════════════════════════════════════════════════════════════════

@Composable
private fun QrCodeImage(
    data: String,
    size: androidx.compose.ui.unit.Dp,
    qrColor: Color = NeonMint,
    backgroundColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    if (data.isBlank()) return

    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    val qrBitmap = remember(data, sizePx) {
        generateQrBitmap(data, sizePx, qrColor, backgroundColor)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0D1A2A32)) // Very subtle dark glass behind QR
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier
                .size(size - 24.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Generates a QR code bitmap with custom foreground/background colors.
 */
private fun generateQrBitmap(
    data: String,
    sizePx: Int,
    fgColor: Color,
    bgColor: Color
): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val fgArgb = fgColor.copy(alpha = 1f).let {
        AndroidColor.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt())
    }
    val bgArgb = bgColor.let {
        AndroidColor.argb(
            (it.alpha * 255).toInt(),
            (it.red * 255).toInt(),
            (it.green * 255).toInt(),
            (it.blue * 255).toInt()
        )
    }
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) fgArgb else bgArgb)
        }
    }
    return bitmap
}

// ════════════════════════════════════════════════════════════════
// Glass Settings Card — container for navigation rows
// ════════════════════════════════════════════════════════════════

@Composable
private fun GlassSettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        content()
    }
}

// ════════════════════════════════════════════════════════════════
// Glass Navigation Row — individual setting item
// ════════════════════════════════════════════════════════════════

@Composable
private fun GlassNavigationRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon in a glass circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Glass Divider — subtle separator inside glass cards
// ════════════════════════════════════════════════════════════════

@Composable
private fun GlassDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

// ════════════════════════════════════════════════════════════════
// Section Label
// ════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = NeonMint,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

// ════════════════════════════════════════════════════════════════
// Logout Glass Button — destructive red glass card
// ════════════════════════════════════════════════════════════════

@Composable
private fun LogoutGlassButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DestructiveBackgroundAlpha)
            .border(
                width = 1.dp,
                color = DestructiveText.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = DestructiveText,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = "Log Out",
                color = DestructiveText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Logout Confirmation Dialog
// ════════════════════════════════════════════════════════════════

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                fontSize = 15.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Log Out",
                    color = DestructiveText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = TextSecondary
                )
            }
        },
        containerColor = Color(0xFF1A2A32),
        shape = RoundedCornerShape(20.dp)
    )
}
