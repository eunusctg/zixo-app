package com.zixo.app.ui.settings.SubPages

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VideoCall

// ─────────────────────────────────────────────────────────────────────────────
// Notification Manager Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Notifications & Alerts Manager" screen with Liquid Glass aesthetic.
 *
 * All settings bound to [SettingsViewModel.settingsState] — no local state, no dummy data.
 *
 * Features:
 * - Conversation tones toggle
 * - Per-channel ringtone selection (message, group, call, video call)
 *   via Android system ringtone picker
 * - Vibration pattern selection (GlassSegmentedPicker)
 * - Notification light color (placeholder)
 */
@Composable
fun NotificationManagerScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Ringtone picker launchers ──────────────────────────────────────────
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

    val videoCallToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.updateVideoCallRingtone(uri.toString()) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 32.dp
            ),
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

            // ── Section 1: Conversation Tones ────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SOUNDS",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conversation Tones",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Play sounds for incoming and outgoing messages",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.areConversationTonesEnabled,
                            onCheckedChange = { viewModel.updateConversationTones(it) }
                        )
                    }
                }
            }

            // ── Section 2: Notification Tones ────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "NOTIFICATION TONES",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message notification tone
                    NavigationItem(
                        title = "Message Notification Tone",
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
                        title = "Group Notification Tone",
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

                    // Video call ringtone
                    NavigationItem(
                        title = "Video Call Ringtone",
                        subtitle = resolveToneName(context, settingsState.videoCallRingtoneUri),
                        icon = Icons.Filled.VideoCall,
                        onClick = {
                            videoCallToneLauncher.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Video Call Ringtone")
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        parseUriOrNull(settingsState.videoCallRingtoneUri)
                                    )
                                }
                            )
                        }
                    )
                }
            }

            // ── Section 3: Vibration ─────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "VIBRATION",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val vibrationOptions = listOf("Off", "Default", "Short", "Long")
                    val selectedVibrationIndex = when (settingsState.vibrationPattern) {
                        VibrationOption.OFF -> 0
                        VibrationOption.DEFAULT -> 1
                        VibrationOption.SHORT -> 2
                        VibrationOption.LONG -> 3
                    }

                    GlassSegmentedPicker(
                        options = vibrationOptions,
                        selectedIndex = selectedVibrationIndex,
                        onOptionSelected = { index ->
                            val pattern = when (index) {
                                0 -> VibrationOption.OFF
                                1 -> VibrationOption.DEFAULT
                                2 -> VibrationOption.SHORT
                                else -> VibrationOption.LONG
                            }
                            viewModel.updateVibrationPattern(pattern)
                        }
                    )
                }
            }

            // ── Section 4: Notification Light (Placeholder) ──────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NOTIFICATION LIGHT",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

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
                                text = "Notification Light Color",
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
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Private helpers for ringtone display & URI parsing
// ──────────────────────────────────────────────────────────────────────────────

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
        try {
            android.net.Uri.parse(uriString)
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }
}
