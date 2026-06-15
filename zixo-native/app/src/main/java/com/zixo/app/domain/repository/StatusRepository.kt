package com.zixo.app.domain.repository

import com.zixo.app.domain.model.MyStatusState
import com.zixo.app.domain.model.StatusGroupModel
import com.zixo.app.domain.model.StatusModel
import kotlinx.coroutines.flow.Flow

/**
 * Status Repository Interface — Zero-Trust Status Privacy
 *
 * Statuses are ONLY delivered to verified mutual contacts. Non-contacts
 * cannot see, preview, or interact with any status content. The repository
 * enforces this at the data boundary before any status reaches the UI.
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
 */
interface StatusRepository {

    /**
     * Gets the current user's own statuses.
     *
     * @return A flow emitting the current [MyStatusState].
     */
    fun getMyStatuses(): Flow<MyStatusState>

    /**
     * Gets the status feed for the current user.
     * Returns statuses grouped by sender, containing only statuses
     * from verified mutual contacts. Statuses older than 24 hours
     * are automatically filtered out.
     *
     * @return A flow emitting the current list of [StatusGroupModel] entries.
     */
    fun getContactStatuses(): Flow<List<StatusGroupModel>>

    /**
     * Observes the contact status feed in real-time via Firestore addSnapshotListener.
     * Only statuses from verified mutual contacts are delivered.
     * Auto-expiration logic filters out statuses older than 24 hours.
     *
     * @return A flow emitting the current list of [StatusGroupModel] entries.
     */
    fun observeContactStatusesRealtime(): Flow<List<StatusGroupModel>>

    /**
     * Posts a new status update.
     * The status is delivered only to verified mutual contacts.
     * Media uploads go to Firebase Storage.
     *
     * @param status The [StatusModel] to post.
     * @return A flow emitting the posted [StatusModel] or an error.
     */
    fun postStatus(status: StatusModel): Flow<Result<StatusModel>>

    /**
     * Deletes a status posted by the current user.
     *
     * @param statusId The ID of the status to delete.
     * @return A flow emitting Result success or failure.
     */
    fun deleteStatus(statusId: String): Flow<Result<Unit>>

    /**
     * Marks a status as viewed by the current user.
     *
     * @param statusId The ID of the status to mark as viewed.
     * @return A flow emitting Result success or failure.
     */
    fun markStatusViewed(statusId: String): Flow<Result<Unit>>

    /**
     * Adds a reaction to a status.
     * Only mutual contacts can react to each other's statuses.
     *
     * @param statusId The ID of the status to react to.
     * @param emoji The emoji character to react with.
     * @return A flow emitting Result success or failure.
     */
    fun reactToStatus(statusId: String, emoji: String): Flow<Result<Unit>>
}
