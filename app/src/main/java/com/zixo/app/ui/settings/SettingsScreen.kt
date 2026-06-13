package com.zixo.app.ui.settings

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.ui.components.LiquidGlassCard
import com.zixo.app.ui.components.LiquidGlassSurface
import com.zixo.app.ui.components.ProfileGradientBrush
import com.zixo.app.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val currentUser by settingsViewModel.currentUser.collectAsState()
    val showQrPopup by settingsViewModel.showQrPopup.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Radiant Profile Header Block ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ProfileGradientBrush)
                    .clickable { onEditProfile() }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ZixoSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser?.avatarUrl?.isNotBlank() == true) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(currentUser!!.avatarUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(72.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (currentUser?.displayName ?: "Z").take(1).uppercase(),
                                color = ZixoAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name, Zixo Number, Username
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Zixo User",
                            color = ZixoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentUser?.zixoNumber?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = ZixoAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentUser!!.zixoNumber,
                                    color = ZixoAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (currentUser?.username?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "@${currentUser!!.username}",
                                color = ZixoTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // QR Code icon
                    IconButton(onClick = { settingsViewModel.showQrPopup() }) {
                        Icon(
                            Icons.Filled.QrCode2,
                            "QR Code",
                            tint = ZixoAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Appearance ──
            SettingsSectionTitle("Appearance")
            LiquidGlassCard {
                Column {
                    Text("Theme", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ZixoSurface),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            val label = when (mode) {
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.AMOLED -> "AMOLED"
                                ThemeMode.SYSTEM -> "System"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ZixoAccent else Color.Transparent)
                                    .clickable { settingsViewModel.updateThemeMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else ZixoTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Notifications & Audio ──
            SettingsSectionTitle("Notifications & Audio")
            LiquidGlassCard {
                SettingsToggle("Ringtone", settings.ringtoneEnabled) {
                    settingsViewModel.updateRingtoneEnabled(it)
                }
                SettingsToggle("Vibration", settings.vibrationEnabled) {
                    settingsViewModel.updateVibrationEnabled(it)
                }
                SettingsToggle("Message Preview", settings.messagePreviewEnabled) {
                    settingsViewModel.updateMessagePreviewEnabled(it)
                }
                SettingsToggle("Read Receipts", settings.readReceiptsEnabled) {
                    settingsViewModel.updateReadReceiptsEnabled(it)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Privacy & Security ──
            SettingsSectionTitle("Privacy & Security")
            LiquidGlassCard {
                Text("Status Privacy", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                com.zixo.app.domain.model.StatusPrivacy.entries.forEach { privacy ->
                    val label = when (privacy) {
                        com.zixo.app.domain.model.StatusPrivacy.ALL_CONTACTS -> "All Contacts"
                        com.zixo.app.domain.model.StatusPrivacy.MY_CONTACTS_EXCEPT -> "My Contacts Except..."
                        com.zixo.app.domain.model.StatusPrivacy.ONLY_SHARE_WITH -> "Only Share With..."
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsViewModel.updateStatusPrivacy(privacy) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.statusPrivacy == privacy,
                            onClick = { settingsViewModel.updateStatusPrivacy(privacy) },
                            colors = RadioButtonDefaults.colors(selectedColor = ZixoAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = ZixoTextPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── System ──
            SettingsSectionTitle("System")
            LiquidGlassCard {
                SettingsNavigationItem("Edit Profile", Icons.Filled.Person) { onEditProfile() }
                SettingsNavigationItem("Blocked Users", Icons.Filled.Block) { /* Navigate */ }
                SettingsNavigationItem("Help & Support", Icons.Filled.Help) { /* Navigate */ }
                SettingsNavigationItem("About Zixo", Icons.Filled.Info) { /* Navigate */ }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Account ──
            SettingsSectionTitle("Account")
            LiquidGlassCard {
                SettingsNavigationItem("Sign Out", Icons.Filled.Logout, ZixoError) {
                    settingsViewModel.signOut()
                    onSignOut()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Zixo v2.0.0",
                color = ZixoTextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── QR Code Popup ──
        if (showQrPopup) {
            QrCodePopup(
                userProfile = currentUser,
                onDismiss = { settingsViewModel.dismissQrPopup() }
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = ZixoAccent,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = ZixoTextPrimary, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ZixoAccent,
                checkedThumbColor = Color.Black,
                uncheckedTrackColor = ZixoSurface,
                uncheckedThumbColor = ZixoTextSecondary
            )
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = ZixoTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = ZixoTextTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun QrCodePopup(
    userProfile: UserProfile?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shareLink = "https://zixo.pages.dev/u/${userProfile?.username ?: ""}"

    // Generate QR code
    val qrBitmap = remember(shareLink) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(shareLink, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF00E676.toInt() else 0x000B1519.toInt())
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier.padding(32.dp),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zixo QR Code",
                    color = ZixoAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // QR Code image
                qrBitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ZixoBackground)
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userProfile?.displayName ?: "",
                    color = ZixoTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = userProfile?.zixoNumber ?: "",
                    color = ZixoAccent,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Copy share link
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Zixo Link", shareLink))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZixoSurface)
                ) {
                    Icon(Icons.Filled.Link, null, tint = ZixoAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Share Link", color = ZixoAccent, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close", color = ZixoTextSecondary)
                }
            }
        }
    }
}
