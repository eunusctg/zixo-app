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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Close
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
import com.zixo.app.ui.theme.EmeraldGreen
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ──────────────────────────────────────────────
// Find Contact Dialog
// ──────────────────────────────────────────────

/**
 * A floating search overlay dialog that allows finding users by their
 * exact 8-digit Zixo Number only.
 *
 * Uses the Liquid Glass design system with animated transitions between
 * search states: Idle → Searching → Found/NotFound.
 *
 * @param searchResult   The current search result state from the ViewModel.
 * @param addContactState The current add-contact operation state.
 * @param onSearch       Callback invoked when the user submits a valid 8-digit number.
 * @param onAddContact   Callback invoked when the user taps "Add Contact".
 * @param onDismiss      Callback invoked when the dialog is dismissed.
 * @param onResetSearch  Callback invoked to reset the search state.
 */
@Composable
fun FindContactDialog(
    searchResult: ContactSearchResult,
    addContactState: AddContactState,
    onSearch: (String) -> Unit,
    onAddContact: (uid: String) -> Unit,
    onDismiss: () -> Unit,
    onResetSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zixoNumberInput by remember { mutableStateOf("") }

    // Trigger search when exactly 8 digits are entered
    LaunchedEffect(zixoNumberInput) {
        if (zixoNumberInput.length == 8) {
            onSearch(zixoNumberInput)
        }
    }

    // Reset input when dialog reopens
    LaunchedEffect(Unit) {
        zixoNumberInput = ""
        onResetSearch()
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Validation / Status Label ─────────────
            AnimatedContent(
                targetState = when {
                    addContactState is AddContactState.Success -> "added"
                    addContactState is AddContactState.AlreadyAdded -> "already_added"
                    zixoNumberInput.isEmpty() -> "hint"
                    zixoNumberInput.length < 8 -> "invalid"
                    searchResult is ContactSearchResult.Searching -> "searching"
                    searchResult is ContactSearchResult.NotFound -> "not_found"
                    searchResult is ContactSearchResult.InvalidFormat -> "invalid_format"
                    searchResult is ContactSearchResult.Error -> "error"
                    searchResult is ContactSearchResult.Found -> "found"
                    else -> "hint"
                },
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "search_status_transition"
            ) { state ->
                val (text, color) = when (state) {
                    "hint" -> "Enter 8-digit Zixo Number" to TextSecondary
                    "invalid" -> "Invalid format" to TextSecondary.copy(alpha = 0.7f)
                    "invalid_format" -> "Invalid format" to TextSecondary.copy(alpha = 0.7f)
                    "searching" -> "Searching..." to NeonMint
                    "not_found" -> "Not found" to TextSecondary.copy(alpha = 0.8f)
                    "error" -> (searchResult as? ContactSearchResult.Error)?.message?.let { "Error: $it" } to TextSecondary
                    "found" -> "User found" to NeonMint
                    "added" -> "Contact added!" to NeonMint
                    "already_added" -> "Already in contacts" to TextSecondary
                    else -> "Enter 8-digit Zixo Number" to TextSecondary
                }
                Text(
                    text = text ?: "Enter 8-digit Zixo Number",
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
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

            // ── Success Confirmation ──────────────────
            AnimatedVisibility(
                visible = addContactState is AddContactState.Success ||
                        addContactState is AddContactState.AlreadyAdded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = NeonMint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (addContactState is AddContactState.AlreadyAdded)
                            "Already in your contacts"
                        else
                            "Contact added successfully!",
                        color = NeonMint,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
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
 * - A stylish vector Zixo icon identifier (VerifiedUser in emerald green)
 * - The found user's Display Name
 * - The formatted 8-digit Zixo Number
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
        // ── Avatar + Verified Icon Row ───────────
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
                text = if (isAdding) "Adding..." else "Add Contact",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
