package com.zixo.app.ui.chat

import android.util.Log
import com.zixo.app.data.repository.ChatRepositoryImpl
import com.zixo.app.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepositoryImpl
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val TAG = "ChatViewModel"

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _chatInputState = MutableStateFlow(ChatInputState())
    val chatInputState: StateFlow<ChatInputState> = _chatInputState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedMessage = MutableStateFlow<Message?>(null)
    val selectedMessage: StateFlow<Message?> = _selectedMessage.asStateFlow()

    private val _showActionSheet = MutableStateFlow(false)
    val showActionSheet: StateFlow<Boolean> = _showActionSheet.asStateFlow()

    private val _showReactionPanel = MutableStateFlow(false)
    val showReactionPanel: StateFlow<Boolean> = _showReactionPanel.asStateFlow()

    private var currentChatId: String = ""

    fun loadChat(chatId: String) {
        currentChatId = chatId
        chatRepository.startMessagesListening(chatId)
        scope.launch {
            chatRepository.getMessagesFlow(chatId).collect { messages ->
                _messages.value = messages
            }
        }
        scope.launch {
            chatRepository.markAsRead(chatId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentChatId.isBlank()) return
        val replyToId = _chatInputState.value.replyToMessage?.id ?: ""
        scope.launch {
            try {
                chatRepository.sendMessage(currentChatId, text.trim(), replyToId)
                _chatInputState.value = _chatInputState.value.copy(
                    text = "",
                    isReplying = false,
                    replyToMessage = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                _error.value = "Failed to send message"
            }
        }
    }

    fun sendImageMessage(imageBytes: ByteArray, caption: String = "") {
        if (currentChatId.isBlank()) return
        scope.launch {
            try {
                _isLoading.value = true
                chatRepository.sendImageMessage(currentChatId, imageBytes, caption)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send image", e)
                _error.value = "Failed to send image"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onMessageLongPress(message: Message) {
        _selectedMessage.value = message
        _showActionSheet.value = true
    }

    fun onReactionClick(emoji: String) {
        val message = _selectedMessage.value ?: return
        scope.launch {
            chatRepository.addReaction(currentChatId, message.id, emoji)
            _showReactionPanel.value = false
            _showActionSheet.value = false
            _selectedMessage.value = null
        }
    }

    fun onReply() {
        val message = _selectedMessage.value ?: return
        _chatInputState.value = _chatInputState.value.copy(
            isReplying = true,
            replyToMessage = message
        )
        _showActionSheet.value = false
        _selectedMessage.value = null
    }

    fun onCancelReply() {
        _chatInputState.value = _chatInputState.value.copy(
            isReplying = false,
            replyToMessage = null
        )
    }

    fun onForward() {
        // Forward handled by UI navigation
        _showActionSheet.value = false
        _selectedMessage.value = null
    }

    fun onDeleteForMe() {
        val message = _selectedMessage.value ?: return
        scope.launch {
            chatRepository.deleteMessageForMe(currentChatId, message.id)
            _showActionSheet.value = false
            _selectedMessage.value = null
        }
    }

    fun onDeleteForEveryone() {
        val message = _selectedMessage.value ?: return
        scope.launch {
            chatRepository.deleteMessageForEveryone(currentChatId, message.id)
            _showActionSheet.value = false
            _selectedMessage.value = null
        }
    }

    fun dismissActionSheet() {
        _showActionSheet.value = false
        _showReactionPanel.value = false
        _selectedMessage.value = null
    }

    fun showReactionPanel() {
        _showReactionPanel.value = true
    }

    fun clearError() {
        _error.value = null
    }

    fun isSender(message: Message, currentUserId: String): Boolean = message.senderId == currentUserId

    fun canDeleteForEveryone(message: Message, currentUserId: String): Boolean =
        message.senderId == currentUserId && !message.isDeletedForEveryone
}
