package com.zixo.app.domain.repository

import com.zixo.app.domain.model.MyStatusState
import com.zixo.app.domain.model.StatusGroupModel
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
     * Observes the status feed for the current user.
     *
     * Returns statuses grouped by sender ([StatusGroupModel]), containing
     * only statuses from verified mutual contacts. Statuses older than
     * 24 hours are automatically filtered out.
     *
     * @return A flow emitting the current list of [StatusGroupModel] entries.
     */
    fun observeStatusFeed(): Flow<List<StatusGroupModel>>

    /**
     * Observes the current user's own statuses.
     *
     * @return A flow emitting the current [MyStatusState].
     */
    fun observeMyStatuses(): Flow<MyStatusState>

    /**
     * Posts a text status update.
     *
     * @param text The text content of the status.
     * @param backgroundColor Optional hex color for the background.
     */
    suspend fun postTextStatus(text: String, backgroundColor: String?)

    /**
     * Posts an image status update.
     *
     * @param localFilePath The local file path of the image to upload.
     * @param caption Optional caption for the image.
     */
    suspend fun postImageStatus(localFilePath: String, caption: String?)

    /**
     * Posts a video status update.
     *
     * @param localFilePath The local file path of the video to upload.
     * @param caption Optional caption for the video.
     */
    suspend fun postVideoStatus(localFilePath: String, caption: String?)

    /**
     * Marks a status as viewed by the current user.
     *
     * @param statusId The ID of the status to mark as viewed.
     */
    suspend fun viewStatus(statusId: String)

    /**
     * Adds a reaction to a status.
     *
     * @param statusId The ID of the status to react to.
     * @param emoji The emoji character to react with.
     */
    suspend fun addStatusReaction(statusId: String, emoji: String)

    /**
     * Deletes a status posted by the current user.
     *
     * @param statusId The ID of the status to delete.
     */
    suspend fun deleteStatus(statusId: String)
}
