package com.zixo.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.MyStatusState
import com.zixo.app.domain.model.StatusContentType
import com.zixo.app.domain.model.StatusGroupModel
import com.zixo.app.domain.model.StatusModel
import com.zixo.app.domain.model.StatusPrivacyConfig
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.repository.SettingsRepository
import com.zixo.app.domain.repository.StatusRepository
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

/**
 * ViewModel for the Status feature with 100% real-time Firebase sync
 * and contact-gated delivery enforcement.
 *
 * All status data flows through continuous Firebase snapshot listeners
 * managed by [StatusRepository]. The UI observes [StateFlow] streams
 * that reflect real-time changes instantly — no manual refresh required.
 *
 * Zero-trust privacy is enforced at the repository layer: only mutual
 * contacts can see, view, react to, or receive each other's statuses.
 * The ViewModel does not bypass or duplicate these checks.
 *
 * [ContactRepository] provides the mutual contact whitelist used to
 * gate status delivery. [SettingsRepository] provides the current
 * status privacy configuration.
 */
@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ──────────────────────────────────────────────
    // Real-time Contact Statuses (contacts-only delivery)
    // ──────────────────────────────────────────────

    /**
     * Real-time status feed from all mutual contacts, grouped by sender.
     * Backed by Firestore snapshot listeners — updates propagate instantly.
     * Only statuses from verified mutual contacts are delivered.
     */
    val contactStatuses: StateFlow<List<StatusGroupModel>> = statusRepository
        .observeContactStatusesRealtime()
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
        .getMyStatuses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyStatusState()
        )

    // ──────────────────────────────────────────────
    // Mutual Contacts (privacy config)
    // ──────────────────────────────────────────────

    /**
     * Real-time list of mutual contacts, used for privacy configuration
     * (exclude/only-share-with lists) and contact-gated status delivery.
     * Only contacts where [ContactModel.isMutual] == true are included.
     */
    val mutualContacts: StateFlow<List<ContactModel>> = contactRepository
        .observeContactsRealtime()
        .combine(statusRepository.observeContactStatusesRealtime()) { contacts, _ ->
            // Filter to only mutual, non-blocked contacts
            contacts.filter { it.isMutual && !it.isBlocked }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ──────────────────────────────────────────────
    // Privacy Configuration
    // ──────────────────────────────────────────────

    /**
     * Current status privacy configuration derived from settings flow.
     * Determines who can see the user's statuses.
     */
    val privacyConfig: StateFlow<StatusPrivacyConfig> = settingsRepository
        .settingsFlow
        .combine(contactRepository.observeContactsRealtime()) { settings, contacts ->
            StatusPrivacyConfig(
                option = settings.statusPrivacy,
                excludedContactUids = emptySet(),   // Populated from settings when available
                onlyShareWithUids = emptySet()      // Populated from settings when available
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusPrivacyConfig()
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
    // Initialization — Continuous Real-time Listeners
    // ──────────────────────────────────────────────

    init {
        // Attach continuous Firebase listeners on initialization.
        // The StateFlow streams above are already backed by Firestore
        // snapshot listeners via the repository layer. The initial
        // subscription triggers when the UI starts collecting.
        loadStatuses()
    }

    // ──────────────────────────────────────────────
    // Load / Refresh
    // ──────────────────────────────────────────────

    /**
     * Fetches my statuses and contact statuses.
     * The real-time listeners are always active via StateFlow, but this
     * method can be called to trigger a manual refresh or re-subscription.
     */
    fun loadStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The StateFlow streams are already active; this acts as
                // a trigger point for any additional side-effects such as
                // clearing stale expired statuses or forcing a cache refresh.
                statusRepository.getContactStatuses().collect { /* warm the cache */ }
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to load statuses")
                // Real-time listeners continue regardless
            }
        }
    }

    // ──────────────────────────────────────────────
    // Status Posting
    // ──────────────────────────────────────────────

    /**
     * Posts a text status update with an optional background color.
     *
     * @param text The text content of the status.
     * @param backgroundColor Optional hex color string (e.g. "#FF5722") for the background.
     */
    fun postTextStatus(text: String, backgroundColor: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.update { true }
            _uploadProgress.update { 0f }
            try {
                statusRepository.postStatus(
                    StatusModel(
                        type = StatusContentType.TEXT,
                        textContent = text,
                        backgroundColor = backgroundColor
                    )
                ).collect { result ->
                    result.onSuccess {
                        _uploadProgress.update { 1f }
                    }.onFailure { e ->
                        _errorMessage.update { e.localizedMessage ?: "Failed to post text status" }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to post text status")
                _errorMessage.update { e.localizedMessage ?: "Failed to post text status" }
            } finally {
                _isUploading.update { false }
                _uploadProgress.update { 0f }
            }
        }
    }

    /**
     * Posts a media status (image or video) from a URI with optional caption.
     *
     * @param mediaUri The local URI or file path of the media to upload.
     * @param caption Optional caption text for the media.
     * @param type The content type — [StatusContentType.IMAGE] or [StatusContentType.VIDEO].
     */
    fun postMediaStatus(mediaUri: String, caption: String?, type: StatusContentType) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.update { true }
            _uploadProgress.update { 0f }
            try {
                val status = when (type) {
                    StatusContentType.IMAGE -> StatusModel(
                        type = StatusContentType.IMAGE,
                        mediaUrl = mediaUri,
                        caption = caption
                    )
                    StatusContentType.VIDEO -> StatusModel(
                        type = StatusContentType.VIDEO,
                        mediaUrl = mediaUri,
                        caption = caption
                    )
                    else -> {
                        _errorMessage.update { "Unsupported media type" }
                        return@launch
                    }
                }
                statusRepository.postStatus(status).collect { result ->
                    result.onSuccess {
                        _uploadProgress.update { 1f }
                    }.onFailure { e ->
                        _errorMessage.update { e.localizedMessage ?: "Failed to post media status" }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to post media status")
                _errorMessage.update { e.localizedMessage ?: "Failed to post media status" }
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
     * Deletes a status owned by the current user.
     * Only the owner can delete their own status — enforced at repository layer.
     *
     * @param statusId The ID of the status to delete.
     */
    fun deleteStatus(statusId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusRepository.deleteStatus(statusId).collect { result ->
                    result.onFailure { e ->
                        Timber.e(e, "StatusViewModel: Failed to delete status")
                        _errorMessage.update { e.localizedMessage ?: "Failed to delete status" }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to delete status")
                _errorMessage.update { e.localizedMessage ?: "Failed to delete status" }
            }
        }
    }

    /**
     * Marks a status as viewed by the current user.
     * Only mutual contacts can view statuses — enforced at repository layer.
     *
     * @param statusId The ID of the status to mark as viewed.
     */
    fun markStatusViewed(statusId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusRepository.markStatusViewed(statusId).collect { result ->
                    result.onFailure { e ->
                        Timber.e(e, "StatusViewModel: Failed to mark status as viewed")
                        _errorMessage.update { e.localizedMessage ?: "Failed to mark status as viewed" }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to mark status as viewed")
                _errorMessage.update { e.localizedMessage ?: "Failed to mark status as viewed" }
            }
        }
    }

    /**
     * Reacts to a contact's status with an emoji.
     * Only mutual contacts can react — enforced at repository layer.
     *
     * @param statusId The ID of the status to react to.
     * @param emoji The emoji character to react with.
     */
    fun reactToStatus(statusId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusRepository.reactToStatus(statusId, emoji).collect { result ->
                    result.onFailure { e ->
                        Timber.e(e, "StatusViewModel: Failed to add reaction")
                        _errorMessage.update { e.localizedMessage ?: "Failed to add reaction" }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to add reaction")
                _errorMessage.update { e.localizedMessage ?: "Failed to add reaction" }
            }
        }
    }

    // ──────────────────────────────────────────────
    // Privacy Configuration
    // ──────────────────────────────────────────────

    /**
     * Updates the status privacy configuration.
     * Persists the new settings through [SettingsRepository].
     *
     * @param config The new privacy configuration to apply.
     */
    fun updatePrivacyConfig(config: StatusPrivacyConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.updateStatusPrivacy(config.option)
                // Excluded/share-with lists would be persisted via additional
                // settings methods when available in the repository layer.
            } catch (e: Exception) {
                Timber.e(e, "StatusViewModel: Failed to update privacy settings")
                _errorMessage.update { e.localizedMessage ?: "Failed to update privacy settings" }
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
            markStatusViewed(firstStatus.id)
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
            markStatusViewed(nextStatus.id)
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
            markStatusViewed(prevStatus.id)
        }
    }

    /**
     * Sets the current viewing index directly (used by HorizontalPager).
     *
     * @param index The index to navigate to.
     */
    fun setViewingIndex(index: Int) {
        val group = _viewingStatusGroup.value ?: return
        if (index in group.statuses.indices) {
            _currentViewingIndex.update { index }
            val status = group.statuses[index]
            markStatusViewed(status.id)
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
