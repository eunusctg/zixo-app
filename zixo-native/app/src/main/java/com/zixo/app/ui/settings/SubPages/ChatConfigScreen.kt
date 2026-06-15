package com.zixo.app.ui.settings.SubPages

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.EphemeralTimerOption
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSlider
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
// Chat Configuration Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Chat Configuration" screen with Liquid Glass aesthetic.
 *
 * All settings bound to [SettingsViewModel.settingsState] — no local state, no dummy data.
 *
 * Sections:
 * 1. Chat behavior — Enter is send, Media visibility
 * 2. Font size scale with live preview
 * 3. Ephemeral timer selection
 * 4. Message search & Chat wallpaper (navigation items)
 */
@Composable
fun ChatConfigScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        LazyColumn(
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
                    title = "Chat Configuration",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Section 1: Chat Behavior ─────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CHAT BEHAVIOR",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enter is send
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enter Is Send",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Enter key sends message instead of adding new line",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.enterIsSend,
                            onCheckedChange = { viewModel.updateEnterIsSend(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Media visibility
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PermMedia,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Media Visibility",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Show downloaded media in device gallery",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.isMediaVisibilityEnabled,
                            onCheckedChange = { viewModel.updateMediaVisibility(it) }
                        )
                    }
                }
            }

            // ── Section 2: Font Size ─────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "FONT SIZE",
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
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Font Size",
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

                    Text(
                        text = "Preview",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // ── Section 3: Ephemeral Timer ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "EPHEMERAL MESSAGES",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

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

            // ── Section 4: Navigation Items ──────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(8.dp)
                ) {
                    NavigationItem(
                        title = "Message Search",
                        subtitle = "Search across all conversations",
                        icon = Icons.Filled.Search,
                        onClick = { /* Placeholder – navigate to search */ }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationItem(
                        title = "Chat Wallpaper",
                        subtitle = "Default",
                        icon = Icons.Filled.Wallpaper,
                        onClick = { /* Placeholder – navigate to wallpaper picker */ }
                    )
                }
            }
        }
    }
}
