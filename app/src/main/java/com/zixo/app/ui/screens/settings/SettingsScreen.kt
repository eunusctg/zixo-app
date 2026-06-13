package com.zixo.app.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zixo.app.data.model.AppSettings
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Ringtone picker launchers
    val notificationToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let { viewModel.updateNotificationTone(it.toString()) }
        }
    }
    val callRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let { viewModel.updateIncomingCallRingtone(it.toString()) }
        }
    }

    // Logout / Delete dialogs
    if (uiState.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::hideLogoutConfirm,
            title = { Text("Logout", color = ZixoText) },
            text = { Text("Are you sure you want to logout?", color = ZixoTextSecondary) },
            confirmButton = {
                TextButton(onClick = viewModel::logout) { Text("Logout", color = ZixoError) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideLogoutConfirm) { Text("Cancel", color = ZixoTextSecondary) }
            },
            containerColor = ZixoSurface
        )
    }
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirm,
            title = { Text("Delete Account", color = ZixoError) },
            text = { Text("This action is irreversible. All your data will be permanently deleted.", color = ZixoTextSecondary) },
            confirmButton = {
                TextButton(onClick = viewModel::deleteAccount) { Text("Delete", color = ZixoError) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirm) { Text("Cancel", color = ZixoTextSecondary) }
            },
            containerColor = ZixoSurface
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBg)
    ) {
        // Profile Header Card
        item {
            ProfileHeaderCard(
                user = currentUser,
                onClick = onEditProfile
            )
        }

        // Appearance
        item {
            SectionHeader("Appearance")
            ThemeSelector(
                selected = settings.theme,
                onSelected = viewModel::updateTheme
            )
        }

        // Privacy
        item {
            SectionHeader("Privacy")
            SettingsSwitch(
                icon = Icons.Default.Visibility,
                title = "Online Status",
                subtitle = "Show when you're online",
                checked = settings.onlineStatus,
                onCheckedChange = viewModel::updateOnlineStatus
            )
            SettingsSwitch(
                icon = Icons.Default.DoneAll,
                title = "Read Receipts",
                subtitle = "Show when you've read messages",
                checked = settings.readReceipts,
                onCheckedChange = viewModel::updateReadReceipts
            )
            LastSeenSelector(
                selected = settings.lastSeenVisibility,
                onSelected = viewModel::updateLastSeenVisibility
            )
            StatusPrivacySelector(
                selected = settings.statusPrivacy,
                onSelected = viewModel::updateStatusPrivacy
            )
            SettingsSwitch(
                icon = Icons.Default.BlurOn,
                title = "Privacy Blur",
                subtitle = "Blur app in recent apps",
                checked = settings.privacyBlur,
                onCheckedChange = viewModel::updatePrivacyBlur
            )
            SettingsSwitch(
                icon = Icons.Default.Lock,
                title = "Screen Lock",
                subtitle = "Require biometric to open app",
                checked = settings.screenLock,
                onCheckedChange = viewModel::updateScreenLock
            )
        }

        // Notifications & Audio
        item {
            SectionHeader("Notifications & Audio")
            SettingsRow(
                icon = Icons.Default.Notifications,
                title = "Notification Sound",
                subtitle = if (settings.notificationTone.isEmpty()) "Default" else "Custom",
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Notification Sound")
                    }
                    notificationToneLauncher.launch(intent)
                }
            )
            SettingsRow(
                icon = Icons.Default.Call,
                title = "Incoming Call Ringtone",
                subtitle = if (settings.incomingCallRingtone.isEmpty()) "Default" else "Custom",
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Incoming Call Ringtone")
                    }
                    callRingtoneLauncher.launch(intent)
                }
            )
            SettingsSwitch(
                icon = Icons.Default.DoNotDisturbOn,
                title = "Do Not Disturb",
                subtitle = "Mute all notifications",
                checked = settings.dnd,
                onCheckedChange = viewModel::updateDnd
            )
        }

        // Calls & Network
        item {
            SectionHeader("Calls & Network")
            CallTypeSelector(
                selected = settings.defaultCallType,
                onSelected = viewModel::updateDefaultCallType
            )
            SettingsSwitch(
                icon = Icons.Default.Mic,
                title = "Noise Suppression",
                subtitle = "AI noise suppression for calls",
                checked = settings.noiseSuppression,
                onCheckedChange = viewModel::updateNoiseSuppression
            )
            SettingsSwitch(
                icon = Icons.Default.EnhancedEncryption,
                title = "Force TURN Relay",
                subtitle = "Route all calls through relay to hide IP",
                checked = settings.forceTurnRelay,
                onCheckedChange = viewModel::updateForceTurnRelay
            )
        }

        // LiveKit Network
        item {
            SectionHeader("LiveKit Network")
            LiveKitUrlEditor(
                currentUrl = settings.livekitUrl,
                onSave = viewModel::updateLivekitUrl
            )
            SipPrefixEditor(
                currentPrefix = settings.sipPrefix,
                onSave = viewModel::updateSipPrefix
            )
        }

        // Media & Storage
        item {
            SectionHeader("Media & Storage")
            MediaCompressionSelector(
                selected = settings.mediaCompression,
                onSelected = viewModel::updateMediaCompression
            )
            SelfDestructSelector(
                selected = settings.selfDestructTimer,
                onSelected = viewModel::updateSelfDestructTimer
            )
            SettingsRow(
                icon = Icons.Default.CleaningServices,
                title = "Clear Cache",
                subtitle = uiState.storageUsed,
                onClick = viewModel::clearCache
            )
        }

        // Account
        item {
            SectionHeader("Account")
            SettingsRow(
                icon = Icons.Default.Logout,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = viewModel::showLogoutConfirm,
                tint = ZixoError
            )
            SettingsRow(
                icon = Icons.Default.DeleteForever,
                title = "Delete Account",
                subtitle = "Permanently delete your account and data",
                onClick = viewModel::showDeleteConfirm,
                tint = ZixoError
            )
        }

        // App Info
        item {
            SectionHeader("About")
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "2.0.0 (Build 6)",
                onClick = {}
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ProfileHeaderCard(user: ZixoUser?, onClick: () -> Unit) {
    Surface(
        color = ZixoSurface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = user?.avatar?.ifEmpty { null },
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape).shadow(4.dp, CircleShape).background(ZixoSurfaceLight)
                )
                if (user?.avatar.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(ZixoSurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user?.displayName?.take(1)?.uppercase() ?: "?",
                            color = ZixoPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user?.displayName ?: "User",
                    color = ZixoText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user?.username.isNullOrEmpty()) {
                    Text("@${user!!.username.removePrefix("@")}", color = ZixoTextSecondary, fontSize = 13.sp)
                }
                // Zixo Number Badge with emerald stroke
                val zixoNum = user?.zixoNumber ?: ""
                if (zixoNum.length == 8) {
                    Surface(
                        color = ZixoSurface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, ZixoPrimary),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "${zixoNum.substring(0, 4)} ${zixoNum.substring(4)}",
                            color = ZixoPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ZixoTextSecondary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = ZixoPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: androidx.compose.ui.graphics.Color = ZixoPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ZixoText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = ZixoTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ZixoPrimary, checkedThumbColor = ZixoBg)
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = ZixoPrimary
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = ZixoText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ZixoTextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ZixoTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ThemeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("dark" to "Dark", "amoled" to "AMOLED", "system" to "System")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelected(key) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZixoPrimary,
                    containerColor = ZixoSurface,
                    labelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ZixoHighlight,
                    selectedBorderColor = ZixoPrimary,
                    enabled = true,
                    selected = selected == key
                )
            )
        }
    }
}

@Composable
private fun LastSeenSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("everyone" to "Everyone", "contacts" to "Contacts", "nobody" to "Nobody")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelected(key) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZixoPrimary,
                    containerColor = ZixoSurface,
                    labelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ZixoHighlight,
                    selectedBorderColor = ZixoPrimary,
                    enabled = true,
                    selected = selected == key
                )
            )
        }
    }
}

@Composable
private fun StatusPrivacySelector(selected: String, onSelected: (String) -> Unit) {
    Text("Status Privacy", color = ZixoText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
    val options = listOf(
        "all_contacts" to "My Contacts",
        "exclude_some" to "Share Except...",
        "only_share_with" to "Only Share With..."
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelected(key) },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZixoPrimary,
                    containerColor = ZixoSurface,
                    labelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ZixoHighlight,
                    selectedBorderColor = ZixoPrimary,
                    enabled = true,
                    selected = selected == key
                )
            )
        }
    }
}

@Composable
private fun CallTypeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("ask" to "Ask", "sip" to "SIP Call", "webrtc" to "WebRTC Video")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelected(key) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZixoPrimary,
                    containerColor = ZixoSurface,
                    labelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ZixoHighlight,
                    selectedBorderColor = ZixoPrimary,
                    enabled = true,
                    selected = selected == key
                )
            )
        }
    }
}

@Composable
private fun MediaCompressionSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("lossless" to "Lossless", "balanced" to "Balanced", "extreme" to "Extreme")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelected(key) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZixoPrimary,
                    containerColor = ZixoSurface,
                    labelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ZixoHighlight,
                    selectedBorderColor = ZixoPrimary,
                    enabled = true,
                    selected = selected == key
                )
            )
        }
    }
}

@Composable
private fun SelfDestructSelector(selected: String, onSelected: (String) -> Unit) {
    Text("Ephemeral Timer", color = ZixoText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
    val options = listOf("off" to "Off", "5s" to "5s", "1m" to "1m", "1h" to "1h")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelected(key) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZixoPrimary,
                    containerColor = ZixoSurface,
                    labelColor = ZixoTextSecondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ZixoHighlight,
                    selectedBorderColor = ZixoPrimary,
                    enabled = true,
                    selected = selected == key
                )
            )
        }
    }
}

@Composable
private fun LiveKitUrlEditor(currentUrl: String, onSave: (String) -> Unit) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("LiveKit Server URL", color = ZixoText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            placeholder = { Text("wss://your-livekit-server.com", color = ZixoTextSecondary, fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ZixoText, unfocusedTextColor = ZixoText,
                focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight,
                cursorColor = ZixoPrimary,
                focusedContainerColor = ZixoBg, unfocusedContainerColor = ZixoBg
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                TextButton(onClick = { onSave(url) }) {
                    Text("Save", color = ZixoPrimary, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun SipPrefixEditor(currentPrefix: String, onSave: (String) -> Unit) {
    var prefix by remember(currentPrefix) { mutableStateOf(currentPrefix) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Outbound SIP Prefix", color = ZixoText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            placeholder = { Text("e.g. +1", color = ZixoTextSecondary, fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ZixoText, unfocusedTextColor = ZixoText,
                focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight,
                cursorColor = ZixoPrimary,
                focusedContainerColor = ZixoBg, unfocusedContainerColor = ZixoBg
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                TextButton(onClick = { onSave(prefix) }) {
                    Text("Save", color = ZixoPrimary, fontSize = 12.sp)
                }
            }
        )
    }
}
