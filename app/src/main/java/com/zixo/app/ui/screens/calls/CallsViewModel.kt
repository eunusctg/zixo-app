package com.zixo.app.ui.screens.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.model.CallRecord
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.CallRepository
import com.zixo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallsUiState(
    val callHistory: List<CallRecord> = emptyList(),
    val selectedFilter: String = "all",
    val userProfiles: Map<String, ZixoUser> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallsUiState())
    val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<ZixoUser?> = authRepository.currentUser

    init {
        observeCalls()
    }

    private fun observeCalls() {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    callRepository.observeCallHistory(user.uid).collect { calls ->
                        val uniqueCalls = calls.distinctBy { it.id }
                        _uiState.value = _uiState.value.copy(callHistory = uniqueCalls, isLoading = false)

                        uniqueCalls.forEach { call ->
                            val otherId = if (call.callerId == user.uid) call.receiverId else call.callerId
                            if (otherId.isNotEmpty() && otherId !in _uiState.value.userProfiles) {
                                userRepository.getUserProfile(otherId).getOrNull()?.let { profile ->
                                    val updated = _uiState.value.userProfiles.toMutableMap()
                                    updated[otherId] = profile
                                    _uiState.value = _uiState.value.copy(userProfiles = updated)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredCalls(): List<CallRecord> {
        val filter = _uiState.value.selectedFilter
        val calls = _uiState.value.callHistory
        return when (filter) {
            "missed" -> calls.filter { it.isMissed() }
            "outgoing" -> calls.filter { it.isOutgoing() }
            "incoming" -> calls.filter { it.isIncoming() }
            else -> calls
        }
    }
}
