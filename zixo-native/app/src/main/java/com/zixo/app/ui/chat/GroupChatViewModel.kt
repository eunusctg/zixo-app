package com.zixo.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.Message
import com.zixo.app.domain.model.MessageType
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Dedicated ViewModel for group chat — extracted from GroupChatScreen.
 *
 * Handles real-time group matrices, dynamic membership changes,
 * member identification indexing, and group-specific operations
 * like promote/demote admin, remove member, and leave group.
 *
 * All operations on Dispatchers.IO with comprehensive error boundaries.
 */
@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    data class GroupMemberInfo(
        val uid: String,
        val displayName: String,
        val avatarUrl: String,
        val isAdmin: Boolean = false,
        val isOnline: Boolean = false,
        val joinedAt: Long = System.currentTimeMillis()
    )

    data class GroupChatUiState(
        val chatId: String = "",
        val messages: List<Message> = emptyList(),
        val groupName: String = "",
        val groupDescription: String = "",
        val groupAvatarUrl: String = "",
        val members: List<GroupMemberInfo> = emptyList(),
        val isAdmin: Boolean = false,
        val isEditing: Boolean = false,
        val isLoading: Boolean = true,
        val isMuted: Boolean = false,
        val error: String? = null,
        val memberCount: Int = 0
    )

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    init {
        val chatId = savedStateHandle.get<String>("chatId") ?: ""
        if (chatId.isNotEmpty()) {
            _uiState.update { it.copy(chatId = chatId) }
            loadGroupDetails(chatId)
            loadMembers(chatId)
            observeMessages(chatId)
        }
    }

    fun loadGroupDetails(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val chatThread = chatRepository.getChatThread(chatId)
                chatThread.fold(
                    onSuccess = { thread ->
                        _uiState.update { state ->
                            state.copy(
                                groupName = thread.name ?: "Group Chat",
                                groupDescription = thread.description ?: "",
                                groupAvatarUrl = thread.avatarUrl ?: "",
                                isMuted = thread.isMuted,
                                isLoading = false
                            )
                        }
                        Timber.d("GroupChatViewModel: Group details loaded — %s", thread.name)
                    },
                    onFailure = { error ->
                        Timber.e(error, "GroupChatViewModel: Failed to load group details")
                        _uiState.update { it.copy(isLoading = false, error = error.localizedMessage) }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Unhandled error loading group")
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun loadMembers(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val membersList = chatRepository.getGroupMembers(chatId).getOrElse { emptyList() }
                val memberInfos = membersList.map { member ->
                    GroupMemberInfo(
                        uid = member.uid,
                        displayName = member.displayName,
                        avatarUrl = member.avatarUrl,
                        isAdmin = member.role == "admin",
                        isOnline = member.isOnline,
                        joinedAt = member.joinedAt
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        members = memberInfos,
                        memberCount = memberInfos.size
                    )
                }
                Timber.d("GroupChatViewModel: Loaded %d members", memberInfos.size)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to load members")
            }
        }
    }

    private fun observeMessages(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.getMessages(chatId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Message observation failed")
            }
        }
    }

    fun sendMessage(content: String, type: MessageType = MessageType.TEXT) {
        val chatId = _uiState.value.chatId
        if (chatId.isEmpty() || content.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendMessageUseCase(chatId, content, type)
                _messageText.value = ""
                Timber.d("GroupChatViewModel: Message sent to group %s", chatId)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to send message")
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun updateGroupName(name: String) {
        val chatId = _uiState.value.chatId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.updateGroupName(chatId, name)
                _uiState.update { it.copy(groupName = name) }
                Timber.d("GroupChatViewModel: Group name updated to %s", name)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to update group name")
            }
        }
    }

    fun updateGroupDescription(description: String) {
        val chatId = _uiState.value.chatId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.updateGroupDescription(chatId, description)
                _uiState.update { it.copy(groupDescription = description) }
                Timber.d("GroupChatViewModel: Group description updated")
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to update description")
            }
        }
    }

    fun promoteToAdmin(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.updateMemberRole(_uiState.value.chatId, userId, "admin")
                loadMembers(_uiState.value.chatId)
                Timber.d("GroupChatViewModel: Promoted %s to admin", userId)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to promote member")
            }
        }
    }

    fun demoteFromAdmin(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.updateMemberRole(_uiState.value.chatId, userId, "member")
                loadMembers(_uiState.value.chatId)
                Timber.d("GroupChatViewModel: Demoted %s from admin", userId)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to demote member")
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.removeGroupMember(_uiState.value.chatId, userId)
                loadMembers(_uiState.value.chatId)
                Timber.d("GroupChatViewModel: Removed member %s", userId)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to remove member")
            }
        }
    }

    fun leaveGroup() {
        val chatId = _uiState.value.chatId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.leaveGroup(chatId)
                Timber.d("GroupChatViewModel: Left group %s", chatId)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to leave group")
            }
        }
    }

    fun toggleMute(isMuted: Boolean) {
        val chatId = _uiState.value.chatId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.toggleMuteChat(chatId, isMuted)
                _uiState.update { it.copy(isMuted = isMuted) }
                Timber.d("GroupChatViewModel: Mute toggled to %b", isMuted)
            } catch (e: Exception) {
                Timber.e(e, "GroupChatViewModel: Failed to toggle mute")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
