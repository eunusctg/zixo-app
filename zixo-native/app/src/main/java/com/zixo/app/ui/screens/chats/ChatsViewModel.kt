package com.zixo.app.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Chats tab screen — displays conversation thread list.
 *
 * Attaches continuous Firestore snapshot listeners to observe all threads
 * the authenticated user is part of. Changes to thread metadata (new messages,
 * unread counts, pinned state) appear instantly across devices via active
 * Kotlin StateFlow pipes.
 */
@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _threads = MutableStateFlow<List<ChatThreadModel>>(emptyList())
    val threads: StateFlow<List<ChatThreadModel>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allThreads: List<ChatThreadModel> = emptyList()

    init {
        observeThreadsRealtime()
    }

    /**
     * Attaches a continuous Firestore snapshot listener to the threads collection.
     * Updates flow automatically whenever thread metadata changes.
     */
    private fun observeThreadsRealtime() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.observeThreadsRealtime()
                .catch { e ->
                    Timber.e(e, "Failed to observe threads")
                    _isLoading.value = false
                }
                .collect { threadList ->
                    allThreads = threadList.sortedWith(
                        compareByDescending<ChatThreadModel> { it.isPinned }
                            .thenByDescending { it.lastMessage?.timestamp ?: it.createdAt }
                    )
                    applyFilter()
                    _isLoading.value = false
                }
        }
    }

    /**
     * Filters the thread list based on the current search query.
     * Searches thread display names and last message content.
     */
    fun filterThreads(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        _threads.value = if (query.isBlank()) {
            allThreads
        } else {
            allThreads.filter { thread ->
                val name = getThreadDisplayName(thread).lowercase()
                val lastMsg = thread.lastMessage?.content?.lowercase() ?: ""
                name.contains(query) || lastMsg.contains(query)
            }
        }
    }

    private fun getThreadDisplayName(thread: ChatThreadModel): String {
        return when {
            !thread.groupName.isNullOrBlank() -> thread.groupName
            else -> thread.participantProfiles.values.firstOrNull()?.displayName ?: "Unknown"
        }
    }
}
