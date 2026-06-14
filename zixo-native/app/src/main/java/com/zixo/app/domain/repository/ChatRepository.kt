package com.zixo.app.domain.repository

import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.MessageActionResult
import com.zixo.app.domain.model.MessageModel
import kotlinx.coroutines.flow.Flow

/**
 * Chat Repository Interface — 100% Real-time Chat & Group Messaging
 *
 * All data handlers use continuous database snapshot sockets
 * (.addSnapshotListener / .addValueEventListener). Changes to texts,
 * presence vectors, deletion requests, or reactions appear instantly
 * across devices via active Kotlin StateFlow pipes.
 *
 * Zero-trust enforcement: All send operations verify mutual contact
 * status through [ContactRepository.checkCommunicationGate] before
 * executing. Non-contact messages are blocked at this boundary.
 */
interface ChatRepository {

    /**
     * Observes all chat threads for the current user in real-time.
     *
     * Uses Firestore [addSnapshotListener] for continuous synchronization.
     * New threads, updated last messages, unread counts, and presence
     * changes propagate instantly through the active StateFlow pipeline.
     *
     * @return A flow emitting the current list of [ChatThreadModel] entries,
     *         sorted by pinned status and last message timestamp.
     */
    fun observeThreads(): Flow<List<ChatThreadModel>>

    /**
     * Observes a single chat thread by its ID.
     *
     * @param threadId The ID of the thread to observe.
     * @return A flow emitting the current [ChatThreadModel], or null if not found.
     */
    fun observeThread(threadId: String): Flow<ChatThreadModel?>

    /**
     * Observes all messages in a thread in real-time.
     *
     * Uses Firestore [addSnapshotListener] on the thread's messages
     * sub-collection. New messages, edits, deletions, and reactions
     * appear instantly without manual refresh.
     *
     * @param threadId The ID of the thread to observe messages for.
     * @return A flow emitting the current list of [MessageModel] entries,
     *         ordered by timestamp ascending.
     */
    fun observeMessages(threadId: String): Flow<List<MessageModel>>

    /**
     * Sends a text message to a thread.
     *
     * Before sending, this method verifies that the current user has
     * communication access to all thread participants via the
     * contact-gated communication whitelist. If any participant is
     * not a mutual contact, the message is rejected.
     *
     * @param threadId The ID of the target thread.
     * @param content The text content of the message.
     * @param replyToMessageId Optional ID of the message being replied to.
     * @return A flow emitting the sent [MessageModel] or an error.
     */
    fun sendTextMessage(
        threadId: String,
        content: String,
        replyToMessageId: String? = null
    ): Flow<Result<MessageModel>>

    /**
     * Sends a media message (image, video, audio, or file) to a thread.
     *
     * The media is uploaded to Firebase Storage, and a message referencing
     * the storage URL is created in the thread. Communication gate checks
     * are enforced before upload begins.
     *
     * @param threadId The ID of the target thread.
     * @param localFilePath The local file path of the media to upload.
     * @param mimeType The MIME type of the media.
     * @param caption Optional caption for the media.
     * @param replyToMessageId Optional ID of the message being replied to.
     * @return A flow emitting upload progress (0.0 to 1.0) and the final result.
     */
    fun sendMediaMessage(
        threadId: String,
        localFilePath: String,
        mimeType: String,
        caption: String? = null,
        replyToMessageId: String? = null
    ): Flow<Result<MessageModel>>

    /**
     * Forwards a message to one or more target threads.
     *
     * Communication gate checks are enforced for each target thread.
     *
     * @param messageId The ID of the message to forward.
     * @param targetThreadIds The IDs of the threads to forward the message to.
     * @return A flow emitting the result for each target thread.
     */
    fun forwardMessage(
        messageId: String,
        targetThreadIds: List<String>
    ): Flow<Result<Unit>>

    /**
     * Deletes a message for the current user only ("Delete for Me").
     *
     * This is a soft delete that hides the message from the local
     * user's view without affecting other participants.
     *
     * @param threadId The ID of the thread containing the message.
     * @param messageId The ID of the message to delete.
     */
    suspend fun deleteMessageForMe(threadId: String, messageId: String)

    /**
     * Deletes a message for all participants ("Delete for Everyone").
     *
     * This is a hard delete that removes the message content from
     * all devices. Only the sender can perform this operation, and
     * only within a limited time window after sending.
     *
     * @param threadId The ID of the thread containing the message.
     * @param messageId The ID of the message to delete.
     */
    suspend fun deleteMessageForEveryone(threadId: String, messageId: String)

    /**
     * Adds a reaction to a message.
     *
     * Each user can have only one active reaction per message.
     * Adding a new reaction replaces the previous one.
     *
     * @param threadId The ID of the thread containing the message.
     * @param messageId The ID of the message to react to.
     * @param emoji The emoji character to react with.
     * @param isThreeD Whether this is a 3D emoji variant.
     */
    suspend fun addReaction(
        threadId: String,
        messageId: String,
        emoji: String,
        isThreeD: Boolean = false
    )

    /**
     * Marks messages in a thread as read up to the current time.
     *
     * @param threadId The ID of the thread to mark as read.
     */
    suspend fun markThreadAsRead(threadId: String)

    /**
     * Creates a new 1-on-1 chat thread with a specific user.
     *
     * Before creating, this method verifies that the target user is
     * a verified mutual contact. If not, the operation is rejected.
     *
     * @param contactUid The UID of the contact to start a chat with.
     * @return A flow emitting the created [ChatThreadModel] or an error.
     */
    fun createSingleThread(contactUid: String): Flow<Result<ChatThreadModel>>

    /**
     * Creates a new group chat thread.
     *
     * All initial participants must be verified mutual contacts of
     * the group creator. Any non-mutual participants are excluded.
     *
     * @param groupName The name of the group.
     * @param participantUids The UIDs of the initial participants.
     * @return A flow emitting the created [ChatThreadModel] or an error.
     */
    fun createGroupThread(
        groupName: String,
        participantUids: List<String>
    ): Flow<Result<ChatThreadModel>>

    /**
     * Pins or unpins a chat thread.
     *
     * @param threadId The ID of the thread to pin/unpin.
     * @param isPinned Whether the thread should be pinned.
     */
    suspend fun setThreadPinned(threadId: String, isPinned: Boolean)

    /**
     * Mutes or unmutes a chat thread.
     *
     * @param threadId The ID of the thread to mute/unmute.
     * @param isMuted Whether the thread should be muted.
     */
    suspend fun setThreadMuted(threadId: String, isMuted: Boolean)

    /**
     * Archives or unarchives a chat thread.
     *
     * @param threadId The ID of the thread to archive/unarchive.
     * @param isArchived Whether the thread should be archived.
     */
    suspend fun setThreadArchived(threadId: String, isArchived: Boolean)

    /**
     * Sets the ephemeral message timer for a specific thread.
     *
     * @param threadId The ID of the thread.
     * @param timerSeconds The timer duration in seconds (0 = off).
     */
    suspend fun setThreadEphemeralTimer(threadId: String, timerSeconds: Int)

    /**
     * Handles a message action result from the interactive action menu.
     *
     * @param action The [MessageActionResult] to process.
     */
    suspend fun handleActionResult(action: MessageActionResult)
}
