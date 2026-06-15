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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Privacy Control Center Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "Privacy Control Center" page rendered with the Zixo Liquid
 * Glass design language.
 *
 * All settings bound to [SettingsViewModel.settingsState] — no local state, no dummy data.
 *
 * Sections:
 * 1. Last seen visibility
 * 2. Profile photo & about visibility
 * 3. Status privacy
 * 4. Read receipts & screen lock
 * 5. Protect IP in calls
 * 6. Block list
 */
@Composable
fun PrivacyCenterScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    val visibilityOptions = listOf("Everyone", "Contacts", "Nobody")
    val statusOptions = listOf("All Contacts", "Exclude Some", "Only Share With")

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
                    title = "Privacy Center",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Section 1: Last Seen Visibility ──────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LAST SEEN & ONLINE",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassSegmentedPicker(
                        options = visibilityOptions,
                        selectedIndex = settingsState.lastSeenVisibility.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateLastSeenVisibility(index.toVisibilityOption())
                        }
                    )
                }
            }

            // ── Section 2: Profile Data Visibility ───────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "WHO CAN SEE MY INFO",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile photo visibility
                    PrivacyPickerRow(
                        label = "Profile Photo",
                        options = visibilityOptions,
                        selectedIndex = settingsState.profilePhotoVisibility.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateProfilePhotoVisibility(index.toVisibilityOption())
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // About visibility
                    PrivacyPickerRow(
                        label = "About / Bio",
                        options = visibilityOptions,
                        selectedIndex = settingsState.aboutVisibility.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateAboutVisibility(index.toVisibilityOption())
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status privacy
                    PrivacyPickerRow(
                        label = "Status Privacy",
                        options = statusOptions,
                        selectedIndex = settingsState.statusPrivacy.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateStatusPrivacy(index.toStatusPrivacyOption())
                        }
                    )
                }
            }

            // ── Section 3: Read Receipts & Screen Lock ───────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "MESSAGING",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Read receipts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read Receipts",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Let contacts know when you've read messages",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.areReadReceiptsEnabled,
                            onCheckedChange = { viewModel.updateReadReceipts(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Screen lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen Lock",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Require biometric to open Zixo",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.isScreenLockEnabled,
                            onCheckedChange = { viewModel.updateScreenLock(it) }
                        )
                    }
                }
            }

            // ── Section 4: Advanced Privacy ──────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ADVANCED",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Protect IP in calls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Protect IP in Calls",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Relay calls through servers to hide your IP",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.protectIpInCalls,
                            onCheckedChange = { viewModel.updateProtectIpInCalls(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Disable link previews
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disable Link Previews",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Don't generate previews for URLs in messages",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        GlassSwitch(
                            checked = settingsState.disableLinkPreviews,
                            onCheckedChange = { viewModel.updateDisableLinkPreviews(it) }
                        )
                    }
                }
            }

            // ── Section 5: Block List ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blocked Contacts",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "View and manage your blocked contacts list",
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

// ─────────────────────────────────────────────────────────────────────────────
// Privacy Picker Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrivacyPickerRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlassSegmentedPicker(
            options = options,
            selectedIndex = selectedIndex,
            onOptionSelected = onOptionSelected
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Enum ↔ Index mappers
// ─────────────────────────────────────────────────────────────────────────────

private fun VisibilityOption.toIndex(): Int = when (this) {
    VisibilityOption.EVERYONE -> 0
    VisibilityOption.CONTACTS -> 1
    VisibilityOption.NOBODY -> 2
}

private fun Int.toVisibilityOption(): VisibilityOption = when (this) {
    0 -> VisibilityOption.EVERYONE
    1 -> VisibilityOption.CONTACTS
    else -> VisibilityOption.NOBODY
}

private fun StatusPrivacyOption.toIndex(): Int = when (this) {
    StatusPrivacyOption.ALL_CONTACTS -> 0
    StatusPrivacyOption.EXCLUDE_SOME -> 1
    StatusPrivacyOption.ONLY_SHARE_WITH -> 2
}

private fun Int.toStatusPrivacyOption(): StatusPrivacyOption = when (this) {
    0 -> StatusPrivacyOption.ALL_CONTACTS
    1 -> StatusPrivacyOption.EXCLUDE_SOME
    else -> StatusPrivacyOption.ONLY_SHARE_WITH
}
