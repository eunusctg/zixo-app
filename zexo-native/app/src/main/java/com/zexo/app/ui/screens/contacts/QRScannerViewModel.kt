package com.zexo.app.ui.screens.contacts

import android.util.Log
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

data class QRScannerUiState(
    val scannedUid: String? = null,
    val scannedUser: User? = null,
    val isFetchingUser: Boolean = false,
    val isCreatingChat: Boolean = false,
    val createdChatId: String? = null,
    val showAddContactDialog: Boolean = false,
    val error: String? = null,
    val isCameraReady: Boolean = false
)

@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "QRScannerViewModel"
        private const val ZIXO_QR_PREFIX = "zixo://user/"
    }

    private val _uiState = MutableStateFlow(QRScannerUiState())
    val uiState: StateFlow<QRScannerUiState> = _uiState.asStateFlow()

    private val currentUid: String? get() = authRepository.currentUid

    /**
     * Called when a QR code is detected. Extracts the Zixo user UID
     * from the QR content and fetches the user profile.
     */
    fun onQrCodeDetected(qrContent: String) {
        // Prevent duplicate processing
        if (_uiState.value.showAddContactDialog || _uiState.value.isFetchingUser) return

        val uid = extractUidFromQr(qrContent)
        if (uid == null) {
            _uiState.update { it.copy(error = "Invalid QR code format") }
            return
        }

        // Don't allow scanning own QR
        if (uid == currentUid) {
            _uiState.update { it.copy(error = "This is your own QR code!") }
            return
        }

        _uiState.update { it.copy(scannedUid = uid, isFetchingUser = true) }

        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUserByUid(uid)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            scannedUser = user,
                            isFetchingUser = false,
                            showAddContactDialog = true
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to fetch user from QR UID", e)
                    _uiState.update {
                        it.copy(
                            isFetchingUser = false,
                            error = "User not found"
                        )
                    }
                }
        }
    }

    /**
     * Start a chat with the scanned user.
     */
    fun startChatWithScannedUser() {
        val uid = currentUid ?: return
        val otherUid = _uiState.value.scannedUid ?: return

        _uiState.update { it.copy(isCreatingChat = true) }
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.createOrGetChat(uid, otherUid)
                .onSuccess { chatId ->
                    _uiState.update { it.copy(isCreatingChat = false, createdChatId = chatId) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to create chat from QR scan", e)
                    _uiState.update { it.copy(isCreatingChat = false, error = e.message) }
                }
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                showAddContactDialog = false,
                scannedUid = null,
                scannedUser = null
            )
        }
    }

    fun clearCreatedChatId() {
        _uiState.update { it.copy(createdChatId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setCameraReady(ready: Boolean) {
        _uiState.update { it.copy(isCameraReady = ready) }
    }

    /**
     * Extract UID from QR content.
     * Expected formats:
     *   - "zixo://user/{uid}"
     *   - Just the raw UID string
     */
    private fun extractUidFromQr(content: String): String? {
        return if (content.startsWith(ZIXO_QR_PREFIX)) {
            content.removePrefix(ZIXO_QR_PREFIX).takeIf { it.isNotBlank() }
        } else if (content.length in 20..36 && content.all { it.isLetterOrDigit() }) {
            // Looks like a Firebase UID (typically 28 chars)
            content
        } else {
            null
        }
    }
}
