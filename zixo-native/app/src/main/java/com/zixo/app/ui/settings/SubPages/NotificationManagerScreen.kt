package com.zixo.app.ui.settings.SubPages

import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.VibrationPattern
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.screens.settings.SettingsViewModel
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

/**
 * "Notifications & Alerts Manager" screen with Liquid Glass aesthetic.
 *
 * Provides controls for:
 * - Conversation tones (on/off)
 * - Per-channel ringtone selection (message, group, call, video call)
 * - Vibration pattern selection
 *
 * Ringtone selection uses the Android system ringtone picker via
 * [ActivityResultContracts.StartActivityForResult].
 *
 * @param onBackClick Callback invoked when the user taps the back arrow.
 * @param viewModel   Hilt-injected [SettingsViewModel] providing state and mutations.
 */
@Composable
fun NotificationManagerScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Ringtone picker launchers ──────────────────────────────────────────────
    // Each launcher opens the Android system ringtone picker and persists the
    // selected URI back to the ViewModel on result.

    val messageToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.setMessageNotificationToneUri(uri.toString()) }
        }
    }

    val groupToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.setGroupNotificationToneUri(uri.toString()) }
        }
    }

    val callToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.setCallRingtoneUri(uri.toString()) }
        }
    }

    val videoCallToneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?.let { uri -> viewModel.setVideoCallRingtoneUri(uri.toString()) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Animated glass background layer ──
        ZixoGlassBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar with back navigation ──
            ZixoTopBar(
                title = "Notifications & Alerts",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // ═══════════════════════════════════════════════════════════════════
            // 1. SOUNDS
            // ═══════════════════════════════════════════════════════════════════
            SectionHeader(title = "SOUNDS")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    GlassSwitch(
                        checked = uiState.areConversationTonesEnabled,
                        onCheckedChange = viewModel::setConversationTonesEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════════════════════════
            // 2. NOTIFICATION TONES
            // ═══════════════════════════════════════════════════════════════════
            SectionHeader(title = "NOTIFICATION TONES")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                // Message Notification Tone
                NavigationItem(
                    title = "Message Notification Tone",
                    subtitle = resolveToneName(context, uiState.messageNotificationToneUri),
                    icon = Icons.Filled.NotificationsActive,
                    onClick = {
                        messageToneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Message Notification Tone")
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    parseUriOrNull(uiState.messageNotificationToneUri)
                                )
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Group Notification Tone
                NavigationItem(
                    title = "Group Notification Tone",
                    subtitle = resolveToneName(context, uiState.groupNotificationToneUri),
                    icon = Icons.Filled.Group,
                    onClick = {
                        groupToneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Group Notification Tone")
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    parseUriOrNull(uiState.groupNotificationToneUri)
                                )
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Call Ringtone
                NavigationItem(
                    title = "Call Ringtone",
                    subtitle = "Rhythmic, low-latency telecom signaling tone",
                    icon = Icons.Filled.Call,
                    onClick = {
                        callToneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Call Ringtone")
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    parseUriOrNull(uiState.callRingtoneUri)
                                )
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Video Call Ringtone
                NavigationItem(
                    title = "Video Call Ringtone",
                    subtitle = "Modern, high-fidelity electronic resonance tone",
                    icon = Icons.Filled.VideoCall,
                    onClick = {
                        videoCallToneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Video Call Ringtone")
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    parseUriOrNull(uiState.videoCallRingtoneUri)
                                )
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════════════════════════
            // 3. VIBRATION
            // ═══════════════════════════════════════════════════════════════════
            SectionHeader(title = "VIBRATION")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Vibration Pattern",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val vibrationOptions = listOf("Off", "Default", "Short", "Long")
                val selectedVibrationIndex = when (uiState.vibrationPattern) {
                    VibrationPattern.OFF -> 0
                    VibrationPattern.DEFAULT -> 1
                    VibrationPattern.SHORT -> 2
                    VibrationPattern.LONG -> 3
                }

                GlassSegmentedPicker(
                    options = vibrationOptions,
                    selectedIndex = selectedVibrationIndex,
                    onOptionSelected = { index ->
                        val pattern = when (index) {
                            0 -> VibrationPattern.OFF
                            1 -> VibrationPattern.DEFAULT
                            2 -> VibrationPattern.SHORT
                            else -> VibrationPattern.LONG
                        }
                        viewModel.setVibrationPattern(pattern)
                    }
                )
            }

            // Bottom padding to avoid content being cut off
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Private helpers for ringtone display & URI parsing
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Resolves a ringtone URI string to a human-readable name.
 * Returns "Default" if the URI is empty or the ringtone cannot be found.
 */
private fun resolveToneName(context: android.content.Context, uriString: String): String {
    if (uriString.isBlank()) return "Default"
    return try {
        val uri = android.net.Uri.parse(uriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Default"
    } catch (_: Exception) {
        "Default"
    }
}

/**
 * Safely parses a URI string into a [android.net.Uri], returning null if blank.
 * Used to supply the existing ringtone URI to the system picker.
 */
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
