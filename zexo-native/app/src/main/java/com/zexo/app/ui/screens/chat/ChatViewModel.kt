package com.zexo.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.model.Message
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val otherUser: User? = null,
    val isOtherUserOnline: Boolean = false,
    val otherUserLastSeen: Long = 0L,
    val isOtherUserTyping: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val currentUid: String? get() = authRepository.currentUid
    private var typingJob: Job? = null

    fun initChat(chatId: String, otherUserId: String) {
        loadOtherUserProfile(otherUserId)
        observeMessages(chatId)
        observeTyping(chatId)
        observePresence(otherUserId)
        markChatRead(chatId)
    }

    private fun loadOtherUserProfile(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUserByUid(uid).onSuccess { user ->
                _uiState.update { it.copy(otherUser = user) }
            }
        }
    }

    private fun observeMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { messages ->
                _uiState.update {
                    it.copy(messages = messages, isLoading = false)
                }
            }
        }
    }

    private fun observeTyping(chatId: String) {
        viewModelScope.launch {
            chatRepository.observeTyping(chatId).collect { typingUids ->
                val isOtherTyping = typingUids.any { uid -> uid != currentUid }
                _uiState.update { it.copy(isOtherUserTyping = isOtherTyping) }
            }
        }
    }

    private fun observePresence(otherUserId: String) {
        userRepository.observePresence(listOf(otherUserId)) { statuses ->
            statuses[otherUserId]?.let { (online, lastSeen) ->
                _uiState.update {
                    it.copy(isOtherUserOnline = online, otherUserLastSeen = lastSeen)
                }
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        val uid = currentUid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            // Stop typing indicator
            setTyping(chatId, false)
            chatRepository.sendMessage(chatId, uid, text.trim())
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = currentUid ?: return

        // Cancel any existing typing timeout
        typingJob?.cancel()

        if (isTyping) {
            viewModelScope.launch(Dispatchers.IO) {
                chatRepository.setTyping(chatId, uid, true)
            }
            // Auto-stop typing after 3 seconds of inactivity
            typingJob = viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                chatRepository.setTyping(chatId, uid, false)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                chatRepository.setTyping(chatId, uid, false)
            }
        }
    }

    private fun markChatRead(chatId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.markChatRead(chatId, uid)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
    }
}
