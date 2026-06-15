package com.zixo.app.ui.contacts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zixo.app.domain.model.AddContactState
import com.zixo.app.domain.model.ContactPreviewProfile
import com.zixo.app.domain.model.ContactSearchResult
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.theme.DestructiveText
import com.zixo.app.ui.theme.EmeraldGreen
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ──────────────────────────────────────────────
// Find Contact Dialog
// ──────────────────────────────────────────────

/**
 * A floating search overlay dialog that allows finding users by their
 * exact 8-digit Zixo Number only — enforcing the zero-trust contact model.
 *
 * Uses the Liquid Glass design system with animated transitions between
 * search states: Idle → Searching → Found / NotFound / Error.
 *
 * Auto-triggers search with debounce when exactly 8 digits are entered.
 * Shows red error indicator for invalid format. Displays "Already a Contact"
 * in green when the user has already been added.
 *
 * @param searchResult    The current search result state from the ViewModel.
 * @param addContactState The current add-contact operation state.
 * @param onSearch        Callback invoked when the user submits a valid 8-digit number.
 * @param onSearchQueryChanged Callback invoked on every keystroke for debounced search.
 * @param onAddContact    Callback invoked when the user taps "Add Contact".
 * @param onDismiss       Callback invoked when the dialog is dismissed.
 * @param onResetSearch   Callback invoked to reset the search state.
 */
@Composable
fun FindContactDialog(
    searchResult: ContactSearchResult,
    addContactState: AddContactState,
    onSearch: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddContact: (uid: String) -> Unit,
    onDismiss: () -> Unit,
    onResetSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zixoNumberInput by remember { mutableStateOf("") }

    // Auto-trigger search when exactly 8 digits are entered (debounced in ViewModel)
    LaunchedEffect(zixoNumberInput) {
        try {
            onSearchQueryChanged(zixoNumberInput)
            if (zixoNumberInput.length == 8 && zixoNumberInput.all { it.isDigit() }) {
                onSearch(zixoNumberInput)
            }
        } catch (_: Exception) {
            // Search trigger failed — non-critical
        }
    }

    // Reset input when dialog opens
    LaunchedEffect(Unit) {
        try {
            zixoNumberInput = ""
            onResetSearch()
        } catch (_: Exception) {
            // Reset failed — non-critical
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .liquidGlassContainer()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header Row ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Find Contact",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Zixo Number Input Field ───────────────
            val isInvalidFormat = zixoNumberInput.isNotEmpty() &&
                    (zixoNumberInput.length < 8 || !zixoNumberInput.all { it.isDigit() })

            GlassOutlinedTextField(
                value = zixoNumberInput,
                onValueChange = { input ->
                    // Only allow digits, max 8 characters
                    val filtered = input.filter { it.isDigit() }
                    zixoNumberInput = filtered.take(8)
                    if (filtered.length < 8) {
                        onResetSearch()
                    }
                },
                placeholder = {
                    Text(
                        text = "e.g. 1234 5678",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                maxLength = 8,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                trailingIcon = if (searchResult is ContactSearchResult.InvalidFormat || isInvalidFormat) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Invalid format",
                            tint = DestructiveText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (searchResult is ContactSearchResult.Found) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Found",
                            tint = NeonMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Validation / Status Label ─────────────
            AnimatedContent(
                targetState = when {
                    addContactState is AddContactState.Success -> "added"
                    addContactState is AddContactState.AlreadyAdded -> "already_added"
                    zixoNumberInput.isEmpty() -> "hint"
                    zixoNumberInput.length < 8 -> "partial"
                    searchResult is ContactSearchResult.InvalidFormat -> "invalid_format"
                    searchResult is ContactSearchResult.Searching -> "searching"
                    searchResult is ContactSearchResult.NotFound -> "not_found"
                    searchResult is ContactSearchResult.Error -> "error"
                    searchResult is ContactSearchResult.Found -> "found"
                    else -> "hint"
                },
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                },
                label = "search_status_transition"
            ) { state ->
                val (text, color, icon) = when (state) {
                    "hint" -> Triple(
                        "Enter 8-digit Zixo Number",
                        TextSecondary,
                        null
                    )
                    "partial" -> Triple(
                        "${zixoNumberInput.length}/8 digits",
                        TextSecondary.copy(alpha = 0.7f),
                        null
                    )
                    "invalid_format" -> Triple(
                        "Enter a valid 8-digit Zixo Number",
                        DestructiveText,
                        Icons.Outlined.ErrorOutline
                    )
                    "searching" -> Triple(
                        "Searching\u2026",
                        NeonMint,
                        null
                    )
                    "not_found" -> Triple(
                        "Zixo Number not found",
                        TextSecondary.copy(alpha = 0.8f),
                        null
                    )
                    "error" -> Triple(
                        (searchResult as? ContactSearchResult.Error)?.message?.let { "Error: $it" }
                            ?: "Search failed",
                        DestructiveText,
                        Icons.Outlined.ErrorOutline
                    )
                    "found" -> Triple(
                        "User found",
                        NeonMint,
                        Icons.Outlined.CheckCircle
                    )
                    "added" -> Triple(
                        "Contact added successfully!",
                        NeonMint,
                        Icons.Outlined.CheckCircle
                    )
                    "already_added" -> Triple(
                        "Already a Contact",
                        EmeraldGreen,
                        Icons.Outlined.CheckCircle
                    )
                    else -> Triple(
                        "Enter 8-digit Zixo Number",
                        TextSecondary,
                        null
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = text,
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Search Progress Indicator ─────────────
            AnimatedVisibility(
                visible = searchResult is ContactSearchResult.Searching,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                CircularProgressIndicator(
                    color = NeonMint,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }

            // ── Found Profile Result ──────────────────
            AnimatedVisibility(
                visible = searchResult is ContactSearchResult.Found &&
                        addContactState !is AddContactState.Success &&
                        addContactState !is AddContactState.AlreadyAdded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                val profile = (searchResult as? ContactSearchResult.Found)?.previewProfile
                if (profile != null) {
                    FoundProfileSnippet(
                        profile = profile,
                        isAdding = addContactState is AddContactState.Adding,
                        onAddContact = { onAddContact(profile.uid) }
                    )
                }
            }

            // ── Success / Already-Added Confirmation ──
            AnimatedVisibility(
                visible = addContactState is AddContactState.Success ||
                        addContactState is AddContactState.AlreadyAdded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                val isAlreadyAdded = addContactState is AddContactState.AlreadyAdded
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isAlreadyAdded) EmeraldGreen.copy(alpha = 0.08f)
                            else NeonMint.copy(alpha = 0.08f)
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = if (isAlreadyAdded) EmeraldGreen else NeonMint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAlreadyAdded) "Already a Contact"
                        else "Contact added successfully!",
                        color = if (isAlreadyAdded) EmeraldGreen else NeonMint,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Found Profile Snippet
// ──────────────────────────────────────────────

/**
 * Displays a profile snippet for a found user, including:
 * - Avatar with the found user's initial/photo
 * - Display name with a verified Zixo icon
 * - Formatted 8-digit Zixo Number (XXXX XXXX)
 * - Bio preview (first line)
 * - An active "Add Contact" button styled in NeonMint green
 */
@Composable
private fun FoundProfileSnippet(
    profile: ContactPreviewProfile,
    isAdding: Boolean,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EmeraldGreen.copy(alpha = 0.08f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Avatar + Info Row ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarComponent(
                imageUrl = profile.avatarUrl,
                name = profile.displayName,
                size = 52.dp,
                isOnline = false
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.displayName,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = "Verified Zixo User",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = profile.formattedZixoNumber,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.bio,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Add Contact Button ──────────────────
        Button(
            onClick = onAddContact,
            enabled = !isAdding,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonMint,
                contentColor = NeonMint.copy(alpha = 0.15f),
                disabledContainerColor = NeonMint.copy(alpha = 0.5f),
                disabledContentColor = TextSecondary
            )
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = if (isAdding) "Adding\u2026" else "Add Contact",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
