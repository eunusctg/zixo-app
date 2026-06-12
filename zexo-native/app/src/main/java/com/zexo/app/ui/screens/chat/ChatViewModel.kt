package com.zexo.app.ui.screens.chat

import android.util.Log
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val otherUser: User? = null,
    val isOtherUserOnline: Boolean = false,
    val otherUserLastSeen: Long = 0L,
    val isOtherUserTyping: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val chatId: String = "",
    val otherUserId: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val currentUid: String? get() = authRepository.currentUid
    private var typingJob: Job? = null
    private var initialized = false

    fun initChat(chatId: String, otherUserId: String) {
        if (initialized) return
        initialized = true

        _uiState.update { it.copy(chatId = chatId, otherUserId = otherUserId) }

        if (otherUserId.isNotBlank()) {
            // otherUserId provided directly
            loadOtherUserProfile(otherUserId)
            observeMessages(chatId)
            observeTyping(chatId)
            observePresence(otherUserId)
            markChatRead(chatId)
        } else {
            // otherUserId not provided — try to resolve it from chat participants
            resolveOtherUserAndInit(chatId)
        }
    }

    private fun resolveOtherUserAndInit(chatId: String) {
        val myUid = currentUid ?: run {
            Log.e(TAG, "No current user UID, cannot resolve other user")
            _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Try to get the chat document to find participants
            try {
                val chatDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("chats").document(chatId).get().await()

                if (chatDoc.exists()) {
                    val participants = chatDoc.get("participants") as? List<String> ?: emptyList()
                    val otherUid = participants.firstOrNull { it != myUid } ?: ""

                    if (otherUid.isNotBlank()) {
                        _uiState.update { it.copy(otherUserId = otherUid) }
                        loadOtherUserProfile(otherUid)
                        observePresence(otherUid)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve other user from chat doc", e)
            }

            // Always observe messages and typing regardless
            observeMessages(chatId)
            observeTyping(chatId)
            markChatRead(chatId)
        }
    }

    private fun loadOtherUserProfile(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUserByUid(uid).onSuccess { user ->
                _uiState.update { it.copy(otherUser = user) }
            }.onFailure {
                Log.e(TAG, "Failed to load other user profile", it)
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
