package com.zixo.app.ui.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.TopBarAction
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.EmeraldGreen
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ════════════════════════════════════════════════════════════════
// Contact List Screen
// ════════════════════════════════════════════════════════════════

/**
 * Full-screen contact list showing all verified mutual contacts.
 *
 * Features:
 * - [ZixoGlassBackground] animated background
 * - [ZixoTopBar] with "Contacts" title and search icon to open [FindContactDialog]
 * - LazyColumn displaying contacts sorted by pinned status then alphabetically
 * - Each contact item uses [liquidGlassCard] modifier showing:
 *   - Avatar with online indicator dot
 *   - Display name + formatted Zixo Number
 *   - Mutual verification badge (checkmark)
 *   - Last seen timestamp
 * - Long-press menu with Block and Remove contact options
 * - Pull-to-refresh support
 * - Empty state with centered text and a "Find Contacts" button
 * - Floating "Find Contact" FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onContactClick: (contactUserId: String) -> Unit = {},
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val addContactState by viewModel.addContactState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showFindDialog by remember { mutableStateOf(false) }
    var showContactActions by remember { mutableStateOf<ContactModel?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle snackbar messages from ViewModel
    LaunchedEffect(snackbarMessage) {
        try {
            snackbarMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.clearSnackbar()
            }
        } catch (_: Exception) {
            viewModel.clearSnackbar()
        }
    }

    // Sort contacts: pinned first, then alphabetically by display name
    val sortedContacts = remember(contacts) {
        contacts
            .filter { !it.isBlocked }
            .sortedWith(
                compareByDescending<ContactModel> { it.isPinned }
                    .thenBy { it.contactDisplayName.lowercase() }
            )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Top Bar ─────────────────────────────
            ZixoTopBar(
                title = "Contacts",
                actionIcons = listOf(
                    TopBarAction(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Find contact",
                        onClick = { showFindDialog = true }
                    )
                )
            )

            // ── Pull-to-Refresh + Contact List ──────
            val pullToRefreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    contacts.isEmpty() -> {
                        // ── Empty State ──────────────────
                        EmptyContactsState(
                            onFindContacts = { showFindDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        // ── Contact List ─────────────────
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 16.dp // Parent already pads for bottom nav
                            )
                        ) {
                            items(
                                items = sortedContacts,
                                key = { it.id }
                            ) { contact ->
                                ContactListItem(
                                    contact = contact,
                                    onClick = {
                                        if (contact.isMutual) {
                                            onContactClick(contact.contactUserId)
                                        }
                                    },
                                    onLongPress = {
                                        showContactActions = contact
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Snackbar overlay ────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ── Find Contact Dialog Overlay ────────────
        if (showFindDialog) {
            FindContactDialog(
                searchResult = searchResult,
                addContactState = addContactState,
                onSearch = viewModel::searchByZixoNumber,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onAddContact = viewModel::addContact,
                onDismiss = {
                    showFindDialog = false
                    viewModel.resetSearch()
                },
                onResetSearch = viewModel::resetSearch
            )
        }

        // ── Contact Actions Bottom Sheet ───────────
        showContactActions?.let { contact ->
            ContactActionsSheet(
                contact = contact,
                onBlock = {
                    viewModel.blockContact(contact.contactUserId)
                    showContactActions = null
                },
                onRemove = {
                    viewModel.removeContact(contact.contactUserId)
                    showContactActions = null
                },
                onDismiss = { showContactActions = null }
            )
        }
    }
}

// ──────────────────────────────────────────────
// Contact List Item
// ──────────────────────────────────────────────

/**
 * A single contact row displayed in the contact list.
 *
 * Uses [liquidGlassCard] modifier for the glass aesthetic and shows:
 * - Circular avatar with online indicator (green dot when online)
 * - Display name and formatted Zixo Number
 * - Mutual verification badge (checkmark if [ContactModel.isMutual])
 * - Last seen timestamp
 * - Pinned indicator icon if pinned
 * - Tap to open chat with the contact (only if mutual)
 * - Long-press to open context menu (Block, Remove)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactListItem(
    contact: ContactModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .liquidGlassCard()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Avatar ─────────────────────────────────
        AvatarComponent(
            imageUrl = contact.contactAvatarUrl,
            name = contact.contactDisplayName,
            isOnline = contact.isOnline,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // ── Name + Zixo Number + Last Seen ─────────
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.contactDisplayName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // ── Mutual Verification Badge ─────────
                if (contact.isMutual) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = "Mutual contact",
                        tint = NeonMint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // ── Pinned Indicator ──────────────────
                if (contact.isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = contact.formattedZixoNumber,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            // ── Last Seen Timestamp ─────────────────
            if (contact.lastSeenTimestamp > 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatLastSeen(contact.lastSeenTimestamp, contact.isOnline),
                    color = if (contact.isOnline) NeonMint else TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Contact Actions Bottom Sheet
// ──────────────────────────────────────────────

/**
 * Bottom sheet with Block and Remove contact actions.
 * Triggered by long-pressing a contact in the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactActionsSheet(
    contact: ContactModel,
    onBlock: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkPetrolCharcoal,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Contact Header ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarComponent(
                    imageUrl = contact.contactAvatarUrl,
                    name = contact.contactDisplayName,
                    size = 40.dp,
                    isOnline = contact.isOnline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = contact.contactDisplayName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = contact.formattedZixoNumber,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // ── Block Action ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBlock)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Block ${contact.contactDisplayName}",
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Remove Action ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Remove contact",
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Empty State
// ──────────────────────────────────────────────

/**
 * Centered empty state displayed when the user has no contacts yet.
 * Includes a "Find Contacts" button that opens the [FindContactDialog].
 */
@Composable
private fun EmptyContactsState(
    onFindContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No contacts yet",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Find people by their Zixo Number",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            FloatingActionButton(
                onClick = onFindContacts,
                modifier = Modifier.clip(CircleShape),
                containerColor = NeonMint,
                contentColor = DarkPetrolCharcoal,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = DarkPetrolCharcoal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Find Contacts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkPetrolCharcoal
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Last Seen Formatting
// ──────────────────────────────────────────────

private fun formatLastSeen(timestamp: Long, isOnline: Boolean): String {
    if (isOnline) return "Online"
    val instant = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    val duration = Duration.between(instant, now)
    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    val days = duration.toDays()

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> {
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            "last seen ${DateTimeFormatter.ofPattern("h:mm a").format(dateTime)}"
        }
        days == 1L -> "last seen yesterday"
        days < 7L -> "last seen ${days}d ago"
        else -> {
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            "last seen ${DateTimeFormatter.ofPattern("MMM d").format(dateTime)}"
        }
    }
}
