package com.zixo.app.ui.settings.SubPages

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zixo.app.domain.model.EphemeralTimerOption
import com.zixo.app.domain.model.LastSeenVisibility
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.ui.components.GlassSegmentedPicker
import com.zixo.app.ui.components.GlassSwitch
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.screens.settings.SettingsViewModel
import com.zixo.app.ui.theme.DarkPetrolCharcoal
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
 * Sections:
 * 1. Presence Controls – last seen & online visibility
 * 2. Profile Data Visibility – photo, about, status sharing
 * 3. Messaging Privacy – read receipts, ephemeral timer
 * 4. App Protection – screen lock
 * 5. Advanced Privacy – IP protection, link previews
 *
 * @param onBackClick Callback invoked when the user taps the back arrow.
 */
@Composable
fun PrivacyCenterScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Local state for features not yet in SettingsUiState ──
    var profilePhotoVisibility by rememberSaveable {
        mutableStateOf(VisibilityOption.EVERYONE)
    }
    var aboutVisibility by rememberSaveable {
        mutableStateOf(VisibilityOption.EVERYONE)
    }
    var statusPrivacy by rememberSaveable {
        mutableStateOf(StatusPrivacyOption.ALL_CONTACTS)
    }
    var ephemeralDestructTimer by rememberSaveable {
        mutableStateOf(EphemeralTimerOption.OFF)
    }
    var protectIpInCalls by rememberSaveable { mutableStateOf(true) }
    var disableLinkPreviews by rememberSaveable { mutableStateOf(false) }

    // ── Map ViewModel LastSeenVisibility ↔ picker index ──
    val lastSeenOptions = listOf("Everyone", "My Contacts", "Nobody")
    val lastSeenSelectedIndex = when (uiState.lastSeenVisibility) {
        LastSeenVisibility.EVERYONE -> 0
        LastSeenVisibility.CONTACTS -> 1
        LastSeenVisibility.NOBODY -> 2
    }

    // ── VisibilityOption ↔ picker index helpers ──
    val visibilityOptions = listOf("Everyone", "Contacts", "Nobody")

    val profilePhotoIndex = when (profilePhotoVisibility) {
        VisibilityOption.EVERYONE -> 0
        VisibilityOption.CONTACTS -> 1
        VisibilityOption.NOBODY -> 2
    }

    val aboutIndex = when (aboutVisibility) {
        VisibilityOption.EVERYONE -> 0
        VisibilityOption.CONTACTS -> 1
        VisibilityOption.NOBODY -> 2
    }

    // ── StatusPrivacyOption ↔ picker index helpers ──
    val statusOptions = listOf("All Contacts", "Exclude Some", "Only Share With")
    val statusIndex = when (statusPrivacy) {
        StatusPrivacyOption.ALL_CONTACTS -> 0
        StatusPrivacyOption.EXCLUDE_SOME -> 1
        StatusPrivacyOption.ONLY_SHARE_WITH -> 2
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ZixoTopBar(
                    title = "Privacy Control Center",
                    showBackButton = true,
                    onBackClick = onBackClick
                )

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
                    // ──────────────────────────────────────────────────────────
                    // Section 1: Presence Controls
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                                .padding(16.dp)
                        ) {
                            SectionHeader(title = "Last Seen & Online")

                            Spacer(modifier = Modifier.height(8.dp))

                            GlassSegmentedPicker(
                                options = lastSeenOptions,
                                selectedIndex = lastSeenSelectedIndex,
                                onOptionSelected = { index ->
                                    val visibility = when (index) {
                                        0 -> LastSeenVisibility.EVERYONE
                                        1 -> LastSeenVisibility.CONTACTS
                                        else -> LastSeenVisibility.NOBODY
                                    }
                                    viewModel.setLastSeenVisibility(visibility)
                                }
                            )
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 2: Profile Data Visibility
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                                .padding(16.dp)
                        ) {
                            SectionHeader(title = "Who Can See My Info")

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Profile Photo ──
                            PrivacyPickerRow(
                                label = "Profile Photo",
                                options = visibilityOptions,
                                selectedIndex = profilePhotoIndex,
                                onOptionSelected = { index ->
                                    profilePhotoVisibility = when (index) {
                                        0 -> VisibilityOption.EVERYONE
                                        1 -> VisibilityOption.CONTACTS
                                        else -> VisibilityOption.NOBODY
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── About / Bio ──
                            PrivacyPickerRow(
                                label = "About / Bio",
                                options = visibilityOptions,
                                selectedIndex = aboutIndex,
                                onOptionSelected = { index ->
                                    aboutVisibility = when (index) {
                                        0 -> VisibilityOption.EVERYONE
                                        1 -> VisibilityOption.CONTACTS
                                        else -> VisibilityOption.NOBODY
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Status Privacy ──
                            PrivacyPickerRow(
                                label = "Status Privacy",
                                options = statusOptions,
                                selectedIndex = statusIndex,
                                onOptionSelected = { index ->
                                    statusPrivacy = when (index) {
                                        0 -> StatusPrivacyOption.ALL_CONTACTS
                                        1 -> StatusPrivacyOption.EXCLUDE_SOME
                                        else -> StatusPrivacyOption.ONLY_SHARE_WITH
                                    }
                                }
                            )
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 3: Messaging Privacy
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                                .padding(16.dp)
                        ) {
                            SectionHeader(title = "Messaging")

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Read Receipts ──
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
                                    checked = uiState.readReceiptsEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setReadReceiptsEnabled(enabled)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // ── Ephemeral Default Timer ──
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Ephemeral Default Timer",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Automatically delete new messages after a set time",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                EphemeralTimerDropdown(
                                    selectedOption = ephemeralDestructTimer,
                                    onOptionSelected = { ephemeralDestructTimer = it }
                                )
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 4: App Protection
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                                .padding(16.dp)
                        ) {
                            SectionHeader(title = "App Protection")

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
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
                                    checked = uiState.screenLockEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setScreenLockEnabled(enabled)
                                    }
                                )
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Section 5: Advanced Privacy
                    // ──────────────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard()
                                .padding(16.dp)
                        ) {
                            SectionHeader(title = "Advanced")

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Protect IP in Calls ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
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
                                    checked = protectIpInCalls,
                                    onCheckedChange = { enabled ->
                                        protectIpInCalls = enabled
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Disable Link Previews ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
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
                                    checked = disableLinkPreviews,
                                    onCheckedChange = { enabled ->
                                        disableLinkPreviews = enabled
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Privacy Picker Row – Label + GlassSegmentedPicker combo
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A labeled row that displays a [GlassSegmentedPicker] below a bold label.
 * Used for visibility and privacy option pickers in the Privacy Control Center.
 *
 * @param label            The row label displayed above the picker.
 * @param options          The list of string options for the segmented picker.
 * @param selectedIndex    The currently selected index.
 * @param onOptionSelected Callback invoked with the new index when the user selects an option.
 */
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
// Ephemeral Timer Dropdown
// ─────────────────────────────────────────────────────────────────────────────

/**
 * An [ExposedDropdownMenuBox] dropdown styled with the glass aesthetic that
 * allows the user to select from [EphemeralTimerOption] values.
 *
 * @param selectedOption   The currently selected timer option.
 * @param onOptionSelected Callback invoked when the user picks a new option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EphemeralTimerDropdown(
    selectedOption: EphemeralTimerOption,
    onOptionSelected: (EphemeralTimerOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allOptions = EphemeralTimerOption.entries

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedOption.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextSecondary.copy(alpha = 0.5f),
                focusedContainerColor = DarkPetrolCharcoal.copy(alpha = 0.5f),
                unfocusedContainerColor = DarkPetrolCharcoal.copy(alpha = 0.5f),
                disabledContainerColor = DarkPetrolCharcoal.copy(alpha = 0.3f),
                cursorColor = NeonMint,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTrailingIconColor = TextSecondary,
                unfocusedTrailingIconColor = TextSecondary
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF152530),
            shape = RoundedCornerShape(12.dp)
        ) {
            allOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (option == selectedOption) NeonMint else TextPrimary,
                            fontWeight = if (option == selectedOption) FontWeight.SemiBold
                                         else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
