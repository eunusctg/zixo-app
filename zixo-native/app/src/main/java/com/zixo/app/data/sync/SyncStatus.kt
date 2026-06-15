package com.zixo.app.data.sync

/**
 * Sync state tracking model for the WorkManager offline-first sync engine.
 * Observed by UI to display sync progress, errors, and completion status.
 */
sealed class SyncStatus {

    /** No sync operation is currently active. */
    data object Idle : SyncStatus()

    /** A sync operation is in progress with estimated completion percentage. */
    data class Syncing(val progress: Float) : SyncStatus()

    /** Sync completed successfully with a timestamp. */
    data class Completed(val syncedAt: Long) : SyncStatus()

    /** Sync encountered a non-recoverable error. */
    data class Error(val message: String) : SyncStatus()

    /** One or more conflicts were resolved using server-wins strategy. */
    data class ConflictResolved(val count: Int) : SyncStatus()

    fun isSyncing(): Boolean = this is Syncing
    fun isError(): Boolean = this is Error

    /**
     * Metadata about the last successful sync for diagnostic display.
     */
    data class SyncMetadata(
        val lastSyncTimestamp: Long,
        val entitiesSynced: Int,
        val conflictsResolved: Int,
        val lastErrorMessage: String?
    )
}
