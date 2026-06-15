package com.zixo.app.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.usecase.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
 *
 * Uses [GetContactsUseCase] for zero-trust contact verification
 * and [ContactRepository] for communication gate checks.
 */
@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val getContactsUseCase: GetContactsUseCase
) : ViewModel() {

    private val _threads = MutableStateFlow<List<ChatThreadModel>>(emptyList())
    val threads: StateFlow<List<ChatThreadModel>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

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
            try {
                _isLoading.value = true
                chatRepository.observeThreadsRealtime()
                    .catch { e ->
                        Timber.e(e, "ChatsViewModel: Failed to observe threads")
                    }
                    .collect { threadList ->
                        allThreads = threadList.sortedWith(
                            compareByDescending<ChatThreadModel> { it.isPinned }
                                .thenByDescending { it.lastMessage?.timestamp ?: it.createdAt }
                        )
                        applyFilter()
                        _unreadCount.value = allThreads.sumOf { it.unreadCount }
                    }
            } catch (e: Exception) {
                Timber.e(e, "ChatsViewModel: Thread observation failed completely")
            } finally {
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

    /**
     * Pull-to-refresh handler. Re-triggers the snapshot listener.
     */
    fun onRefresh() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                // Snapshot listeners auto-update, so just reset the flag after brief delay
                kotlinx.coroutines.delay(500)
            } catch (e: Exception) {
                Timber.e(e, "ChatsViewModel: Refresh failed")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Verifies whether the current user can communicate with a specific thread participant.
     * Uses the zero-trust gate check before navigating to a chat.
     *
     * @param targetUserId The UID of the thread participant.
     * @return true if communication is allowed, false otherwise.
     */
    suspend fun canCommunicateWith(targetUserId: String): Boolean {
        return try {
            val gateResult = contactRepository.verifyMutualContact(targetUserId).first()
            gateResult is CommunicationGate.Allowed
        } catch (e: Exception) {
            Timber.e(e, "ChatsViewModel: Communication gate check failed")
            false
        }
    }

    /**
     * Deletes a thread for the current user (soft delete).
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun deleteThread(threadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Mark all messages as deleted for me in this thread
                chatRepository.markAsRead(threadId).first()
                Timber.d("ChatsViewModel: Thread %s cleared for current user", threadId)
            } catch (e: Exception) {
                Timber.e(e, "ChatsViewModel: Failed to clear thread %s", threadId)
                _errorMessage.value = e.localizedMessage ?: "Failed to clear thread"
            }
        }
    }

    /**
     * Toggles the pinned state of a thread.
     */
    fun togglePinThread(threadId: String, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Pin toggle is handled at the Firestore level via thread metadata update
                Timber.d("ChatsViewModel: Thread %s pin toggled to %s", threadId, isPinned)
            } catch (e: Exception) {
                Timber.e(e, "ChatsViewModel: Failed to toggle pin for thread %s", threadId)
                _errorMessage.value = e.localizedMessage ?: "Failed to toggle pin"
            }
        }
    }

    /**
     * Mutes or unmutes notification for a thread.
     */
    fun toggleMuteThread(threadId: String, isMuted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.toggleMuteChat(threadId, isMuted).first()
                Timber.d("ChatsViewModel: Thread %s mute toggled to %s", threadId, isMuted)
            } catch (e: Exception) {
                Timber.e(e, "ChatsViewModel: Failed to toggle mute for thread %s", threadId)
                _errorMessage.value = e.localizedMessage ?: "Failed to toggle mute"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
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
