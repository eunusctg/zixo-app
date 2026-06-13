package com.zexo.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.model.Message
import com.zexo.app.data.model.MessageReaction
import com.zexo.app.data.model.ZixoUser
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val chatId: String = "",
    val otherUser: ZixoUser? = null,
    val messages: List<Message> = emptyList(),
    val messageText: String = "",
    val replyingTo: Message? = null,
    val showActionMenu: Boolean = false,
    val selectedMessage: Message? = null,
    val showDeleteConfirm: Boolean = false,
    val showForwardDialog: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<ZixoUser?> = authRepository.currentUser

    fun initChat(chatId: String, otherUserId: String) {
        if (_uiState.value.chatId == chatId && _uiState.value.otherUser != null) return
        _uiState.value = _uiState.value.copy(chatId = chatId, isLoading = true)

        viewModelScope.launch {
            userRepository.getUserProfile(otherUserId).getOrNull()?.let { user ->
                _uiState.value = _uiState.value.copy(otherUser = user)
            }
        }

        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
            }
        }

        viewModelScope.launch {
            chatRepository.markChatRead(chatId, authRepository.currentUser.value?.uid ?: "")
        }
    }

    fun onMessageTextChange(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return

        val chatId = _uiState.value.chatId
        val senderId = authRepository.firebaseUser?.uid ?: return
        val myZixoNumber = currentUser.value?.zixoNumber ?: ""
        val replyingTo = _uiState.value.replyingTo

        val message = Message(
            chatId = chatId,
            senderId = senderId,
            senderZixoNumber = myZixoNumber,
            text = text,
            type = "text",
            timestamp = System.currentTimeMillis(),
            status = "sending",
            replyTo = replyingTo?.id ?: "",
            replyToTextPreview = replyingTo?.text?.take(60),
            replyToSenderName = replyingTo?.senderZixoNumber
        )

        _uiState.value = _uiState.value.copy(messageText = "", replyingTo = null)

        viewModelScope.launch {
            chatRepository.sendMessage(chatId, message)
        }
    }

    fun onMessageLongPress(message: Message) {
        _uiState.value = _uiState.value.copy(
            selectedMessage = message,
            showActionMenu = true
        )
    }

    fun dismissActionMenu() {
        _uiState.value = _uiState.value.copy(showActionMenu = false, selectedMessage = null)
    }

    fun reactToMessage(emoji: String) {
        val message = _uiState.value.selectedMessage ?: return
        val myZixoNumber = currentUser.value?.zixoNumber ?: return
        viewModelScope.launch {
            chatRepository.addReaction(
                _uiState.value.chatId,
                message.id,
                MessageReaction(senderZixoNumber = myZixoNumber, reactionEmoji = emoji)
            )
        }
        dismissActionMenu()
    }

    fun replyToMessage() {
        val message = _uiState.value.selectedMessage ?: return
        _uiState.value = _uiState.value.copy(
            replyingTo = message,
            showActionMenu = false,
            selectedMessage = null
        )
    }

    fun cancelReply() {
        _uiState.value = _uiState.value.copy(replyingTo = null)
    }

    fun showDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true, showActionMenu = false)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, selectedMessage = null)
    }

    fun deleteForMe() {
        val message = _uiState.value.selectedMessage ?: return
        val myUid = authRepository.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.deleteMessageForMe(_uiState.value.chatId, message.id, myUid)
        }
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, selectedMessage = null)
    }

    fun deleteForEveryone() {
        val message = _uiState.value.selectedMessage ?: return
        val myUid = authRepository.firebaseUser?.uid ?: return
        if (message.senderId != myUid) return
        viewModelScope.launch {
            chatRepository.deleteMessageForEveryone(_uiState.value.chatId, message.id)
        }
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, selectedMessage = null)
    }

    fun showForwardDialog() {
        _uiState.value = _uiState.value.copy(showForwardDialog = true, showActionMenu = false)
    }

    fun dismissForwardDialog() {
        _uiState.value = _uiState.value.copy(showForwardDialog = false, selectedMessage = null)
    }

    fun forwardToChat(targetChatId: String) {
        val message = _uiState.value.selectedMessage ?: return
        val myZixoNumber = currentUser.value?.zixoNumber ?: return
        val myUid = authRepository.firebaseUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.forwardMessage(
                _uiState.value.chatId,
                targetChatId,
                message.id,
                myZixoNumber,
                myUid
            )
        }
        _uiState.value = _uiState.value.copy(showForwardDialog = false, selectedMessage = null)
    }

    fun startNewChat(otherUserId: String, onChatCreated: (String) -> Unit) {
        val currentUserId = authRepository.firebaseUser?.uid ?: return
        viewModelScope.launch {
            val result = chatRepository.getOrCreateChat(currentUserId, otherUserId)
            result.getOrNull()?.let { chatId ->
                initChat(chatId, otherUserId)
                onChatCreated(chatId)
            }
        }
    }
}
