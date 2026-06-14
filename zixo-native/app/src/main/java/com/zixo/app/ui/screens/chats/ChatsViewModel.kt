package com.zixo.app.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.ChatRepository
import com.zixo.app.data.repository.UserRepository
import com.zixo.app.domain.model.ChatThread
import com.zixo.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatsUiState(
    val threads: List<ChatThread> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val currentUser: User? = null
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val threadsFlow = chatRepository.getChatThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val currentUserFlow = userRepository.getCurrentUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            combine(
                threadsFlow,
                _searchQuery,
                currentUserFlow
            ) { threads, query, user ->
                val filtered = if (query.isBlank()) {
                    threads
                } else {
                    chatRepository.searchThreads(query).first()
                }
                ChatsUiState(
                    threads = filtered,
                    searchQuery = query,
                    isLoading = false,
                    currentUser = user
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        refresh()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                chatRepository.syncThreadsFromRemote()
            } catch (_: Exception) {
                // Local data remains available; silently handle sync failure
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
