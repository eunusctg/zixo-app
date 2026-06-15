package com.zixo.app.data.remote.firebase

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zixo.app.data.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-specific sync scheduler using WorkManager.
 *
 * Manages periodic and immediate sync requests, observes sync status,
 * and handles work cancellation. All sync operations use the [SyncWorker]
 * with Hilt injection for dependency resolution.
 */
@Singleton
class FirestoreSyncWorker @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "zixo_periodic_sync"
        private const val IMMEDIATE_SYNC_WORK_NAME = "zixo_immediate_sync"
        private const val SYNC_INTERVAL_MINUTES = 15L
        private const val INITIAL_BACKOFF_SECONDS = 30L
    }

    /**
     * Schedules a periodic sync running every 15 minutes (WorkManager minimum).
     * Uses ExistingPeriodicWorkPolicy.KEEP to avoid duplicating the work.
     * Constraints: Network connected, battery not low.
     */
    fun schedulePeriodicSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            Timber.d("FirestoreSyncWorker: Periodic sync scheduled (every %d min)", SYNC_INTERVAL_MINUTES)
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncWorker: Failed to schedule periodic sync")
        }
    }

    /**
     * Schedules an immediate one-time sync for urgent data reconciliation.
     * Uses expedited execution when possible (falls back to regular on quota).
     */
    fun scheduleImmediateSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf("trigger" to "immediate"))
                .build()

            workManager.enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                immediateRequest
            )

            Timber.d("FirestoreSyncWorker: Immediate sync scheduled")
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncWorker: Failed to schedule immediate sync")
        }
    }

    /**
     * Observes the status of the periodic sync work.
     * Returns a Flow of WorkInfo for UI observation.
     */
    fun observeSyncStatus(): Flow<List<WorkInfo>> = try {
        workManager.getWorkInfosForUniqueWorkFlow(PERIODIC_SYNC_WORK_NAME)
    } catch (e: Exception) {
        Timber.e(e, "FirestoreSyncWorker: Failed to observe sync status")
        kotlinx.coroutines.flow.flowOf(emptyList())
    }

    /**
     * Cancels all pending sync work.
     */
    fun cancelSync() {
        try {
            workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
            Timber.d("FirestoreSyncWorker: All sync work cancelled")
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncWorker: Failed to cancel sync work")
        }
    }
}
