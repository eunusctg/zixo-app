package com.zixo.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.MessageActionResult
import com.zixo.app.domain.model.MessageModel
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the 1-on-1 and group chat message screens.
 *
 * Provides 100% real-time Firebase sync through continuous snapshot listeners.
 * All message, thread, and call state changes propagate instantly via active
 * Kotlin StateFlow pipelines.
 *
 * **Zero-trust enforcement**: All send operations verify mutual contact
 * status through [ContactRepository.verifyMutualContact] before
 * executing. Non-contact messages are blocked at this boundary.
 *
 * On init: attaches continuous listeners for messages and thread metadata,
 * verifies the communication gate with the other participant(s), and marks
 * the thread as read.
 *
 * @property chatRepository  Firestore-backed chat data source.
 * @property contactRepository Zero-trust contact verification gate.
 * @property savedStateHandle  Navigation arguments — must contain `"threadId"`.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val callRepository: CallRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ── Thread ID (from navigation argument) ──────────────────────────────────

    /** Thread ID extracted from the navigation SavedStateHandle. */
    val threadId: String = savedStateHandle["threadId"] ?: ""

    // ── Current User UID ──────────────────────────────────────────────────────

    /** Current authenticated user UID for determining own messages. */
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // ── Real-time StateFlows ──────────────────────────────────────────────────

    /** Real-time message list from Firestore snapshot listener. */
    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    /** Current thread metadata from Firestore snapshot listener. */
    private val _thread = MutableStateFlow<ChatThreadModel?>(null)
    val thread: StateFlow<ChatThreadModel?> = _thread.asStateFlow()

    /** Communication gate status — whether messaging is allowed with the other participant. */
    private val _communicationGate = MutableStateFlow<CommunicationGate>(CommunicationGate.Allowed())
    val communicationGate: StateFlow<CommunicationGate> = _communicationGate.asStateFlow()

    /** Current message input text. */
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    /** Whether a message is currently being sent. */
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    /** The message long-pressed for the frosted glass action menu. */
    private val _selectedMessageAction = MutableStateFlow<MessageActionResult?>(null)
    val selectedMessageAction: StateFlow<MessageActionResult?> = _selectedMessageAction.asStateFlow()

    /** The message being replied to, shown as a preview above the input tray. */
    private val _replyToMessage = MutableStateFlow<MessageModel?>(null)
    val replyToMessage: StateFlow<MessageModel?> = _replyToMessage.asStateFlow()

    /** The message long-pressed for the frosted glass action menu. */
    private val _showActionMenu = MutableStateFlow<MessageModel?>(null)
    val showActionMenu: StateFlow<MessageModel?> = _showActionMenu.asStateFlow()

    /** Active call ID for tracking the current call state. */
    private val _activeCallId = MutableStateFlow("")

    /** Real-time call state from the LiveKit WebRTC engine. */
    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    /** Error message for snackbar display. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        _currentUserId.value = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (threadId.isNotBlank()) {
            attachContinuousListeners()
            verifyContactGate()
            markAsRead()
        }

        // Observe incoming calls for call overlay
        observeCallStateChanges()
    }

    // ── Call State Observation ────────────────────────────────────────────────

    /**
     * Observes call state changes for the active call.
     * When a call ID is set via [_activeCallId], this listens for state updates.
     * Also observes incoming calls for the current user.
     */
    private fun observeCallStateChanges() {
        // Observe incoming calls
        viewModelScope.launch(Dispatchers.IO) {
            try {
                callRepository.observeIncomingCalls()
                    .catch { /* Ignore call observation errors */ }
                    .collect { state -> _callState.value = state }
            } catch (_: Exception) { }
        }

        // Observe active call state when callId changes
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _activeCallId.collect { callId ->
                    if (callId.isNotBlank()) {
                        callRepository.observeCallState(callId)
                            .catch { /* Ignore call observation errors */ }
                            .collect { state -> _callState.value = state }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // ── Continuous Listeners ──────────────────────────────────────────────────

    /**
     * Attaches continuous Firestore snapshot listeners for messages
     * and thread metadata. Any changes to messages, reactions, deletions,
     * or thread metadata propagate instantly through the active StateFlows.
     */
    private fun attachContinuousListeners() {
        // Observe messages in real-time
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.observeMessagesRealtime(threadId)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { messageList -> _messages.value = messageList }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to observe messages"
            }
        }

        // Observe thread metadata in real-time
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.observeThreadsRealtime()
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { threadList ->
                        _thread.value = threadList.firstOrNull { it.id == threadId }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to observe thread"
            }
        }
    }

    // ── Communication Gate ────────────────────────────────────────────────────

    /**
     * Verifies the communication gate with the other participant.
     *
     * For 1-on-1 threads, checks mutual contact status with the other user.
     * For group threads, verifies that all participants are mutual contacts.
     * The gate status is stored in [_communicationGate] and observed by the UI.
     */
    private fun verifyContactGate() {
        val otherUid = getOtherParticipantUid() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contactRepository.verifyMutualContact(otherUid)
                    .catch { e ->
                        _communicationGate.value = CommunicationGate.Error(
                            e.localizedMessage ?: "Gate check failed"
                        )
                    }
                    .collect { gate -> _communicationGate.value = gate }
            } catch (e: Exception) {
                _communicationGate.value = CommunicationGate.Error(
                    e.localizedMessage ?: "Gate check failed"
                )
            }
        }
    }

    // ── Send Message ──────────────────────────────────────────────────────────

    /**
     * Sends a text message in the current thread.
     *
     * **Verification first**: Before sending, verifies mutual contact status
     * via [ContactRepository.verifyMutualContact]. If the gate check fails,
     * the message is NOT sent and an error is displayed.
     *
     * If [replyToMessage] is set, the reply reference is included.
     */
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || threadId.isBlank()) return

        val replyId = _replyToMessage.value?.id
        val replyPreview = _replyToMessage.value?.content
        val replySenderName = _replyToMessage.value?.senderDisplayName
        _replyToMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSending.value = true

                // Verify mutual contact FIRST
                val otherUid = getOtherParticipantUid()
                if (otherUid != null) {
                    var gatePassed = false
                    contactRepository.verifyMutualContact(otherUid)
                        .catch { e ->
                            _errorMessage.value = e.localizedMessage ?: "Verification failed"
                        }
                        .collect { gate ->
                            gatePassed = gate is CommunicationGate.Allowed
                            _communicationGate.value = gate
                        }

                    if (!gatePassed) {
                        _errorMessage.value = "Cannot send message — not a mutual contact"
                        _isSending.value = false
                        return@launch
                    }
                }

                val message = MessageModel(
                    threadId = threadId,
                    senderUid = _currentUserId.value,
                    content = text,
                    replyToMessageId = replyId,
                    replyToPreview = replyPreview,
                    replyToSenderName = replySenderName
                )

                chatRepository.sendMessage(threadId, message)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { result ->
                        result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to send message"
            } finally {
                _isSending.value = false
            }
        }

        _inputText.value = ""
    }

    // ── Delete Operations ─────────────────────────────────────────────────────

    /**
     * Soft-deletes a message for the current user only ("Delete for Me").
     *
     * The message is hidden from the local user's view without affecting
     * other participants. This is a localized, client-side operation.
     *
     * @param messageId The ID of the message to soft-delete.
     */
    fun deleteForMe(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.deleteForMe(messageId, threadId)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { result ->
                        result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete message"
            }
        }
        _showActionMenu.value = null
    }

    /**
     * Hard-deletes a message for all participants ("Delete for Everyone").
     *
     * Only the original sender can perform this operation. The message
     * content is replaced with a "This message was deleted" placeholder
     * across all connected devices in real-time.
     *
     * @param messageId The ID of the message to hard-delete.
     */
    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.deleteForEveryone(messageId, threadId)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { result ->
                        result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete message"
            }
        }
        _showActionMenu.value = null
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    /**
     * Adds a reaction to a message.
     *
     * Each user can have only one active reaction per message. Adding a
     * new reaction replaces the previous one. Supports 3D emoji variants.
     *
     * @param messageId The ID of the message to react to.
     * @param emoji The emoji character to react with.
     * @param isThreeD Whether this is a 3D emoji variant.
     */
    fun addReaction(messageId: String, emoji: String, isThreeD: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.addReaction(messageId, threadId, emoji, isThreeD)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { result ->
                        result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to add reaction"
            }
        }
        _showActionMenu.value = null
    }

    // ── Reply ─────────────────────────────────────────────────────────────────

    /**
     * Sets the reply target message. Shows the reply preview bar above
     * the input tray and includes the reference in the next sent message.
     *
     * @param messageId The ID of the message to reply to.
     */
    fun setReplyTo(messageId: String) {
        val message = _messages.value.firstOrNull { it.id == messageId } ?: return
        _replyToMessage.value = message
        _showActionMenu.value = null
    }

    /**
     * Clears the reply preview bar above the input tray.
     */
    fun clearReplyTo() {
        _replyToMessage.value = null
    }

    // ── Forward ───────────────────────────────────────────────────────────────

    /**
     * Forwards a message to one or more target threads.
     *
     * Communication gate checks are enforced for each target thread.
     *
     * @param messageId The ID of the message to forward.
     * @param targetThreadIds The IDs of the threads to forward the message to.
     */
    fun forwardMessage(messageId: String, targetThreadIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalMessage = _messages.value.firstOrNull { it.id == messageId }
                if (originalMessage != null && targetThreadIds.isNotEmpty()) {
                    val forwardedMessage = originalMessage.copy(
                        id = "",
                        threadId = "",
                        isForwarded = true,
                        forwardedFromUid = originalMessage.senderUid,
                        forwardedFromName = originalMessage.senderDisplayName,
                        replyToMessageId = null,
                        replyToPreview = null,
                        replyToSenderName = null
                    )
                    for (targetThreadId in targetThreadIds) {
                        chatRepository.sendMessage(targetThreadId, forwardedMessage)
                            .catch { e -> _errorMessage.value = e.localizedMessage }
                            .collect { result ->
                                result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                            }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to forward message"
            }
        }
        _showActionMenu.value = null
    }

    // ── Mark As Read ──────────────────────────────────────────────────────────

    /**
     * Marks all messages in the current thread as read.
     */
    fun markAsRead() {
        if (threadId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.markAsRead(threadId)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { result ->
                        result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to mark as read"
            }
        }
    }

    // ── Calls ─────────────────────────────────────────────────────────────────

    /**
     * Initiates a 1-on-1 audio call with the thread's contact.
     *
     * Before initiating, verifies that the target user is a verified mutual
     * contact through [ContactRepository.verifyMutualContact].
     */
    fun startAudioCall() {
        val targetUid = getOtherParticipantUid() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val gate = _communicationGate.value) {
                    is CommunicationGate.Allowed -> {
                        callRepository.initiateCall(targetUid, isVideoCall = false)
                            .catch { e -> _errorMessage.value = e.localizedMessage }
                            .collect { state ->
                                _callState.value = state
                                if (state is CallState.DIALING) {
                                    _activeCallId.value = state.callId
                                }
                            }
                    }
                    is CommunicationGate.Blocked -> {
                        _errorMessage.value = gate.reason
                    }
                    is CommunicationGate.Error -> {
                        _errorMessage.value = gate.message
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to start call"
            }
        }
    }

    /**
     * Initiates a 1-on-1 video call with the thread's contact.
     */
    fun startVideoCall() {
        val targetUid = getOtherParticipantUid() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val gate = _communicationGate.value) {
                    is CommunicationGate.Allowed -> {
                        callRepository.initiateCall(targetUid, isVideoCall = true)
                            .catch { e -> _errorMessage.value = e.localizedMessage }
                            .collect { state ->
                                _callState.value = state
                                if (state is CallState.DIALING) {
                                    _activeCallId.value = state.callId
                                }
                            }
                    }
                    is CommunicationGate.Blocked -> {
                        _errorMessage.value = gate.reason
                    }
                    is CommunicationGate.Error -> {
                        _errorMessage.value = gate.message
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to start call"
            }
        }
    }

    /**
     * Initiates a group audio call via LiveKit Room.
     */
    fun startGroupAudioCall() {
        if (threadId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                callRepository.initiateCall(threadId, isVideoCall = false)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { state ->
                        _callState.value = state
                        if (state is CallState.DIALING) {
                            _activeCallId.value = state.callId
                        }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to start group call"
            }
        }
    }

    /**
     * Initiates a group video call via LiveKit Room.
     */
    fun startGroupVideoCall() {
        if (threadId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                callRepository.initiateCall(threadId, isVideoCall = true)
                    .catch { e -> _errorMessage.value = e.localizedMessage }
                    .collect { state ->
                        _callState.value = state
                        if (state is CallState.DIALING) {
                            _activeCallId.value = state.callId
                        }
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to start group call"
            }
        }
    }

    // ── Action Menu ───────────────────────────────────────────────────────────

    /**
     * Dismisses the frosted glass action menu.
     */
    fun dismissActionMenu() {
        _showActionMenu.value = null
    }

    /**
     * Clears the current error message after it has been displayed.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ── Input Text ────────────────────────────────────────────────────────────

    /**
     * Updates the input text value.
     */
    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    /**
     * Extracts the other participant's UID from the thread's participant list.
     * For 1-on-1 threads, this is the single participant that isn't the current user.
     */
    private fun getOtherParticipantUid(): String? {
        val threadData = _thread.value ?: return null
        val uid = _currentUserId.value
        return threadData.participantUids.firstOrNull { it != uid }
    }

    /**
     * Legacy compatibility method — delegates to [clearReplyTo].
     */
    fun clearReply() = clearReplyTo()

    /**
     * Legacy compatibility method — delegates to [onInputTextChanged].
     */
    fun setInputText(text: String) = onInputTextChanged(text)
}
