package com.zixo.app.ui.contacts

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.AddContactState
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.ContactSearchResult
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.TopBarAction
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.EmeraldGreen
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════════
// Contacts ViewModel
// ════════════════════════════════════════════════════════════════

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactModel>>(emptyList())
    val contacts: StateFlow<List<ContactModel>> = _contacts.asStateFlow()

    private val _searchResult = MutableStateFlow<ContactSearchResult>(ContactSearchResult.Idle)
    val searchResult: StateFlow<ContactSearchResult> = _searchResult.asStateFlow()

    private val _addContactState = MutableStateFlow<AddContactState>(AddContactState.Idle)
    val addContactState: StateFlow<AddContactState> = _addContactState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Observe contacts in real-time from Firestore
        contactRepository.observeContacts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
            .let { contactsFlow ->
                viewModelScope.launch {
                    contactsFlow.collect { contactList ->
                        _contacts.value = contactList
                    }
                }
            }
    }

    /**
     * Searches for a user by their exact 8-digit Zixo Number.
     * The search is only triggered for valid 8-digit numeric input.
     */
    fun searchByZixoNumber(number: String) {
        if (number.length != 8 || !number.all { it.isDigit() }) {
            _searchResult.value = ContactSearchResult.InvalidFormat()
            return
        }

        viewModelScope.launch {
            contactRepository.searchByZixoNumber(number).collect { result ->
                _searchResult.value = result
            }
        }
    }

    /**
     * Adds a contact by their UID after a successful search.
     */
    fun addContact(uid: String) {
        viewModelScope.launch {
            _addContactState.value = AddContactState.Adding
            contactRepository.addContact(uid).collect { state ->
                _addContactState.value = state
            }
        }
    }

    /**
     * Resets the search and add-contact states so the user can
     * start a fresh search.
     */
    fun resetSearch() {
        _searchResult.value = ContactSearchResult.Idle
        _addContactState.value = AddContactState.Idle
    }

    /**
     * Refreshes the contact list from the remote source.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Re-subscribe triggers a fresh Firestore snapshot
            contactRepository.observeContacts().collect { contactList ->
                _contacts.value = contactList
            }
            _isRefreshing.value = false
        }
    }
}

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
 * - Each contact item uses [liquidGlassCard] modifier
 * - Pull-to-refresh support
 * - Empty state with centered text and a "Find Contacts" button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onContactClick: (contactUserId: String) -> Unit = {},
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val addContactState by viewModel.addContactState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showFindDialog by remember { mutableStateOf(false) }

    // Sort contacts: pinned first, then alphabetically by display name
    val sortedContacts = remember(contacts) {
        contacts.sortedWith(
            compareByDescending<ContactModel> { it.isPinned }
                .thenBy { it.contactDisplayName.lowercase() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Column(modifier = Modifier.fillMaxSize()) {
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
                                bottom = 88.dp // Space for bottom nav
                            )
                        ) {
                            items(
                                items = sortedContacts,
                                key = { it.id }
                            ) { contact ->
                                ContactListItem(
                                    contact = contact,
                                    onClick = { onContactClick(contact.contactUserId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Find Contact Dialog Overlay ────────────
        if (showFindDialog) {
            FindContactDialog(
                searchResult = searchResult,
                addContactState = addContactState,
                onSearch = viewModel::searchByZixoNumber,
                onAddContact = viewModel::addContact,
                onDismiss = {
                    showFindDialog = false
                    viewModel.resetSearch()
                },
                onResetSearch = viewModel::resetSearch
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
 * - Pinned indicator icon if pinned
 * - Tap to open chat with the contact
 */
@Composable
private fun ContactListItem(
    contact: ContactModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .liquidGlassCard()
            .clickable(onClick = onClick)
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

        // ── Name + Zixo Number ─────────────────────
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = contact.contactDisplayName,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = contact.formattedZixoNumber,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Pinned Indicator ──────────────────────
        if (contact.isPinned) {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = "Pinned",
                tint = EmeraldGreen,
                modifier = Modifier.size(18.dp)
            )
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

            Button(
                onClick = onFindContacts,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMint,
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find Contacts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
