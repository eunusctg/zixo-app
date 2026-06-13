package com.zixo.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.model.CallRecord
import com.zixo.app.data.model.Chat
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.CallRepository
import com.zixo.app.data.repository.ChatRepository
import com.zixo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val chats: List<Chat> = emptyList(),
    val callHistory: List<CallRecord> = emptyList(),
    val userProfiles: Map<String, ZixoUser> = emptyMap(),
    val searchQuery: String = "",
    val selectedCallFilter: String = "all",
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<ZixoUser?> = authRepository.currentUser

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    launch { observeChats(user.uid) }
                    launch { observeCalls(user.uid) }
                }
            }
        }
    }

    private suspend fun observeChats(userId: String) {
        chatRepository.observeChats(userId).collect { chats ->
            _uiState.value = _uiState.value.copy(chats = chats, isLoading = false)
            chats.forEach { chat ->
                val otherId = chat.getOtherUserId(userId)
                if (otherId.isNotEmpty() && otherId !in _uiState.value.userProfiles) {
                    try {
                        userRepository.getUserProfile(otherId).getOrNull()?.let { profile ->
                            val updated = _uiState.value.userProfiles.toMutableMap()
                            updated[otherId] = profile
                            _uiState.value = _uiState.value.copy(userProfiles = updated)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load profile for $otherId: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun observeCalls(userId: String) {
        callRepository.observeCallHistory(userId).collect { calls ->
            _uiState.value = _uiState.value.copy(callHistory = calls)
            // Load user profiles for call participants
            calls.forEach { call ->
                val otherId = if (call.callerId == userId) call.receiverId else call.callerId
                if (otherId.isNotEmpty() && otherId !in _uiState.value.userProfiles) {
                    try {
                        userRepository.getUserProfile(otherId).getOrNull()?.let { profile ->
                            val updated = _uiState.value.userProfiles.toMutableMap()
                            updated[otherId] = profile
                            _uiState.value = _uiState.value.copy(userProfiles = updated)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load profile for call participant $otherId: ${e.message}")
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onCallFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(selectedCallFilter = filter)
    }

    fun getFilteredCalls(): List<CallRecord> {
        val filter = _uiState.value.selectedCallFilter
        val calls = _uiState.value.callHistory
        return when (filter) {
            "missed" -> calls.filter { it.isMissed() }
            "outgoing" -> calls.filter { it.isOutgoing() }
            "incoming" -> calls.filter { it.isIncoming() }
            else -> calls
        }
    }

    fun getFilteredChats(): List<Chat> {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return _uiState.value.chats
        return _uiState.value.chats.filter { chat ->
            val otherId = chat.getOtherUserId(currentUser.value?.uid ?: "")
            val profile = _uiState.value.userProfiles[otherId]
            profile?.displayName?.contains(query, ignoreCase = true) == true ||
            profile?.username?.contains(query, ignoreCase = true) == true
        }
    }
}
