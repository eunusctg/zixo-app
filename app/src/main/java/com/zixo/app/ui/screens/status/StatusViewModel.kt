package com.zixo.app.ui.screens.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.model.StatusUpdate
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.StatusRepository
import com.zixo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusUiState(
    val myStatuses: List<StatusUpdate> = emptyList(),
    val contactStatuses: List<StatusUpdate> = emptyList(),
    val selectedStatus: StatusUpdate? = null,
    val isViewerOpen: Boolean = false,
    val reactionEmoji: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<ZixoUser?> = authRepository.currentUser

    init {
        observeStatuses()
    }

    private fun observeStatuses() {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    launch {
                        try {
                            val allUsers = userRepository.getAllUsers(user.uid).getOrDefault(emptyList())
                            val zixoNumbers = allUsers.map { it.zixoNumber }.filter { it.isNotEmpty() }
                            statusRepository.observeStatuses(zixoNumbers).collect { statuses ->
                                val myStatuses = statuses.filter { it.creatorZixoNumber == user.zixoNumber }
                                val contactStatuses = statuses.filter { it.creatorZixoNumber != user.zixoNumber }
                                _uiState.value = _uiState.value.copy(
                                    myStatuses = myStatuses,
                                    contactStatuses = contactStatuses,
                                    isLoading = false
                                )
                            }
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                        }
                    }
                }
            }
        }
    }

    fun openStatusViewer(status: StatusUpdate) {
        _uiState.value = _uiState.value.copy(selectedStatus = status, isViewerOpen = true)
        viewModelScope.launch {
            val myNumber = currentUser.value?.zixoNumber ?: return@launch
            statusRepository.markStatusViewed(status.statusId, myNumber)
        }
    }

    fun closeStatusViewer() {
        _uiState.value = _uiState.value.copy(isViewerOpen = false, selectedStatus = null, reactionEmoji = "")
    }

    fun reactToStatus(emoji: String) {
        val statusId = _uiState.value.selectedStatus?.statusId ?: return
        viewModelScope.launch {
            statusRepository.addReaction(statusId, emoji)
            _uiState.value = _uiState.value.copy(reactionEmoji = emoji)
        }
    }

    fun createStatus(text: String, backgroundColor: String, symbols: List<String>) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val status = StatusUpdate(
                creatorZixoNumber = user.zixoNumber,
                creatorDisplayName = user.displayName,
                creatorAvatarUrl = user.avatar,
                textContent = text,
                backgroundColorHex = backgroundColor,
                overlaySymbols = symbols,
                timestamp = System.currentTimeMillis()
            )
            statusRepository.createStatus(status)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            statusRepository.deleteStatus(statusId)
        }
    }
}
