package com.zexo.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUid: String? get() = authRepository.currentUid

    fun loadProfile(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUserByUid(uid).onSuccess { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

        // Observe presence
        userRepository.observePresence(listOf(uid)) { statuses ->
            statuses[uid]?.let { (online, lastSeen) ->
                _uiState.update { it.copy(isOnline = online, lastSeen = lastSeen) }
            }
        }
    }

    fun startChat(otherUserId: String, onChatReady: (String) -> Unit) {
        val uid = currentUid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.createOrGetChat(uid, otherUserId).onSuccess { chatId ->
                onChatReady(chatId)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
