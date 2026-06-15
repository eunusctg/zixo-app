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

    /**
     * Gets a single chat thread by its ID.
     *
     * @param chatId The ID of the thread to retrieve.
     * @return A flow emitting the [ChatThreadModel] or null if not found.
     */
    fun getChatThread(chatId: String): Flow<ChatThreadModel?>

    /**
     * Gets the list of group members for a specific group chat thread.
     *
     * @param chatId The ID of the group chat thread.
     * @return A flow emitting the list of [ThreadParticipant] entries.
     */
    fun getGroupMembers(chatId: String): Flow<List<com.zixo.app.domain.model.ThreadParticipant>>

    /**
     * Updates the name of a group chat.
     * Only admins can update the group name.
     *
     * @param chatId The ID of the group chat thread.
     * @param name The new name for the group.
     * @return A flow emitting Result success or failure.
     */
    fun updateGroupName(chatId: String, name: String): Flow<Result<Unit>>

    /**
     * Updates the description of a group chat.
     * Only admins can update the group description.
     *
     * @param chatId The ID of the group chat thread.
     * @param description The new description for the group.
     * @return A flow emitting Result success or failure.
     */
    fun updateGroupDescription(chatId: String, description: String): Flow<Result<Unit>>

    /**
     * Updates the role of a group member.
     * Only admins can change member roles.
     *
     * @param chatId The ID of the group chat thread.
     * @param userId The UID of the member whose role is being changed.
     * @param role The new role to assign.
     * @return A flow emitting Result success or failure.
     */
    fun updateMemberRole(chatId: String, userId: String, role: com.zixo.app.domain.model.ParticipantRole): Flow<Result<Unit>>

    /**
     * Removes a member from a group chat.
     * Only admins can remove members.
     *
     * @param chatId The ID of the group chat thread.
     * @param userId The UID of the member to remove.
     * @return A flow emitting Result success or failure.
     */
    fun removeGroupMember(chatId: String, userId: String): Flow<Result<Unit>>

    /**
     * Allows the current user to leave a group chat.
     * Admins must transfer ownership before leaving if they are the last admin.
     *
     * @param chatId The ID of the group chat thread to leave.
     * @return A flow emitting Result success or failure.
     */
    fun leaveGroup(chatId: String): Flow<Result<Unit>>

    /**
     * Toggles the mute state of a chat thread for the current user.
     *
     * @param chatId The ID of the chat thread.
     * @param isMuted Whether the thread should be muted.
     * @return A flow emitting Result success or failure.
     */
    fun toggleMuteChat(chatId: String, isMuted: Boolean): Flow<Result<Unit>>

    /**
     * Gets or creates a 1-on-1 direct thread with another user.
     * If a thread already exists between the two users, returns it.
     * If not, creates a new thread and returns it.
     * Verifies mutual contact status before creating.
     *
     * @param otherUserId The UID of the other user.
     * @return A flow emitting the [ChatThreadModel] or an error.
     */
    fun getOrCreateDirectThread(otherUserId: String): Flow<Result<ChatThreadModel>>
}
