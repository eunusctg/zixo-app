package com.zixo.app.ui.settings.SubPages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PermMedia
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
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSlider
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.screens.settings.SettingsViewModel
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

/**
 * "Chat Configuration" screen with Liquid Glass aesthetic.
 *
 * Allows the user to configure chat appearance (theme, wallpaper),
 * chat behavior (enter-is-send, media visibility), and the
 * typography scale with a live preview.
 *
 * @param onBackClick Callback invoked when the user taps the back arrow.
 * @param viewModel   Hilt-injected [SettingsViewModel] providing state and mutations.
 */
@Composable
fun ChatConfigScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                title = "Chat Configuration",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // ═══════════════════════════════════════════════════════════════════
            // 1. APPEARANCE
            // ═══════════════════════════════════════════════════════════════════
            SectionHeader(title = "APPEARANCE")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                // Theme Mode segmented picker
                Text(
                    text = "Theme Mode",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val themeOptions = listOf("Dark", "AMOLED", "System")
                val selectedThemeIndex = when (uiState.themeMode) {
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
                        viewModel.setThemeMode(mode)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wallpaper navigation item
                NavigationItem(
                    title = "Chat Wallpaper",
                    subtitle = "Default",
                    icon = Icons.Filled.Wallpaper,
                    onClick = { /* Placeholder – navigate to wallpaper picker */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════════════════════════
            // 2. CHAT BEHAVIOR
            // ═══════════════════════════════════════════════════════════════════
            SectionHeader(title = "CHAT BEHAVIOR")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                // Enter Is Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.padding(end = 4.dp)
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    GlassSwitch(
                        checked = uiState.enterIsSend,
                        onCheckedChange = viewModel::setEnterIsSend
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Media Visibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PermMedia,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.padding(end = 4.dp)
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    GlassSwitch(
                        checked = uiState.isMediaVisibilityEnabled,
                        onCheckedChange = viewModel::setMediaVisibilityEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════════════════════════
            // 3. FONT SIZE
            // ═══════════════════════════════════════════════════════════════════
            SectionHeader(title = "FONT SIZE")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                // Header row with icon and percentage label
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
                        text = "${(uiState.fontSizeScale * 100).toInt()}%",
                        color = NeonMint,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider for font size scale
                GlassSlider(
                    value = uiState.fontSizeScale,
                    onValueChange = viewModel::setFontSizeScale,
                    valueRange = 0.8f..1.4f,
                    steps = 5   // 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4 → 5 intermediate steps
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live preview text showing current scale
                Text(
                    text = "The quick brown fox jumps over the lazy dog.",
                    color = TextPrimary,
                    fontSize = (16.sp * uiState.fontSizeScale),
                    fontWeight = FontWeight.Normal,
                    lineHeight = ((16.sp * uiState.fontSizeScale) * 1.4f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Preview",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Bottom padding to avoid content being cut off
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
