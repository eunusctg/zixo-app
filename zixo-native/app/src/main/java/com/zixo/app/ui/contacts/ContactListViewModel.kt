package com.zixo.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.AddContactState
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.ContactSearchResult
import com.zixo.app.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ════════════════════════════════════════════════════════════════
// Contact List ViewModel
// ════════════════════════════════════════════════════════════════

/**
 * ViewModel for [ContactListScreen] and [FindContactDialog].
 *
 * Manages all contact-related state:
 * - Real-time contact list from Firestore via [ContactRepository.observeContactsRealtime]
 * - Zero-trust Zixo Number search with debounce
 * - Add / remove / block contact operations
 *
 * All repository operations run on [Dispatchers.IO] to keep the
 * main thread free. State is exposed as cold [StateFlow] streams
 * collected by the UI through `collectAsStateWithLifecycle()`.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    // ── Contact List ──────────────────────────────────────────────

    private val _contacts = MutableStateFlow<List<ContactModel>>(emptyList())
    val contacts: StateFlow<List<ContactModel>> = _contacts.asStateFlow()

    // ── Search State ──────────────────────────────────────────────

    private val _searchResult = MutableStateFlow<ContactSearchResult>(ContactSearchResult.Idle)
    val searchResult: StateFlow<ContactSearchResult> = _searchResult.asStateFlow()

    /** Raw input from the 8-digit field — debounced before triggering a search. */
    private val _searchQuery = MutableStateFlow("")

    // ── Add Contact State ─────────────────────────────────────────

    private val _addContactState = MutableStateFlow<AddContactState>(AddContactState.Idle)
    val addContactState: StateFlow<AddContactState> = _addContactState.asStateFlow()

    // ── Pull-to-Refresh ───────────────────────────────────────────

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── Operation Feedback ────────────────────────────────────────

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // ──────────────────────────────────────────────────────────────
    // Init — observe contacts in real-time + debounce search
    // ──────────────────────────────────────────────────────────────

    init {
        // Real-time contact list from Firestore
        viewModelScope.launch {
            try {
                contactRepository.observeContactsRealtime()
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5_000),
                        initialValue = emptyList()
                    )
                    .collect { contactList ->
                        _contacts.value = contactList
                    }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Failed to observe contacts")
            }
        }

        // Debounced search: only fire after 400ms of inactivity
        viewModelScope.launch {
            try {
                _searchQuery
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .distinctUntilChanged()
                    .collect { query ->
                        if (query.length == 8 && query.all { it.isDigit() }) {
                            performSearch(query)
                        } else if (query.isNotEmpty() && query.length < 8) {
                            // Don't overwrite an already-active searching state
                            if (_searchResult.value !is ContactSearchResult.Searching) {
                                _searchResult.value = ContactSearchResult.Idle
                            }
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Debounced search observation failed")
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Public Methods
    // ──────────────────────────────────────────────────────────────

    /**
     * Updates the search query. Triggers a debounced search when
     * exactly 8 digits are entered.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Searches for a user by their exact 8-digit Zixo Number.
     * Called directly (non-debounced) for explicit submit actions.
     */
    fun searchByZixoNumber(number: String) {
        if (number.length != 8 || !number.all { it.isDigit() }) {
            _searchResult.value = ContactSearchResult.InvalidFormat()
            return
        }
        performSearch(number)
    }

    /**
     * Adds a contact by their UID after a successful search.
     */
    fun addContact(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _addContactState.value = AddContactState.Adding
            try {
                contactRepository.addContact(uid).collect { state ->
                    _addContactState.value = state
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Failed to add contact")
                _addContactState.value = AddContactState.Error(
                    e.message ?: "Failed to add contact"
                )
            } finally {
                // Ensure Adding state is cleared if collect ends without emitting
                if (_addContactState.value is AddContactState.Adding) {
                    _addContactState.value = AddContactState.Idle
                }
            }
        }
    }

    /**
     * Removes a contact. Breaks the mutual relationship if it existed.
     */
    fun removeContact(contactUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contactRepository.removeContact(contactUid).collect { result ->
                    result.onSuccess {
                        _snackbarMessage.value = "Contact removed"
                    }.onFailure { error ->
                        _snackbarMessage.value = error.message ?: "Failed to remove contact"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Failed to remove contact")
                _snackbarMessage.value = e.message ?: "Failed to remove contact"
            }
        }
    }

    /**
     * Blocks a contact. Blocked contacts cannot send messages, call,
     * or view the user's status updates.
     */
    fun blockContact(contactUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contactRepository.blockContact(contactUid).collect { result ->
                    result.onSuccess {
                        _snackbarMessage.value = "Contact blocked"
                    }.onFailure { error ->
                        _snackbarMessage.value = error.message ?: "Failed to block contact"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Failed to block contact")
                _snackbarMessage.value = e.message ?: "Failed to block contact"
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
        _searchQuery.value = ""
    }

    /**
     * Refreshes the contact list from the remote source.
     */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                contactRepository.observeContactsRealtime().collect { contactList ->
                    _contacts.value = contactList
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Refresh failed")
                // Swallow — the real-time listener will recover
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Acknowledge and clear the snackbar message.
     */
    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // ──────────────────────────────────────────────────────────────
    // Private Helpers
    // ──────────────────────────────────────────────────────────────

    private fun performSearch(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchResult.value = ContactSearchResult.Searching
            try {
                contactRepository.searchByZixoNumber(number).collect { result ->
                    _searchResult.value = result
                }
            } catch (e: Exception) {
                Timber.e(e, "ContactListViewModel: Search failed for number %s", number)
                _searchResult.value = ContactSearchResult.Error(
                    e.message ?: "Search failed"
                )
            } finally {
                // Ensure Searching state is cleared if collect ends without emitting
                if (_searchResult.value is ContactSearchResult.Searching) {
                    _searchResult.value = ContactSearchResult.Idle
                }
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
