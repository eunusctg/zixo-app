package com.zexo.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.BuildConfig
import com.zexo.app.data.model.CallRecord
import com.zexo.app.data.model.Chat
import com.zexo.app.data.model.Status
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.CallRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.data.repository.StatusRepository
import com.zexo.app.data.repository.UserRepository
import com.zexo.app.ui.navigation.HomeTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val statusRepository: StatusRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // ── Current user ──────────────────────────────────────────────────
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ── Tabs ──────────────────────────────────────────────────────────
    private val _selectedTab = MutableStateFlow(HomeTab.CHATS)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    // ── Chats ─────────────────────────────────────────────────────────
    private val _allChats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _allChats.asStateFlow()

    // ── Call history ──────────────────────────────────────────────────
    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    // ── Statuses ──────────────────────────────────────────────────────
    private val _statuses = MutableStateFlow<List<Status>>(emptyList())
    val statuses: StateFlow<List<Status>> = _statuses.asStateFlow()

    // ── Online presence ───────────────────────────────────────────────
    private val _presenceMap = MutableStateFlow<Map<String, Pair<Boolean, Long>>>(emptyMap())
    val presenceMap: StateFlow<Map<String, Pair<Boolean, Long>>> = _presenceMap.asStateFlow()

    // ── User profiles for chat participants ───────────────────────────
    private val _userProfiles = MutableStateFlow<Map<String, User>>(emptyMap())
    val userProfiles: StateFlow<Map<String, User>> = _userProfiles.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ── Is admin ──────────────────────────────────────────────────────
    val isAdmin: Boolean get() = BuildConfig.IS_ADMIN

    // ── Loading state ─────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Filtered chats based on search ────────────────────────────────
    val filteredChats: StateFlow<List<Chat>> = combine(
        _allChats, _searchQuery, _userProfiles
    ) { chats, query, profiles ->
        if (query.isBlank()) chats
        else chats.filter { chat ->
            val otherUid = chat.participants.firstOrNull { it != authRepository.currentUid }
            val otherUser = otherUid?.let { profiles[it] }
            val name = if (chat.isGroup) chat.groupName else otherUser?.displayName ?: ""
            val lastMsg = chat.lastMessage
            name.contains(query, ignoreCase = true) || lastMsg.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Grouped statuses by user ──────────────────────────────────────
    val groupedStatuses: StateFlow<Map<String, List<Status>>> = _statuses.map { list ->
        list.groupBy { it.userId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadCurrentUser()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Public actions
    // ═══════════════════════════════════════════════════════════════════

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchUsers(query)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Internal
    // ═══════════════════════════════════════════════════════════════════

    private fun loadCurrentUser() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            authRepository.getUserProfile(uid)
                .onSuccess { user ->
                    _currentUser.value = user
                    observeChats(uid)
                    observeCallHistory(uid)
                    observeStatuses(uid)
                }
                .onFailure {
                    Log.e(TAG, "Failed to load current user", it)
                    _isLoading.value = false
                }
        }
    }

    private fun observeChats(uid: String) {
        viewModelScope.launch {
            chatRepository.observeChats(uid).collect { chatList ->
                _allChats.value = chatList
                _isLoading.value = false
                // Resolve participant profiles
                resolveUserProfiles(chatList)
                // Track online presence for participants
                trackPresence(chatList)
            }
        }
    }

    private fun observeCallHistory(uid: String) {
        viewModelScope.launch {
            // Real-time Firestore listener for call history
            callRepository.observeCallHistory(uid).collect { calls ->
                _callHistory.value = calls
            }
        }
    }

    private fun observeStatuses(uid: String) {
        viewModelScope.launch {
            // First load contacts (participants from chats)
            val contactUids = _allChats.value.flatMap { it.participants }.distinct()
                .filter { it != uid }

            // Observe statuses with current contact list; re-observe when contacts change
            _allChats.collect { chats ->
                val uids = chats.flatMap { it.participants }.distinct()
                    .filter { it != uid } + uid // include self for "My Status"
                statusRepository.observeStatuses(uids).collect { statuses ->
                    _statuses.value = statuses
                }
            }
        }
    }

    private fun resolveUserProfiles(chats: List<Chat>) {
        val uid = authRepository.currentUid ?: return
        val otherUids = chats.flatMap { chat ->
            chat.participants.filter { it != uid }
        }.distinct().filter { it !in _userProfiles.value }

        otherUids.forEach { otherUid ->
            if (_userProfiles.value[otherUid] == null) {
                viewModelScope.launch {
                    userRepository.getUserByUid(otherUid)
                        .onSuccess { user ->
                            _userProfiles.update { it + (otherUid to user) }
                        }
                }
            }
        }
    }

    private fun trackPresence(chats: List<Chat>) {
        val uid = authRepository.currentUid ?: return
        val otherUids = chats.flatMap { it.participants }.filter { it != uid }.distinct()
        if (otherUids.isNotEmpty()) {
            userRepository.observePresence(otherUids) { map ->
                _presenceMap.value = map
            }
        }
    }

    private fun searchUsers(query: String) {
        val uid = authRepository.currentUid ?: return
        _isSearching.value = true
        viewModelScope.launch {
            userRepository.searchUsers(query, uid)
                .onSuccess { _searchResults.value = it }
                .onFailure { Log.w(TAG, "Search failed", it) }
            _isSearching.value = false
        }
    }

    fun getOtherUser(chat: Chat): User? {
        val uid = authRepository.currentUid ?: return null
        val otherUid = chat.participants.firstOrNull { it != uid } ?: return null
        return _userProfiles.value[otherUid]
    }

    fun getUnreadCount(chat: Chat): Int {
        val uid = authRepository.currentUid ?: return 0
        return chat.unreadCount[uid] ?: 0
    }

    fun isUserOnline(uid: String): Boolean {
        return _presenceMap.value[uid]?.first == true
    }

    fun getLastSeenText(uid: String): String {
        val (online, lastSeen) = _presenceMap.value[uid] ?: return ""
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

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "HomeViewModel cleared")
    }
}
