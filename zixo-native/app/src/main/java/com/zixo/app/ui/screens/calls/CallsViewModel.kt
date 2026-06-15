package com.zixo.app.ui.screens.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.CallRepository
import com.zixo.app.domain.model.CallFilter
import com.zixo.app.domain.model.CallLogEntry
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallsUiState(
    val calls: List<CallLogEntry> = emptyList(),
    val selectedFilter: CallFilter = CallFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentUserId: String? = null
)

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallsUiState())
    val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)

    private val allCallsFlow = callRepository.getAllCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Filtered calls exposed for the CallsScreen. */
    val filteredCalls: StateFlow<List<CallLogEntry>> = combine(
        allCallsFlow,
        _selectedFilter
    ) { calls, filter ->
        when (filter) {
            CallFilter.ALL -> calls
            CallFilter.MISSED -> calls.filter { it.type.name == "MISSED" }
            CallFilter.OUTGOING -> calls.filter { it.type.name == "OUTGOING" }
            CallFilter.INCOMING -> calls.filter { it.type.name == "INCOMING" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        _uiState.update { it.copy(currentUserId = firebaseAuth.currentUser?.uid) }

        viewModelScope.launch {
            combine(
                allCallsFlow,
                _selectedFilter
            ) { calls, filter ->
                val filtered = when (filter) {
                    CallFilter.ALL -> calls
                    CallFilter.MISSED -> calls.filter { it.type.name == "MISSED" }
                    CallFilter.OUTGOING -> calls.filter { it.type.name == "OUTGOING" }
                    CallFilter.INCOMING -> calls.filter { it.type.name == "INCOMING" }
                }
                CallsUiState(
                    calls = filtered,
                    selectedFilter = filter,
                    isLoading = false,
                    isRefreshing = false,
                    currentUserId = firebaseAuth.currentUser?.uid
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Alias used by CallsScreen segmented picker. */
    fun onFilterSelected(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    /** Alias for backward compatibility. */
    fun setFilter(filter: CallFilter) {
        onFilterSelected(filter)
    }

    /** Pull-to-refresh handler. */
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // The allCallsFlow is a continuous snapshot listener, so a refresh
            // essentially just resets the refreshing flag after a brief delay.
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearCallHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                callRepository.clearCallHistory()
            } catch (_: Exception) {
                // Silently handle; UI will still show whatever is in the local DB
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
