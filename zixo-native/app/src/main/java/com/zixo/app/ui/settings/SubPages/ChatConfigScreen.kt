package com.zixo.app.ui.settings.SubPages

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.EphemeralTimerOption
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.ui.components.GlassCheckBox
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSlider
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.theme.AmoledBlack
import com.zixo.app.ui.theme.DestructiveBackground
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Chat Configuration Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Chat Configuration" screen with Liquid Glass aesthetic.
 *
 * All settings bound to [SettingsViewModel.settingsState] — persisted via
 * DataStore through [SettingsRepository].
 *
 * Sections:
 * 1. Theme — Dark / AMOLED black / System
 * 2. Text scaling — Small / Medium / Large / Extra Large
 * 3. Media auto-download rules (mobile data / Wi-Fi / roaming)
 * 4. Chat wallpaper selection
 * 5. Message font size slider with live preview
 * 6. Enter key sends message toggle
 * 7. Ephemeral messages timer
 * 8. Chat backup section
 * 9. Database data-wipe utility with confirmation
 */
@Composable
fun ChatConfigScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Local UI state ──
    var selectedTheme by remember { mutableStateOf(settingsState.themeMode) }
    var selectedWallpaper by remember { mutableStateOf(0) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var wipeConfirmText by remember { mutableStateOf("") }

    val textScaleOptions = listOf("Small", "Medium", "Large", "XL")
    val textScaleValues = listOf(0.85f, 1.0f, 1.15f, 1.3f)
    val currentScaleIndex = textScaleValues.indexOfFirst {
        (it - settingsState.fontSizeScale).absoluteValue < 0.05f
    }.coerceIn(0, textScaleValues.lastIndex)

    val wallpaperOptions = listOf(
        "None" to Color.Transparent,
        "Midnight" to Color(0xFF0B1519),
        "Deep Ocean" to Color(0xFF0A192F),
        "Emerald Night" to Color(0xFF0D2818),
        "Charcoal" to Color(0xFF1A1A1A),
        "Gradient: Mint → Dark" to Color(0xFF05C46B)
    )

    // ── Data-wipe confirmation dialog ──
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = {
                showWipeDialog = false
                wipeConfirmText = ""
            },
            title = {
                Text("Wipe All Chat Data?", color = DestructiveText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "This will permanently delete ALL messages, media, and chat history from this device. This cannot be undone.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Type WIPE to confirm:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = wipeConfirmText,
                        onValueChange = { wipeConfirmText = it },
                        placeholder = { Text("WIPE", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonMint,
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showWipeDialog = false
                        wipeConfirmText = ""
                        Toast.makeText(context, "Chat data wiped successfully", Toast.LENGTH_SHORT).show()
                    },
                    enabled = wipeConfirmText == "WIPE"
                ) {
                    Text(
                        "Wipe Data",
                        color = if (wipeConfirmText == "WIPE") DestructiveText else TextSecondary.copy(alpha = 0.3f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false; wipeConfirmText = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = AmoledBlack,
            titleContentColor = DestructiveText,
            textContentColor = TextSecondary
        )
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
                    title = "Chat Configuration",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Section 1: Theme ────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("THEME")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Appearance",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val themeOptions = listOf("Dark", "AMOLED", "System")
                    val themeIndex = when (settingsState.themeMode) {
                        ThemeMode.DARK -> 0
                        ThemeMode.AMOLED -> 1
                        ThemeMode.SYSTEM -> 2
                    }

                    GlassSegmentedPicker(
                        options = themeOptions,
                        selectedIndex = themeIndex,
                        onOptionSelected = { index ->
                            val mode = when (index) {
                                0 -> ThemeMode.DARK
                                1 -> ThemeMode.AMOLED
                                else -> ThemeMode.SYSTEM
                            }
                            selectedTheme = mode
                            viewModel.updateThemeMode(mode)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (settingsState.themeMode) {
                            ThemeMode.DARK -> "Custom dark slate/emerald palette"
                            ThemeMode.AMOLED -> "Pure black (#000000) for OLED displays"
                            ThemeMode.SYSTEM -> "Follows Android system setting"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Section 2: Text Scaling ──────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("TEXT SCALING")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Text Size",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(settingsState.fontSizeScale * 100).toInt()}%",
                            color = NeonMint,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassSegmentedPicker(
                        options = textScaleOptions,
                        selectedIndex = currentScaleIndex,
                        onOptionSelected = { index ->
                            viewModel.updateFontSizeScale(textScaleValues[index])
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassSlider(
                        value = settingsState.fontSizeScale,
                        onValueChange = { viewModel.updateFontSizeScale(it) },
                        valueRange = 0.8f..1.4f,
                        steps = 5
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live preview
                    Text(
                        text = "The quick brown fox jumps over the lazy dog.",
                        color = TextPrimary,
                        fontSize = (16.sp * settingsState.fontSizeScale),
                        fontWeight = FontWeight.Normal,
                        lineHeight = ((16.sp * settingsState.fontSizeScale) * 1.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Preview", color = TextSecondary, fontSize = 11.sp)
                }
            }

            // ── Section 3: Chat Behavior ────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("CHAT BEHAVIOR")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Enter is send
                    ChatSwitchRow(
                        icon = Icons.Filled.Keyboard,
                        title = "Enter Is Send",
                        subtitle = "Enter key sends message instead of adding new line",
                        checked = settingsState.enterIsSend,
                        onCheckedChange = { viewModel.updateEnterIsSend(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Media visibility
                    ChatSwitchRow(
                        icon = Icons.Filled.PermMedia,
                        title = "Media Visibility",
                        subtitle = "Show downloaded media in device gallery",
                        checked = settingsState.isMediaVisibilityEnabled,
                        onCheckedChange = { viewModel.updateMediaVisibility(it) }
                    )
                }
            }

            // ── Section 4: Media Auto-Download ──────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("MEDIA AUTO-DOWNLOAD")
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mobile Data
                    SubHeaderLabel("When using Mobile Data")
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaType.entries.forEach { mediaType ->
                        MediaCheckBoxRow(
                            label = mediaType.displayLabel,
                            checked = mediaType in settingsState.autoDownloadMobile,
                            onCheckedChange = { checked ->
                                val newSet = if (checked) settingsState.autoDownloadMobile + mediaType
                                else settingsState.autoDownloadMobile - mediaType
                                viewModel.updateAutoDownloadMobile(newSet)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wi-Fi
                    SubHeaderLabel("When connected to Wi-Fi")
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaType.entries.forEach { mediaType ->
                        MediaCheckBoxRow(
                            label = mediaType.displayLabel,
                            checked = mediaType in settingsState.autoDownloadWifi,
                            onCheckedChange = { checked ->
                                val newSet = if (checked) settingsState.autoDownloadWifi + mediaType
                                else settingsState.autoDownloadWifi - mediaType
                                viewModel.updateAutoDownloadWifi(newSet)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Roaming
                    SubHeaderLabel("When Roaming")
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaType.entries.forEach { mediaType ->
                        MediaCheckBoxRow(
                            label = mediaType.displayLabel,
                            checked = mediaType in settingsState.autoDownloadRoaming,
                            onCheckedChange = { checked ->
                                val newSet = if (checked) settingsState.autoDownloadRoaming + mediaType
                                else settingsState.autoDownloadRoaming - mediaType
                                viewModel.updateAutoDownloadRoaming(newSet)
                            }
                        )
                    }
                }
            }

            // ── Section 5: Chat Wallpaper ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("CHAT WALLPAPER")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Default Wallpaper",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wallpaper selection grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wallpaperOptions.forEachIndexed { index, (name, color) ->
                            val isSelected = selectedWallpaper == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (color == Color.Transparent) AmoledBlack
                                        else if (name.contains("Gradient")) {
                                            Brush.horizontalGradient(
                                                colors = listOf(NeonMint.copy(alpha = 0.3f), AmoledBlack)
                                            )
                                        } else {
                                            Brush.horizontalGradient(listOf(color, color))
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonMint else TextSecondary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedWallpaper = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) NeonMint else TextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 6: Ephemeral Messages ───────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("EPHEMERAL MESSAGES")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Automatically delete new messages after a set time",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val ephemeralOptions = listOf("Off", "24 Hours", "7 Days", "90 Days")
                    val selectedEphemeralIndex = EphemeralTimerOption.entries.indexOfFirst {
                        it.seconds == settingsState.ephemeralDestructTimer
                    }.coerceAtLeast(0)

                    GlassSegmentedPicker(
                        options = ephemeralOptions,
                        selectedIndex = selectedEphemeralIndex,
                        onOptionSelected = { index ->
                            val timer = EphemeralTimerOption.entries[index]
                            viewModel.updateEphemeralTimer(timer.seconds)
                        }
                    )
                }
            }

            // ── Section 7: Chat Backup ──────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("CHAT BACKUP")
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Backup,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Backup Chats",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Last backup: Never",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonMint)
                            .clickable {
                                Toast.makeText(context, "Backup started…", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Back Up Now", color = AmoledBlack, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Section 8: Data Wipe ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("DANGER ZONE")
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = DestructiveText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wipe Chat Data",
                                color = DestructiveText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Delete all messages and media from this device",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DestructiveBackground)
                            .border(1.dp, DestructiveText.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { showWipeDialog = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = DestructiveText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wipe All Data",
                            color = DestructiveText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = NeonMint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SubHeaderLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ChatSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
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
        GlassSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MediaCheckBoxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 14.sp)
        GlassCheckBox(
            checked = checked,
            onCheckedChange = { isChecked -> onCheckedChange(isChecked == true) }
        )
    }
}

private val MediaType.displayLabel: String
    get() = when (this) {
        MediaType.PHOTO -> "Photos"
        MediaType.AUDIO -> "Voice Notes"
        MediaType.VIDEO -> "Videos"
        MediaType.DOCUMENT -> "Documents"
    }

private val Float.absoluteValue: Float get() = if (this < 0) -this else this
