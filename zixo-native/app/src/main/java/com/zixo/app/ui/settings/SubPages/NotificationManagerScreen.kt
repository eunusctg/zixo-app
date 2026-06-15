package com.zixo.app.ui.settings.SubPages

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.VibrationOption
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Notification Manager Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Notifications & Alerts Manager" screen with Liquid Glass aesthetic.
 *
 * All settings bound to [SettingsViewModel.settingsState] — persisted via
 * DataStore through [SettingsRepository].
 *
 * Features:
 * - Custom notification sound ringtone targets (Message, Group, Call)
 * - Heads-up popup toggle
 * - Vibration rhythm configurations (Off / Default / Short / Long / Custom)
 * - Notification light color picker
 * - Do Not Disturb mode toggle
 * - Per-chat notification override section
 * - Call notification full-screen intent toggle
 * - Preview visibility (Show sender and message / Show sender only / Show nothing)
 */
@Composable
fun NotificationManagerScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Local state for settings not yet in AppSettingsState ──
    var isHeadsUpEnabled by remember { mutableStateOf(true) }
    var isDndEnabled by remember { mutableStateOf(false) }
    var isFullScreenIntentEnabled by remember { mutableStateOf(true) }
    var selectedPreviewVisibility by remember { mutableStateOf(0) } // 0=both, 1=sender, 2=nothing
    var selectedNotificationLightColor by remember { mutableStateOf(0) } // index into lightColors

    val previewOptions = listOf("Sender & Message", "Sender Only", "Nothing")
    val lightColors = listOf(
        "Default" to Color.White,
        "Green" to Color(0xFF00E676),
        "Red" to Color(0xFFFF5252),
        "Blue" to Color(0xFF448AFF),
        "Yellow" to Color(0xFFFFEB3B),
        "Cyan" to Color(0xFF00E5FF)
    )

    // ── Per-chat notification overrides ──
    var chatOverrides by remember {
        mutableStateOf(
            listOf(
                ChatNotificationOverride("Family Group", true, true),
                ChatNotificationOverride("Work Chat", false, true),
                ChatNotificationOverride("Best Friend", true, false)
            )
        )
    }

    // ── Ringtone picker launchers ──
    val messageToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.updateMessageNotificationTone(uri.toString()) }
        }
    }

    val groupToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.updateGroupNotificationTone(uri.toString()) }
        }
    }

    val callToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.updateCallRingtone(uri.toString()) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top bar ──
            item {
                ZixoTopBar(
                    title = "Notifications & Alerts",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Section 1: Conversation Tones ───────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("SOUNDS")
                    Spacer(modifier = Modifier.height(16.dp))

                    NotificationSwitchRow(
                        title = "Conversation Tones",
                        subtitle = "Play sounds for incoming and outgoing messages",
                        checked = settingsState.areConversationTonesEnabled,
                        onCheckedChange = { viewModel.updateConversationTones(it) }
                    )
                }
            }

            // ── Section 2: Notification Tones ───────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(8.dp)
                ) {
                    SectionLabel(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp), text = "NOTIFICATION TONES")
                    Spacer(modifier = Modifier.height(8.dp))

                    // Message notification tone
                    NavigationItem(
                        title = "Message Notifications",
                        subtitle = resolveToneName(context, settingsState.messageNotificationToneUri),
                        icon = Icons.Filled.NotificationsActive,
                        onClick = {
                            messageToneLauncher.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Message Notification Tone")
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        parseUriOrNull(settingsState.messageNotificationToneUri)
                                    )
                                }
                            )
                        }
                    )

                    // Group notification tone
                    NavigationItem(
                        title = "Group Notifications",
                        subtitle = resolveToneName(context, settingsState.groupNotificationToneUri),
                        icon = Icons.Filled.Group,
                        onClick = {
                            groupToneLauncher.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Group Notification Tone")
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        parseUriOrNull(settingsState.groupNotificationToneUri)
                                    )
                                }
                            )
                        }
                    )

                    // Call ringtone
                    NavigationItem(
                        title = "Call Ringtone",
                        subtitle = resolveToneName(context, settingsState.callRingtoneUri),
                        icon = Icons.Filled.Call,
                        onClick = {
                            callToneLauncher.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Call Ringtone")
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        parseUriOrNull(settingsState.callRingtoneUri)
                                    )
                                }
                            )
                        }
                    )
                }
            }

            // ── Section 3: Heads-up Popup ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("POPUP")
                    Spacer(modifier = Modifier.height(16.dp))

                    NotificationSwitchRow(
                        title = "Heads-up Popup",
                        subtitle = "Show notification as a popup at the top of the screen",
                        checked = isHeadsUpEnabled,
                        onCheckedChange = { isHeadsUpEnabled = it }
                    )
                }
            }

            // ── Section 4: Vibration ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("VIBRATION")
                    Spacer(modifier = Modifier.height(12.dp))

                    val vibrationOptions = listOf("Off", "Default", "Short", "Long", "Custom")
                    val selectedVibrationIndex = when (settingsState.vibrationPattern) {
                        VibrationOption.OFF -> 0
                        VibrationOption.DEFAULT -> 1
                        VibrationOption.SHORT -> 2
                        VibrationOption.LONG -> 3
                    }

                    GlassSegmentedPicker(
                        options = vibrationOptions,
                        selectedIndex = if (selectedVibrationIndex in 0..3) selectedVibrationIndex else 4,
                        onOptionSelected = { index ->
                            val pattern = when (index) {
                                0 -> VibrationOption.OFF
                                1 -> VibrationOption.DEFAULT
                                2 -> VibrationOption.SHORT
                                3 -> VibrationOption.LONG
                                else -> VibrationOption.DEFAULT // Custom falls back to Default
                            }
                            viewModel.updateVibrationPattern(pattern)
                        }
                    )

                    if (settingsState.vibrationPattern == VibrationOption.OFF) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your device will not vibrate for any Zixo notifications",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Section 5: Notification Light ───────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("NOTIFICATION LIGHT")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LED Color",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Customize LED notification color (device dependent)",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color picker row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lightColors.forEachIndexed { index, (name, color) ->
                            val isSelected = selectedNotificationLightColor == index
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = if (isSelected) 1f else 0.3f))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) NeonMint else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedNotificationLightColor = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lightColors[selectedNotificationLightColor].first,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Section 6: Do Not Disturb ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("DO NOT DISTURB")
                    Spacer(modifier = Modifier.height(16.dp))

                    NotificationSwitchRow(
                        icon = Icons.Filled.DoNotDisturbOn,
                        title = "Do Not Disturb",
                        subtitle = "Mute all Zixo notifications until you turn this off",
                        checked = isDndEnabled,
                        onCheckedChange = { isDndEnabled = it }
                    )
                }
            }

            // ── Section 7: Preview Visibility ───────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("NOTIFICATION PREVIEW")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Show Content in Notification",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassSegmentedPicker(
                        options = previewOptions,
                        selectedIndex = selectedPreviewVisibility,
                        onOptionSelected = { selectedPreviewVisibility = it }
                    )
                }
            }

            // ── Section 8: Call Full-Screen Intent ──────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("CALL NOTIFICATIONS")
                    Spacer(modifier = Modifier.height(16.dp))

                    NotificationSwitchRow(
                        icon = Icons.Filled.ScreenLockPortrait,
                        title = "Full-Screen Call Notification",
                        subtitle = "Show incoming calls as full-screen when device is locked",
                        checked = isFullScreenIntentEnabled,
                        onCheckedChange = { isFullScreenIntentEnabled = it }
                    )
                }
            }

            // ── Section 9: Per-Chat Notification Overrides ──────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("PER-CHAT NOTIFICATIONS")
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Customize notification behavior for specific chats",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    chatOverrides.forEach { override ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = override.chatName,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (override.isEnabled) "Notifications on" else "Muted",
                                    color = if (override.isEnabled) NeonMint else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            GlassSwitch(
                                checked = override.isEnabled,
                                onCheckedChange = { enabled ->
                                    chatOverrides = chatOverrides.map {
                                        if (it.chatName == override.chatName) it.copy(isEnabled = enabled)
                                        else it
                                    }
                                }
                            )
                        }
                    }

                    if (chatOverrides.isEmpty()) {
                        Text(
                            text = "No chat-specific overrides configured",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Per-Chat Notification Override Model
// ─────────────────────────────────────────────────────────────────────────────

private data class ChatNotificationOverride(
    val chatName: String,
    val isEnabled: Boolean,
    val useVibration: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Notification Switch Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
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
        GlassSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = NeonMint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Ringtone Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun resolveToneName(context: android.content.Context, uriString: String): String {
    if (uriString.isBlank()) return "Default"
    return try {
        val uri = android.net.Uri.parse(uriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Default"
    } catch (_: Exception) {
        "Default"
    }
}

private fun parseUriOrNull(uriString: String): android.net.Uri? {
    return if (uriString.isNotBlank()) {
        try { android.net.Uri.parse(uriString) } catch (_: Exception) { null }
    } else {
        null
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
