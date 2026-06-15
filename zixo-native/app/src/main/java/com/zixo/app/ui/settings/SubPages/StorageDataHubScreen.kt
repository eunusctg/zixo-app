package com.zixo.app.ui.settings.SubPages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.ConversationStorageEntry
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StorageBreakdown
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.GlassCheckBox
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ══════════════════════════════════════════════════════════════════════════
// Storage & Data Optimization Hub
// ══════════════════════════════════════════════════════════════════════════

/**
 * Full-screen hub for storage analytics, auto-download rules, and
 * upload quality configuration. All bound to [SettingsViewModel].
 *
 * @param onBackClick  Callback invoked when the user taps the back arrow.
 * @param viewModel    Hilt-injected [SettingsViewModel].
 */
@Composable
fun StorageDataHubScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val conversationStorage by viewModel.conversationStorage.collectAsStateWithLifecycle()

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
                    title = "Storage & Data",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Section 1: Storage Breakdown ─────────────────────────────
            item {
                NetworkMetricsCard(breakdown = storageBreakdown)
            }

            // ── Section 2: Storage Explorer ──────────────────────────────
            item {
                StorageExplorerCard(
                    breakdown = storageBreakdown,
                    conversations = conversationStorage,
                    onClearCache = { viewModel.clearCache() }
                )
            }

            // ── Section 3: Auto-Download Matrix ─────────────────────────
            item {
                AutoDownloadCard(
                    mobileTypes = settingsState.autoDownloadMobile,
                    wifiTypes = settingsState.autoDownloadWifi,
                    roamingTypes = settingsState.autoDownloadRoaming,
                    onMobileToggle = { viewModel.updateAutoDownloadMobile(it) },
                    onWifiToggle = { viewModel.updateAutoDownloadWifi(it) },
                    onRoamingToggle = { viewModel.updateAutoDownloadRoaming(it) }
                )
            }

            // ── Section 4: Upload Quality ───────────────────────────────
            item {
                UploadQualityCard(
                    quality = settingsState.mediaUploadQuality,
                    onQualityChange = { viewModel.updateMediaUploadQuality(it) }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Section 1 — Storage Breakdown Card
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun NetworkMetricsCard(breakdown: StorageBreakdown) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("STORAGE BREAKDOWN")

        Spacer(modifier = Modifier.height(12.dp))

        NetworkMetricRow(
            icon = Icons.Filled.Call,
            label = "Calls",
            valueMB = breakdown.callsMB
        )
        NetworkMetricRow(
            icon = Icons.Filled.Chat,
            label = "Messages",
            valueMB = breakdown.messagesMB
        )
        NetworkMetricRow(
            icon = Icons.Filled.CloudUpload,
            label = "Status Uploads",
            valueMB = breakdown.statusUploadsMB
        )
        NetworkMetricRow(
            icon = Icons.Filled.CloudQueue,
            label = "Cloud Sync Media",
            valueMB = breakdown.cloudSyncMB
        )

        HorizontalDivider(
            color = TextSecondary.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatMB(breakdown.totalMB),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NetworkMetricRow(
    icon: ImageVector,
    label: String,
    valueMB: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonMint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        Text(
            text = formatMB(valueMB),
            color = TextPrimary,
            fontSize = 14.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Section 2 — Storage Explorer
// ══════════════════════════════════════════════════════════════════════════

private const val MAX_STORAGE_MB = 500f

@Composable
private fun StorageExplorerCard(
    breakdown: StorageBreakdown,
    conversations: List<ConversationStorageEntry>,
    onClearCache: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("STORAGE USAGE")

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        val progress = (breakdown.totalMB / MAX_STORAGE_MB).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TextSecondary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(NeonMint, NeonMint.copy(alpha = 0.7f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${formatMB(breakdown.totalMB)} used",
            color = TextSecondary,
            fontSize = 12.sp
        )

        // Conversation list
        if (conversations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            val topConversations = conversations.take(10)
            topConversations.forEachIndexed { index, entry ->
                ConversationStorageRow(entry = entry)
                if (index < topConversations.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Clear cache button
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onClearCache,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = NeonMint
            ),
            border = BorderStroke(1.dp, NeonMint)
        ) {
            Text(
                text = "Cache Vacuum Purge",
                color = NeonMint,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ConversationStorageRow(entry: ConversationStorageEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            AvatarComponent(
                imageUrl = entry.avatarUrl,
                name = entry.displayName,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = entry.displayName,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        Text(
            text = formatMB(entry.storageMB),
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Section 3 — Media Auto-Download Matrix
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun AutoDownloadCard(
    mobileTypes: Set<MediaType>,
    wifiTypes: Set<MediaType>,
    roamingTypes: Set<MediaType>,
    onMobileToggle: (Set<MediaType>) -> Unit,
    onWifiToggle: (Set<MediaType>) -> Unit,
    onRoamingToggle: (Set<MediaType>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("AUTO-DOWNLOAD MEDIA")

        Spacer(modifier = Modifier.height(12.dp))

        // Mobile Data
        SubHeaderLabel("When using Mobile Data")
        Spacer(modifier = Modifier.height(8.dp))
        MediaType.entries.forEach { mediaType ->
            MediaCheckBoxRow(
                label = mediaType.displayLabel,
                checked = mediaType in mobileTypes,
                onCheckedChange = { checked ->
                    val newSet = if (checked) mobileTypes + mediaType else mobileTypes - mediaType
                    onMobileToggle(newSet)
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
                checked = mediaType in wifiTypes,
                onCheckedChange = { checked ->
                    val newSet = if (checked) wifiTypes + mediaType else wifiTypes - mediaType
                    onWifiToggle(newSet)
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
                checked = mediaType in roamingTypes,
                onCheckedChange = { checked ->
                    val newSet = if (checked) roamingTypes + mediaType else roamingTypes - mediaType
                    onRoamingToggle(newSet)
                }
            )
        }
    }
}

private val MediaType.displayLabel: String
    get() = when (this) {
        MediaType.PHOTO -> "Photos"
        MediaType.AUDIO -> "Audio"
        MediaType.VIDEO -> "Videos"
        MediaType.DOCUMENT -> "Documents"
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
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp
        )
        GlassCheckBox(
            checked = checked,
            onCheckedChange = { isChecked -> onCheckedChange(isChecked == true) }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Section 4 — Upload Quality
// ══════════════════════════════════════════════════════════════════════════

private val UploadQualityOptions = listOf("Auto", "Best Quality", "Balanced")

private fun UploadQuality.toIndex(): Int = when (this) {
    UploadQuality.AUTO -> 0
    UploadQuality.BEST_QUALITY -> 1
    UploadQuality.BALANCED -> 2
}

private fun Int.toUploadQuality(): UploadQuality = when (this) {
    0 -> UploadQuality.AUTO
    1 -> UploadQuality.BEST_QUALITY
    2 -> UploadQuality.BALANCED
    else -> UploadQuality.AUTO
}

@Composable
private fun UploadQualityCard(
    quality: UploadQuality,
    onQualityChange: (UploadQuality) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        SectionLabel("UPLOAD QUALITY")

        Spacer(modifier = Modifier.height(12.dp))

        GlassSegmentedPicker(
            options = UploadQualityOptions,
            selectedIndex = quality.toIndex(),
            onOptionSelected = { index -> onQualityChange(index.toUploadQuality()) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Auto: Adjusts quality based on network\n" +
                    "Best Quality: Highest resolution\n" +
                    "Balanced: Standard optimized compression",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// Shared UI Helpers
// ══════════════════════════════════════════════════════════════════════════

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

private fun formatMB(mb: Float): String {
    return if (mb < 0.1f) "0.0 MB" else "%.1f MB".format(mb)
}
