package com.zixo.app.domain.repository

import com.zixo.app.domain.model.ChatThreadModel
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
 * status through [ContactRepository.verifyMutualContact] before
 * executing. Non-contact messages are blocked at this boundary.
 */
interface ChatRepository {

    /**
     * Gets all chat threads for the current user.
     * Sorted by pinned status and last message timestamp.
     *
     * @return A flow emitting the current list of [ChatThreadModel] entries.
     */
    fun getThreads(): Flow<List<ChatThreadModel>>

    /**
     * Observes all chat threads in real-time using Firestore addSnapshotListener.
     * New threads, updated last messages, unread counts, and presence
     * changes propagate instantly through the active StateFlow pipeline.
     *
     * @return A flow emitting the current list of [ChatThreadModel] entries.
     */
    fun observeThreadsRealtime(): Flow<List<ChatThreadModel>>

    /**
     * Gets all messages in a thread.
     *
     * @param threadId The ID of the thread.
     * @return A flow emitting the current list of [MessageModel] entries.
     */
    fun getMessages(threadId: String): Flow<List<MessageModel>>

    /**
     * Observes all messages in a thread in real-time via addSnapshotListener.
     * New messages, edits, deletions, and reactions appear instantly.
     *
     * @param threadId The ID of the thread.
     * @return A flow emitting the current list of [MessageModel] entries,
     *         ordered by timestamp ascending.
     */
    fun observeMessagesRealtime(threadId: String): Flow<List<MessageModel>>

    /**
     * Sends a message to a thread.
     * Before sending, verifies that the current user has communication
     * access to all thread participants via the contact-gated whitelist.
     *
     * @param threadId The ID of the target thread.
     * @param message The [MessageModel] to send.
     * @return A flow emitting the sent [MessageModel] or an error.
     */
    fun sendMessage(threadId: String, message: MessageModel): Flow<Result<MessageModel>>

    /**
     * Deletes a message for the current user only ("Delete for Me").
     * Soft delete that hides the message from the local user's view.
     *
     * @param messageId The ID of the message to delete.
     * @param threadId The ID of the thread containing the message.
     * @return A flow emitting Result success or failure.
     */
    fun deleteForMe(messageId: String, threadId: String): Flow<Result<Unit>>

    /**
     * Deletes a message for all participants ("Delete for Everyone").
     * Hard delete that removes the message content from all devices.
     *
     * @param messageId The ID of the message to delete.
     * @param threadId The ID of the thread containing the message.
     * @return A flow emitting Result success or failure.
     */
    fun deleteForEveryone(messageId: String, threadId: String): Flow<Result<Unit>>

    /**
     * Adds a reaction to a message.
     * Each user can have only one active reaction per message.
     *
     * @param messageId The ID of the message to react to.
     * @param threadId The ID of the thread containing the message.
     * @param emoji The emoji character to react with.
     * @param isThreeD Whether this is a 3D emoji variant.
     * @return A flow emitting Result success or failure.
     */
    fun addReaction(
        messageId: String,
        threadId: String,
        emoji: String,
        isThreeD: Boolean = false
    ): Flow<Result<Unit>>

    /**
     * Marks all messages in a thread as read up to the current time.
     *
     * @param threadId The ID of the thread to mark as read.
     * @return A flow emitting Result success or failure.
     */
    fun markAsRead(threadId: String): Flow<Result<Unit>>

    /**
     * Creates a new group chat thread.
     * All initial participants must be verified mutual contacts of
     * the group creator. Any non-mutual participants are excluded.
     *
     * @param name The name of the group.
     * @param participantUids The UIDs of the initial participants.
     * @return A flow emitting the created [ChatThreadModel] or an error.
     */
    fun createGroupThread(
        name: String,
        participantUids: Set<String>
    ): Flow<Result<ChatThreadModel>>
}
