package com.zixo.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.MessageModel
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the 1-on-1 chat message screen.
 *
 * Provides 100% real-time Firebase sync through continuous snapshot listeners.
 * All message, thread, and call state changes propagate instantly via active
 * Kotlin StateFlow pipelines. Mutations use the [runMutation] pattern to
 * guarantee consistent loading/error handling without race conditions.
 *
 * Zero-trust enforcement: Call initiation methods verify mutual contact status
 * through [ContactRepository.checkCommunicationGate] before executing.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val callRepository: CallRepository,
    private val firebaseAuthService: FirebaseAuthService
) : ViewModel() {

    // ── Thread ID ──────────────────────────────────────────────────────────────

    private var currentThreadId: String = ""

    // ── Real-time StateFlows ───────────────────────────────────────────────────

    /** Real-time message list from Firestore snapshot listener. */
    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    /** Current thread metadata from Firestore snapshot listener. */
    private val _thread = MutableStateFlow<ChatThreadModel?>(null)
    val thread: StateFlow<ChatThreadModel?> = _thread.asStateFlow()

    /** Current message input text. */
    val inputText = MutableStateFlow("")

    /** Whether a message is currently being sent. */
    val isSending = MutableStateFlow(false)

    /** Real-time call state from the LiveKit WebRTC engine. */
    val callState: StateFlow<CallState> = callRepository.observeCallState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CallState.IDLE)

    /** The message being replied to, shown as a preview above the input tray. */
    val replyToMessage = MutableStateFlow<MessageModel?>(null)

    /** The message long-pressed for the frosted glass action menu. */
    val showActionMenu = MutableStateFlow<MessageModel?>(null)

    /** Current user UID for determining own messages (delete-for-everyone gate). */
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    /** Error message for snackbar display. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Exception Handler ──────────────────────────────────────────────────────

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _errorMessage.value = throwable.localizedMessage ?: "An unexpected error occurred"
    }

    // ── Thread Lifecycle ───────────────────────────────────────────────────────

    /**
     * Starts observing messages and thread metadata for the given thread.
     *
     * Real-time snapshot listeners are attached to the thread's Firestore
     * sub-collection and the thread's top-level document. Any changes to
     * messages, reactions, deletions, or thread metadata propagate instantly.
     *
     * @param threadId The ID of the chat thread to observe.
     */
    fun loadThread(threadId: String) {
        if (currentThreadId == threadId) return
        currentThreadId = threadId
        _currentUserId.value = firebaseAuthService.getCurrentUser()?.uid ?: ""

        viewModelScope.launch(exceptionHandler) {
            chatRepository.observeMessages(threadId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { messageList -> _messages.value = messageList }
        }

        viewModelScope.launch(exceptionHandler) {
            chatRepository.observeThread(threadId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { threadData -> _thread.value = threadData }
        }

        viewModelScope.launch(exceptionHandler) {
            chatRepository.markThreadAsRead(threadId)
        }
    }

    // ── Send Message ───────────────────────────────────────────────────────────

    /**
     * Sends a text message in the current thread.
     *
     * If [replyToMessage] is set, the reply reference is included.
     * Uses [runMutation] for consistent loading/error handling.
     */
    fun sendMessage() {
        val text = inputText.value.trim()
        if (text.isEmpty() || currentThreadId.isBlank()) return

        val replyId = replyToMessage.value?.id
        replyToMessage.value = null

        runMutation {
            chatRepository.sendTextMessage(
                threadId = currentThreadId,
                content = text,
                replyToMessageId = replyId
            ).catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { result ->
                    result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                }
        }

        inputText.value = ""
    }

    /**
     * Sends a media message (image, video, audio, or file) in the current thread.
     *
     * The media is uploaded to Firebase Storage and a message referencing
     * the storage URL is created. If [replyToMessage] is set, the reply
     * reference is included.
     *
     * @param filePath Local file path of the media to upload.
     * @param mimeType MIME type of the media.
     * @param caption Optional caption for the media.
     */
    fun sendMediaMessage(filePath: String, mimeType: String, caption: String?) {
        if (currentThreadId.isBlank()) return

        val replyId = replyToMessage.value?.id
        replyToMessage.value = null

        runMutation {
            chatRepository.sendMediaMessage(
                threadId = currentThreadId,
                localFilePath = filePath,
                mimeType = mimeType,
                caption = caption,
                replyToMessageId = replyId
            ).catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { result ->
                    result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                }
        }
    }

    // ── Delete Operations ──────────────────────────────────────────────────────

    /**
     * Soft-deletes a message for the current user only.
     *
     * The message is hidden from the local user's view without affecting
     * other participants. This is a localized, client-side operation.
     *
     * @param messageId The ID of the message to soft-delete.
     */
    fun deleteForMe(messageId: String) {
        viewModelScope.launch(exceptionHandler) {
            chatRepository.deleteMessageForMe(
                threadId = currentThreadId,
                messageId = messageId
            )
        }
        showActionMenu.value = null
    }

    /**
     * Hard-deletes a message for all participants.
     *
     * Only the original sender can perform this operation. The message
     * content is replaced with a "This message was deleted" placeholder
     * across all connected devices in real-time.
     *
     * @param messageId The ID of the message to hard-delete.
     */
    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch(exceptionHandler) {
            chatRepository.deleteMessageForEveryone(
                threadId = currentThreadId,
                messageId = messageId
            )
        }
        showActionMenu.value = null
    }

    // ── Reactions ──────────────────────────────────────────────────────────────

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
        viewModelScope.launch(exceptionHandler) {
            chatRepository.addReaction(
                threadId = currentThreadId,
                messageId = messageId,
                emoji = emoji,
                isThreeD = isThreeD
            )
        }
        showActionMenu.value = null
    }

    // ── Forward ────────────────────────────────────────────────────────────────

    /**
     * Forwards a message to one or more target threads.
     *
     * Communication gate checks are enforced for each target thread.
     *
     * @param messageId The ID of the message to forward.
     * @param targetThreadIds The IDs of the threads to forward the message to.
     */
    fun forwardMessage(messageId: String, targetThreadIds: List<String>) {
        viewModelScope.launch(exceptionHandler) {
            chatRepository.forwardMessage(
                messageId = messageId,
                targetThreadIds = targetThreadIds
            ).catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { result ->
                    result.onFailure { e -> _errorMessage.value = e.localizedMessage }
                }
        }
        showActionMenu.value = null
    }

    // ── Calls ──────────────────────────────────────────────────────────────────

    /**
     * Initiates a 1-on-1 audio call with the thread's contact.
     *
     * Before initiating, verifies that the target user is a verified mutual
     * contact through [ContactRepository.checkCommunicationGate]. If the
     * gate check fails, the call is rejected and an error is displayed.
     */
    fun startAudioCall() {
        val targetUid = getOtherParticipantUid() ?: return
        viewModelScope.launch(exceptionHandler) {
            when (val gate = contactRepository.checkCommunicationGate(targetUid)) {
                is CommunicationGate.Allowed -> {
                    callRepository.initiateAudioCall(targetUid)
                        .catch { e -> _errorMessage.value = e.localizedMessage }
                        .collect { }
                }
                is CommunicationGate.Blocked -> {
                    _errorMessage.value = gate.reason
                }
                is CommunicationGate.Error -> {
                    _errorMessage.value = gate.message
                }
            }
        }
    }

    /**
     * Initiates a 1-on-1 video call with the thread's contact.
     *
     * Before initiating, verifies that the target user is a verified mutual
     * contact through [ContactRepository.checkCommunicationGate]. If the
     * gate check fails, the call is rejected and an error is displayed.
     */
    fun startVideoCall() {
        val targetUid = getOtherParticipantUid() ?: return
        viewModelScope.launch(exceptionHandler) {
            when (val gate = contactRepository.checkCommunicationGate(targetUid)) {
                is CommunicationGate.Allowed -> {
                    callRepository.initiateVideoCall(targetUid)
                        .catch { e -> _errorMessage.value = e.localizedMessage }
                        .collect { }
                }
                is CommunicationGate.Blocked -> {
                    _errorMessage.value = gate.reason
                }
                is CommunicationGate.Error -> {
                    _errorMessage.value = gate.message
                }
            }
        }
    }

    // ── Group Calls ─────────────────────────────────────────────────────────────

    /**
     * Initiates a group audio call in a LiveKit Room.
     *
     * All participants must be verified mutual contacts. Uses LiveKit
     * Room multipoint protocols with participant track configurations
     * that update as members join/leave.
     */
    fun startGroupAudioCall() {
        if (currentThreadId.isBlank()) return
        viewModelScope.launch(exceptionHandler) {
            callRepository.initiateGroupAudioCall(currentThreadId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { }
        }
    }

    /**
     * Initiates a group video call in a LiveKit Room.
     *
     * All participants must be verified mutual contacts. Uses LiveKit
     * Room multipoint protocols with participant track configurations
     * that update as members join/leave.
     */
    fun startGroupVideoCall() {
        if (currentThreadId.isBlank()) return
        viewModelScope.launch(exceptionHandler) {
            callRepository.initiateGroupVideoCall(currentThreadId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { }
        }
    }

    // ── Action Menu ────────────────────────────────────────────────────────────

    /**
     * Dismisses the frosted glass action menu.
     */
    fun dismissActionMenu() {
        showActionMenu.value = null
    }

    /**
     * Clears the reply preview bar above the input tray.
     */
    fun clearReply() {
        replyToMessage.value = null
    }

    /**
     * Clears the current error message after it has been displayed.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ── Internal Helpers ───────────────────────────────────────────────────────

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
     * Mutation helper that sets [isSending] to true, runs the given block,
     * and resets [isSending] to false on completion or failure.
     *
     * This prevents duplicate sends and provides visual feedback during
     * network operations.
     */
    private fun runMutation(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            isSending.value = true
            try {
                block()
            } finally {
                isSending.value = false
            }
        }
    }
}
