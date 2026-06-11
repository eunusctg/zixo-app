package com.zexo.app.ui.screens.contacts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewChatUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingAll: Boolean = true,
    val presenceMap: Map<String, Pair<Boolean, Long>> = emptyMap(),
    val createdChatId: String? = null,
    val isCreatingChat: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NewChatViewModel"
        private const val DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow(NewChatUiState())
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val currentUid: String? get() = authRepository.currentUid

    init {
        loadAllUsers()
        observeSearch()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Search
    // ═══════════════════════════════════════════════════════════════════

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        kotlinx.coroutines.flow.flowOf(emptyList())
                    } else {
                        _uiState.update { it.copy(isSearching = true) }
                        kotlinx.coroutines.flow.flowOf(runCatching {
                            val uid = currentUid ?: return@runCatching emptyList()
                            userRepository.searchUsers(query, uid).getOrDefault(emptyList())
                        }.getOrDefault(emptyList()))
                    }
                }
                .collect { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  All users
    // ═══════════════════════════════════════════════════════════════════

    private fun loadAllUsers() {
        val uid = currentUid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getAllUsers(uid)
                .onSuccess { users ->
                    _uiState.update { it.copy(allUsers = users, isLoadingAll = false) }
                    trackPresence(users.map { it.uid })
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load all users", e)
                    _uiState.update { it.copy(isLoadingAll = false, error = e.message) }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Presence tracking
    // ═══════════════════════════════════════════════════════════════════

    private fun trackPresence(uids: List<String>) {
        if (uids.isEmpty()) return
        userRepository.observePresence(uids) { map ->
            _uiState.update { it.copy(presenceMap = map) }
        }
    }

    fun isUserOnline(uid: String): Boolean {
        return _uiState.value.presenceMap[uid]?.first == true
    }

    fun getLastSeenText(uid: String): String {
        val (online, lastSeen) = _uiState.value.presenceMap[uid] ?: return ""
        if (online) return "online"
        if (lastSeen == 0L) return ""
        val diff = System.currentTimeMillis() - lastSeen
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Create / Get chat
    // ═══════════════════════════════════════════════════════════════════

    fun startChat(otherUserId: String) {
        val uid = currentUid ?: return
        _uiState.update { it.copy(isCreatingChat = true) }
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.createOrGetChat(uid, otherUserId)
                .onSuccess { chatId ->
                    _uiState.update { it.copy(isCreatingChat = false, createdChatId = chatId) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to create/get chat", e)
                    _uiState.update { it.copy(isCreatingChat = false, error = e.message) }
                }
        }
    }

    fun clearCreatedChatId() {
        _uiState.update { it.copy(createdChatId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Fetch a user by UID (used after QR scan).
     */
    fun fetchUserByUid(uid: String, onResult: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUserByUid(uid)
                .onSuccess { onResult(it) }
                .onFailure {
                    Log.e(TAG, "Failed to fetch user by UID from QR", it)
                    onResult(null)
                }
        }
    }
}
