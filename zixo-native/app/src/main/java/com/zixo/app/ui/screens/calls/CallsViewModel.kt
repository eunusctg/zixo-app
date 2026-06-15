package com.zixo.app.ui.screens.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.CallFilter
import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.usecase.InitiateCallUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CallsUiState(
    val calls: List<CallLogEntry> = emptyList(),
    val selectedFilter: CallFilter = CallFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentUserId: String? = null,
    val activeCallState: CallState? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Calls tab screen — displays call history with filters
 * and supports call initiation through [InitiateCallUseCase].
 *
 * Uses Clean Architecture Use Cases instead of direct repository access
 * for decoupled testability and zero-trust contact verification.
 */
@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val initiateCallUseCase: InitiateCallUseCase,
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
            try {
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
                        currentUserId = firebaseAuth.currentUser?.uid,
                        activeCallState = _uiState.value.activeCallState,
                        errorMessage = _uiState.value.errorMessage
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                Timber.e(e, "CallsViewModel: Failed to observe calls")
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

    /**
     * Initiates an audio call to a mutual contact via [InitiateCallUseCase].
     * The use case enforces zero-trust contact verification before signaling.
     *
     * @param targetUserId The UID of the contact to call.
     */
    fun initiateAudioCall(targetUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeCallState = CallState.DIALING(targetUid = targetUserId, isVideoCall = false)) }
            try {
                initiateCallUseCase.invokeWithVerification(targetUserId, isVideoCall = false)
                    .onSuccess { callState ->
                        _uiState.update { it.copy(activeCallState = callState) }
                        Timber.d("CallsViewModel: Audio call initiated to %s", targetUserId)
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                activeCallState = null,
                                errorMessage = error.localizedMessage ?: "Call failed"
                            )
                        }
                        Timber.w(error, "CallsViewModel: Audio call failed to %s", targetUserId)
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        activeCallState = null,
                        errorMessage = e.localizedMessage ?: "Call initiation error"
                    )
                }
                Timber.e(e, "CallsViewModel: Audio call error")
            }
        }
    }

    /**
     * Initiates a video call to a mutual contact via [InitiateCallUseCase].
     *
     * @param targetUserId The UID of the contact to call.
     */
    fun initiateVideoCall(targetUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeCallState = CallState.DIALING(targetUid = targetUserId, isVideoCall = true)) }
            try {
                initiateCallUseCase.invokeWithVerification(targetUserId, isVideoCall = true)
                    .onSuccess { callState ->
                        _uiState.update { it.copy(activeCallState = callState) }
                        Timber.d("CallsViewModel: Video call initiated to %s", targetUserId)
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                activeCallState = null,
                                errorMessage = error.localizedMessage ?: "Call failed"
                            )
                        }
                        Timber.w(error, "CallsViewModel: Video call failed to %s", targetUserId)
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        activeCallState = null,
                        errorMessage = e.localizedMessage ?: "Call initiation error"
                    )
                }
                Timber.e(e, "CallsViewModel: Video call error")
            }
        }
    }

    /**
     * Accepts an incoming call via the [InitiateCallUseCase].
     *
     * @param callId The ID of the incoming call.
     * @param callerId The UID of the caller.
     */
    fun acceptCall(callId: String, callerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                initiateCallUseCase.acceptCall(callId, callerId).collect { state ->
                    _uiState.update { it.copy(activeCallState = state) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Accept call failed")
                }
                Timber.e(e, "CallsViewModel: Accept call failed")
            }
        }
    }

    /**
     * Declines an incoming call.
     *
     * @param callId The ID of the call to decline.
     */
    fun declineCall(callId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                initiateCallUseCase.declineCall(callId)
                _uiState.update { it.copy(activeCallState = null) }
            } catch (e: Exception) {
                Timber.e(e, "CallsViewModel: Decline call failed")
            }
        }
    }

    /**
     * Ends the active call and releases WebRTC resources.
     *
     * @param callId The ID of the call to end.
     */
    fun endCall(callId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                initiateCallUseCase.endCall(callId)
                _uiState.update { it.copy(activeCallState = null) }
            } catch (e: Exception) {
                Timber.e(e, "CallsViewModel: End call failed")
            }
        }
    }

    /** Clears the error message after it has been displayed. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
