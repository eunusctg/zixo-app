package com.zixo.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.MyStatusState
import com.zixo.app.domain.model.StatusContentType
import com.zixo.app.domain.model.StatusGroupModel
import com.zixo.app.domain.model.StatusModel
import com.zixo.app.domain.model.StatusReaction
import com.zixo.app.domain.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Status feature with 100% real-time Firebase sync.
 *
 * All status data flows through continuous Firebase snapshot listeners
 * managed by [StatusRepository]. The UI observes [StateFlow] streams
 * that reflect real-time changes instantly — no manual refresh required.
 *
 * Zero-trust privacy is enforced at the repository layer: only mutual
 * contacts can see, view, react to, or receive each other's statuses.
 * The ViewModel does not bypass or duplicate these checks.
 *
 * StatusRepository is at com.zixo.app.domain.repository.StatusRepository
 *
 * interface StatusRepository {
 *     fun observeStatusFeed(): Flow<List<StatusGroupModel>>
 *     fun observeMyStatuses(): Flow<MyStatusState>
 *     suspend fun postTextStatus(text: String, backgroundColor: String?)
 *     suspend fun postImageStatus(localFilePath: String, caption: String?)
 *     suspend fun postVideoStatus(localFilePath: String, caption: String?)
 *     suspend fun viewStatus(statusId: String)
 *     suspend fun addStatusReaction(statusId: String, emoji: String)
 *     suspend fun deleteStatus(statusId: String)
 * }
 */
@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository
) : ViewModel() {

    // ──────────────────────────────────────────────
    // Real-time Status Feed
    // ──────────────────────────────────────────────

    /**
     * Real-time status feed from all mutual contacts, grouped by sender.
     * Backed by Firestore snapshot listeners — updates propagate instantly.
     */
    val statusFeed: StateFlow<List<StatusGroupModel>> = statusRepository
        .observeStatusFeed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ──────────────────────────────────────────────
    // My Statuses
    // ──────────────────────────────────────────────

    /**
     * Current user's own status state including active statuses,
     * upload progress, and error messages.
     */
    val myStatuses: StateFlow<MyStatusState> = statusRepository
        .observeMyStatuses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyStatusState()
        )

    // ──────────────────────────────────────────────
    // Upload State
    // ──────────────────────────────────────────────

    /** Whether a status upload is currently in progress. */
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    /** Upload progress from 0.0 (started) to 1.0 (complete). */
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    // ──────────────────────────────────────────────
    // Error State
    // ──────────────────────────────────────────────

    /** Current error message to display in the UI, or null if no error. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ──────────────────────────────────────────────
    // Status Viewer State
    // ──────────────────────────────────────────────

    /** The status group currently being viewed in the full-screen viewer. */
    private val _viewingStatusGroup = MutableStateFlow<StatusGroupModel?>(null)
    val viewingStatusGroup: StateFlow<StatusGroupModel?> = _viewingStatusGroup.asStateFlow()

    /** The index of the current status within the group being viewed. */
    private val _currentViewingIndex = MutableStateFlow(0)
    val currentViewingIndex: StateFlow<Int> = _currentViewingIndex.asStateFlow()

    // ──────────────────────────────────────────────
    // Status Posting
    // ──────────────────────────────────────────────

    /**
     * Posts a text status update with an optional background color.
     *
     * @param text The text content of the status.
     * @paramBackgroundColor Optional hex color string (e.g. "#FF5722") for the background.
     */
    fun postTextStatus(text: String, backgroundColor: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.update { true }
            _uploadProgress.update { 0f }
            try {
                statusRepository.postTextStatus(text, backgroundColor)
                _uploadProgress.update { 1f }
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to post text status" }
            } finally {
                _isUploading.update { false }
                _uploadProgress.update { 0f }
            }
        }
    }

    /**
     * Posts an image status from a local file path with an optional caption.
     *
     * @param filePath The local file path of the image to upload.
     * @param caption Optional caption text for the image.
     */
    fun postImageStatus(filePath: String, caption: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.update { true }
            _uploadProgress.update { 0f }
            try {
                statusRepository.postImageStatus(filePath, caption)
                _uploadProgress.update { 1f }
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to post image status" }
            } finally {
                _isUploading.update { false }
                _uploadProgress.update { 0f }
            }
        }
    }

    /**
     * Posts a video status from a local file path with an optional caption.
     *
     * @param filePath The local file path of the video to upload.
     * @param caption Optional caption text for the video.
     */
    fun postVideoStatus(filePath: String, caption: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.update { true }
            _uploadProgress.update { 0f }
            try {
                statusRepository.postVideoStatus(filePath, caption)
                _uploadProgress.update { 1f }
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to post video status" }
            } finally {
                _isUploading.update { false }
                _uploadProgress.update { 0f }
            }
        }
    }

    // ──────────────────────────────────────────────
    // Status Interactions
    // ──────────────────────────────────────────────

    /**
     * Marks a status as viewed by the current user.
     * Only mutual contacts can view statuses — enforced at repository layer.
     *
     * @param statusId The ID of the status to mark as viewed.
     */
    fun viewStatus(statusId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusRepository.viewStatus(statusId)
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to mark status as viewed" }
            }
        }
    }

    /**
     * Adds an emoji reaction to a status.
     * Only mutual contacts can react — enforced at repository layer.
     *
     * @param statusId The ID of the status to react to.
     * @param emoji The emoji character to react with.
     */
    fun addReaction(statusId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusRepository.addStatusReaction(statusId, emoji)
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to add reaction" }
            }
        }
    }

    /**
     * Deletes a status owned by the current user.
     * Only the owner can delete their own status — enforced at repository layer.
     *
     * @param statusId The ID of the status to delete.
     */
    fun deleteStatus(statusId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusRepository.deleteStatus(statusId)
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to delete status" }
            }
        }
    }

    // ──────────────────────────────────────────────
    // Status Viewer Navigation
    // ──────────────────────────────────────────────

    /**
     * Enters the full-screen status viewer for the given group.
     * Sets the viewing group and resets the index to the first status.
     *
     * @param group The [StatusGroupModel] to start viewing.
     */
    fun startViewing(group: StatusGroupModel) {
        _viewingStatusGroup.update { group }
        _currentViewingIndex.update { 0 }
        // Mark the first status as viewed
        val firstStatus = group.statuses.firstOrNull()
        if (firstStatus != null) {
            viewStatus(firstStatus.id)
        }
    }

    /**
     * Advances to the next status in the current group.
     * If at the end of the group, exits the viewer.
     */
    fun nextStatus() {
        val group = _viewingStatusGroup.value ?: return
        val currentIndex = _currentViewingIndex.value
        val nextIndex = currentIndex + 1

        if (nextIndex < group.statuses.size) {
            _currentViewingIndex.update { nextIndex }
            val nextStatus = group.statuses[nextIndex]
            viewStatus(nextStatus.id)
        } else {
            stopViewing()
        }
    }

    /**
     * Goes back to the previous status in the current group.
     * If already at the first status, does nothing.
     */
    fun previousStatus() {
        val currentIndex = _currentViewingIndex.value
        if (currentIndex > 0) {
            _currentViewingIndex.update { currentIndex - 1 }
            val group = _viewingStatusGroup.value ?: return
            val prevStatus = group.statuses[currentIndex - 1]
            viewStatus(prevStatus.id)
        }
    }

    /**
     * Exits the full-screen status viewer and clears all viewing state.
     */
    fun stopViewing() {
        _viewingStatusGroup.update { null }
        _currentViewingIndex.update { 0 }
    }

    // ──────────────────────────────────────────────
    // Error Handling
    // ──────────────────────────────────────────────

    /**
     * Clears the current error message. Should be called after the UI
     * has displayed the error (e.g., Snackbar dismissed).
     */
    fun clearError() {
        _errorMessage.update { null }
    }
}
