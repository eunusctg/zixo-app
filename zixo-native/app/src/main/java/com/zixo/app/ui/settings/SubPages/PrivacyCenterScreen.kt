package com.zixo.app.ui.settings.SubPages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
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

            // ── Section 8: Premium PSTN Calling ─────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("PREMIUM CALLING")
                    Spacer(modifier = Modifier.height(16.dp))

                    PrivacySwitchRow(
                        icon = Icons.Default.Call,
                        title = "Receive regular calls via Zixo number",
                        subtitle = if (settingsState.isPremiumSubscriber) {
                            "Allow PSTN calls to ring through your Zixo number"
                        } else {
                            "Premium feature — subscription required"
                        },
                        checked = settingsState.isIncomingPstnEnabled,
                        onCheckedChange = { viewModel.updateIncomingPstnEnabled(it) }
                    )

                    if (!settingsState.isPremiumSubscriber) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonMint.copy(alpha = 0.1f))
                                .clickable { viewModel.updateIncomingPstnEnabled(true) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonMint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upgrade to Zixo Premium",
                                color = NeonMint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── Premium Paywall Overlay ────────────────────────────────────────
        AnimatedVisibility(
            visible = settingsState.showPremiumPaywall,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            PremiumPaywallOverlay(
                onDismiss = { viewModel.dismissPremiumPaywall() },
                onSubscribe = {
                    viewModel.dismissPremiumPaywall()
                    // TODO: Launch Google Play Billing flow
                }
            )
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

// ─────────────────────────────────────────────────────────────────────────────
// Premium Paywall Overlay — Glassmorphic Subscription Sheet
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ultra-premium glassmorphic subscription purchase sheet overlay.
 *
 * Displayed when a non-premium user attempts to enable the
 * "Receive regular calls via Zixo number" feature. Features:
 * - Liquid Glass card with blur and border
 * - Neon emerald accent highlights
 * - Feature list with check icons
 * - Subscribe button with neon glow
 * - Dismiss via close button or tapping outside
 */
@Composable
private fun PremiumPaywallOverlay(
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x3B1A2A32))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { /* consume clicks inside */ }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Premium icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonMint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Zixo Premium",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unlock PSTN calling and receive regular\nphone calls through your Zixo number",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Feature list
            listOf(
                "Receive PSTN calls via Zixo number",
                "Dedicated phone line forwarding",
                "G.711 & Opus narrowband audio",
                "Priority call routing",
                "Premium support"
            ).forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NeonMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subscribe button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonMint)
                    .clickable(onClick = onSubscribe)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subscribe Now",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Billed through Google Play • Cancel anytime",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
