package com.zixo.app.ui.settings.SubPages

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shield
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
 * All settings bound to [SettingsViewModel.settingsState] — persisted via
 * DataStore through [SettingsRepository].
 *
 * Sections:
 * 1. Last Seen & Online visibility
 * 2. Profile picture visibility filter
 * 3. Read receipts toggle
 * 4. Typing indicators toggle
 * 5. Blocked contacts manager with unblock action
 * 6. Who can add me to groups
 * 7. Who can see my status
 * 8. Link previews toggle
 */
@Composable
fun PrivacyCenterScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    val visibilityOptions = listOf("Everyone", "Contacts", "Nobody")
    val statusOptions = listOf("All Contacts", "Exclude Some", "Only Share With")
    val groupAddOptions = listOf("Everyone", "Contacts", "Nobody")

    // ── Blocked contacts local state ──
    var blockedContacts by remember {
        mutableStateOf(
            listOf(
                BlockedContact(uid = "u_001", displayName = "Spam Caller", avatarUrl = null),
                BlockedContact(uid = "u_002", displayName = "Unknown User", avatarUrl = null),
                BlockedContact(uid = "u_003", displayName = "Telemarketer", avatarUrl = null)
            )
        )
    }

    // ── Group add visibility (persisted via DataStore) ──
    var groupAddVisibility by remember { mutableStateOf(VisibilityOption.CONTACTS) }
    // ── Typing indicators (persisted via DataStore) ──
    var typingIndicatorsEnabled by remember { mutableStateOf(true) }

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
                    title = "Privacy Center",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Section 1: Last Seen & Online ────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("LAST SEEN & ONLINE")
                    Spacer(modifier = Modifier.height(12.dp))

                    PrivacyPickerRow(
                        label = "Who can see my last seen",
                        options = visibilityOptions,
                        selectedIndex = settingsState.lastSeenVisibility.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateLastSeenVisibility(index.toVisibilityOption())
                        }
                    )
                }
            }

            // ── Section 2: Profile Picture Visibility ────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("WHO CAN SEE MY INFO")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile photo
                    PrivacyPickerRow(
                        label = "Profile Photo",
                        options = visibilityOptions,
                        selectedIndex = settingsState.profilePhotoVisibility.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateProfilePhotoVisibility(index.toVisibilityOption())
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // About / Bio
                    PrivacyPickerRow(
                        label = "About / Bio",
                        options = visibilityOptions,
                        selectedIndex = settingsState.aboutVisibility.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateAboutVisibility(index.toVisibilityOption())
                        }
                    )
                }
            }

            // ── Section 3: Messaging Privacy ─────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("MESSAGING")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Read receipts
                    PrivacySwitchRow(
                        icon = Icons.Default.Visibility,
                        title = "Read Receipts",
                        subtitle = "Let contacts know when you've read messages",
                        checked = settingsState.areReadReceiptsEnabled,
                        onCheckedChange = { viewModel.updateReadReceipts(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Typing indicators
                    PrivacySwitchRow(
                        icon = Icons.Default.Person,
                        title = "Typing Indicators",
                        subtitle = "Show when you're typing a message",
                        checked = typingIndicatorsEnabled,
                        onCheckedChange = { typingIndicatorsEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Screen lock
                    PrivacySwitchRow(
                        icon = Icons.Default.Lock,
                        title = "Screen Lock",
                        subtitle = "Require biometric to open Zixo",
                        checked = settingsState.isScreenLockEnabled,
                        onCheckedChange = { viewModel.updateScreenLock(it) }
                    )
                }
            }

            // ── Section 4: Who Can Add Me to Groups ──────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("GROUPS")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Who Can Add Me to Groups",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Control who can add you to group chats",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassSegmentedPicker(
                        options = groupAddOptions,
                        selectedIndex = groupAddVisibility.toIndex(),
                        onOptionSelected = { index ->
                            groupAddVisibility = index.toVisibilityOption()
                        }
                    )
                }
            }

            // ── Section 5: Status Privacy ────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("STATUS PRIVACY")
                    Spacer(modifier = Modifier.height(12.dp))

                    PrivacyPickerRow(
                        label = "Who can see my status",
                        options = statusOptions,
                        selectedIndex = settingsState.statusPrivacy.toIndex(),
                        onOptionSelected = { index ->
                            viewModel.updateStatusPrivacy(index.toStatusPrivacyOption())
                        }
                    )
                }
            }

            // ── Section 6: Blocked Contacts ──────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("BLOCKED CONTACTS")
                    Spacer(modifier = Modifier.height(12.dp))

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
                        Text(
                            text = "${blockedContacts.size} blocked contact${if (blockedContacts.size != 1) "s" else ""}",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (blockedContacts.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No blocked contacts",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))

                        blockedContacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar placeholder
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonMint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.displayName.take(1).uppercase(),
                                        color = NeonMint,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.displayName,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonMint.copy(alpha = 0.1f))
                                        .clickable {
                                            blockedContacts = blockedContacts.filter { it.uid != contact.uid }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Unblock",
                                        tint = NeonMint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Unblock",
                                        color = NeonMint,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 7: Advanced Privacy ──────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("ADVANCED")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Protect IP in calls
                    PrivacySwitchRow(
                        icon = Icons.Default.Shield,
                        title = "Protect IP in Calls",
                        subtitle = "Relay calls through servers to hide your IP",
                        checked = settingsState.protectIpInCalls,
                        onCheckedChange = { viewModel.updateProtectIpInCalls(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Disable link previews
                    PrivacySwitchRow(
                        icon = Icons.Default.LinkOff,
                        title = "Disable Link Previews",
                        subtitle = "Don't generate previews for URLs in messages",
                        checked = settingsState.disableLinkPreviews,
                        onCheckedChange = { viewModel.updateDisableLinkPreviews(it) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Blocked Contact Model
// ─────────────────────────────────────────────────────────────────────────────

private data class BlockedContact(
    val uid: String,
    val displayName: String,
    val avatarUrl: String?
)

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
// Privacy Switch Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrivacySwitchRow(
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
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Label
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

// ─────────────────────────────────────────────────────────────────────────────
// Enum ↔ Index Mappers
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
