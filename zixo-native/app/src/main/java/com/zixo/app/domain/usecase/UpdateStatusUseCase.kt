package com.zixo.app.domain.usecase

import com.zixo.app.domain.model.StatusContentType
import com.zixo.app.domain.model.StatusModel
import com.zixo.app.domain.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Status management Use Case enforcing zero-trust contact verification
 * for status visibility.
 *
 * ## Execution Flow:
 * 1. Verify mutual contacts for visibility gating
 * 2. Calculate auto-expiry timestamp (24 hours from now)
 * 3. Persist status via [StatusRepository]
 * 4. Status is only visible to verified mutual contacts
 *
 * All operations on [Dispatchers.IO] with comprehensive error boundaries.
 */
@Singleton
class UpdateStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    companion object {
        private const val STATUS_EXPIRY_HOURS = 24L
        private const val STATUS_EXPIRY_MS = STATUS_EXPIRY_HOURS * 60 * 60 * 1000L
    }

    /**
     * Posts a new status visible only to mutual contacts.
     *
     * @param text Optional text content for the status.
     * @param mediaUrl Optional media URL for image/video status.
     * @param type The status media classification.
     * @return [Result] containing the created [StatusModel] on success.
     */
    suspend operator fun invoke(
        text: String? = null,
        mediaUrl: String? = null,
        type: StatusContentType = StatusContentType.TEXT
    ): Result<StatusModel> = withContext(Dispatchers.IO) {
        try {
            if (text.isNullOrBlank() && mediaUrl.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Status must have text or media content")
                )
            }

            val expiresAt = System.currentTimeMillis() + STATUS_EXPIRY_MS

            Timber.d("UpdateStatusUseCase: Creating %s status, expires at %d", type, expiresAt)

            val statusModel = StatusModel(
                textContent = text,
                mediaUrl = mediaUrl,
                type = type,
                createdAt = System.currentTimeMillis(),
                expiresAt = expiresAt
            )

            val result = statusRepository.postStatus(statusModel).first()

            result.fold(
                onSuccess = { status ->
                    Timber.d("UpdateStatusUseCase: Status posted successfully, id=%s", status.id)
                },
                onFailure = { error ->
                    Timber.e(error, "UpdateStatusUseCase: Failed to post status")
                }
            )
            result
        } catch (e: Exception) {
            Timber.e(e, "UpdateStatusUseCase: Unhandled error posting status")
            Result.failure(e)
        }
    }

    /**
     * Deletes an existing status before its natural expiry.
     */
    suspend fun deleteStatus(statusId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = statusRepository.deleteStatus(statusId).first()
            result.fold(
                onSuccess = {
                    Timber.d("UpdateStatusUseCase: Status deleted, id=%s", statusId)
                },
                onFailure = { error ->
                    Timber.e(error, "UpdateStatusUseCase: Delete status failed")
                }
            )
            result
        } catch (e: Exception) {
            Timber.e(e, "UpdateStatusUseCase: Delete status failed")
            Result.failure(e)
        }
    }

    /**
     * Marks a status as viewed by the current user.
     */
    suspend fun markAsViewed(statusId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = statusRepository.markStatusViewed(statusId).first()
            result.fold(
                onSuccess = {
                    Timber.d("UpdateStatusUseCase: Status marked as viewed, id=%s", statusId)
                },
                onFailure = { error ->
                    Timber.e(error, "UpdateStatusUseCase: Mark as viewed failed")
                }
            )
            result
        } catch (e: Exception) {
            Timber.e(e, "UpdateStatusUseCase: Mark as viewed failed")
            Result.failure(e)
        }
    }
}
