package com.zixo.app.ui.status

import android.util.Log
import com.zixo.app.data.repository.StatusRepositoryImpl
import com.zixo.app.domain.model.StatusUpdate
import com.zixo.app.domain.model.UserStatus
import com.zixo.app.domain.model.SymbolCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatusViewModel(
    private val statusRepository: StatusRepositoryImpl
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val TAG = "StatusViewModel"

    val userStatuses: StateFlow<List<UserStatus>> = statusRepository.userStatuses
    val myStatuses: StateFlow<List<StatusUpdate>> = statusRepository.myStatuses

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedStatus = MutableStateFlow<StatusUpdate?>(null)
    val selectedStatus: StateFlow<StatusUpdate?> = _selectedStatus.asStateFlow()

    private val _showViewer = MutableStateFlow(false)
    val showViewer: StateFlow<Boolean> = _showViewer.asStateFlow()

    private val _showCreator = MutableStateFlow(false)
    val showCreator: StateFlow<Boolean> = _showCreator.asStateFlow()

    val symbolCategories = SymbolCatalog.categories
    val symbolsByCategory = SymbolCatalog::byCategory

    fun startListening() = statusRepository.startListening()
    fun stopListening() = statusRepository.stopListening()

    fun createTextStatus(text: String, backgroundColor: String) {
        scope.launch {
            try {
                _isCreating.value = true
                statusRepository.createTextStatus(text, backgroundColor)
                _showCreator.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create status", e)
                _error.value = "Failed to create status"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun createImageStatus(imageBytes: ByteArray, caption: String) {
        scope.launch {
            try {
                _isCreating.value = true
                statusRepository.createImageStatus(imageBytes, caption)
                _showCreator.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create image status", e)
                _error.value = "Failed to create image status"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun viewStatus(status: StatusUpdate) {
        _selectedStatus.value = status
        _showViewer.value = true
        scope.launch {
            statusRepository.markStatusViewed(status.id)
        }
    }

    fun dismissViewer() {
        _showViewer.value = false
        _selectedStatus.value = null
    }

    fun addReaction(emoji: String) {
        val status = _selectedStatus.value ?: return
        scope.launch {
            statusRepository.addStatusReaction(status.id, emoji)
        }
    }

    fun showCreator() {
        _showCreator.value = true
    }

    fun dismissCreator() {
        _showCreator.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
