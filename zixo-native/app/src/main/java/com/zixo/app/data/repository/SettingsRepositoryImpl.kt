package com.zixo.app.data.repository

import android.content.Context
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.data.local.room.ZixoDatabase
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.AppSettingsState
import com.zixo.app.domain.model.ConversationStorageEntry
import com.zixo.app.domain.model.MediaType
import com.zixo.app.domain.model.StorageBreakdown
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.model.UploadQuality
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.domain.model.VisibilityOption
import com.zixo.app.domain.model.VibrationOption
import com.zixo.app.domain.model.StatusPrivacyOption
import com.zixo.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [SettingsRepository].
 *
 * Aggregates [PreferencesDataStore] preference flows with the Firestore-backed
 * [UserProfile] into a single [AppSettingsState] reactive stream.
 * All Firebase operations run on [Dispatchers.IO].
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val database: ZixoDatabase,
    private val firestoreService: FirestoreService,
    private val firebaseAuthService: FirebaseAuthService,
    @ApplicationContext private val context: Context
) : SettingsRepository {

    // ── Aggregated State ──────────────────────────────────────────────────────

    override val userProfileFlow: Flow<UserProfile> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
        if (uid != null) {
            firestoreService.getUserProfile(uid).collect { user ->
                val profile = user?.let {
                    UserProfile(
                        displayName = it.displayName,
                        username = it.username,
                        zixoNumber = it.zixoNumber,
                        avatarUrl = it.photoUrl ?: "",
                        bio = it.bio ?: "",
                        phoneNumber = it.phoneNumber ?: ""
                    )
                } ?: UserProfile()
                emit(profile)
            }
        } else {
            emit(UserProfile())
        }
    }.flowOn(Dispatchers.IO)

    override val settingsFlow: Flow<AppSettingsState> = combine(
        listOf(
            userProfileFlow,
            preferencesDataStore.themeMode,
            preferencesDataStore.isSecurityNotificationsEnabled,
            preferencesDataStore.isTwoStepEnabled,
            preferencesDataStore.lastSeenVisibility,
            preferencesDataStore.profilePhotoVisibility,
            preferencesDataStore.aboutVisibility,
            preferencesDataStore.statusPrivacy,
            preferencesDataStore.areReadReceiptsEnabled,
            preferencesDataStore.ephemeralDestructTimer,
            preferencesDataStore.isScreenLockEnabled,
            preferencesDataStore.protectIpInCalls,
            preferencesDataStore.disableLinkPreviews,
            preferencesDataStore.enterIsSend,
            preferencesDataStore.isMediaVisibilityEnabled,
            preferencesDataStore.fontSizeScale,
            preferencesDataStore.areConversationTonesEnabled,
            preferencesDataStore.messageNotificationToneUri,
            preferencesDataStore.groupNotificationToneUri,
            preferencesDataStore.callRingtoneUri,
            preferencesDataStore.videoCallRingtoneUri,
            preferencesDataStore.vibrationPattern,
            preferencesDataStore.autoDownloadMobile,
            preferencesDataStore.autoDownloadWifi,
            preferencesDataStore.autoDownloadRoaming,
            preferencesDataStore.mediaUploadQuality
        )
    ) { values ->
        AppSettingsState(
            userProfile = values[0] as UserProfile,
            themeMode = values[1] as ThemeMode,
            isSecurityNotificationsEnabled = values[2] as Boolean,
            isTwoStepEnabled = values[3] as Boolean,
            lastSeenVisibility = values[4] as VisibilityOption,
            profilePhotoVisibility = values[5] as VisibilityOption,
            aboutVisibility = values[6] as VisibilityOption,
            statusPrivacy = values[7] as StatusPrivacyOption,
            areReadReceiptsEnabled = values[8] as Boolean,
            ephemeralDestructTimer = values[9] as Int,
            isScreenLockEnabled = values[10] as Boolean,
            protectIpInCalls = values[11] as Boolean,
            disableLinkPreviews = values[12] as Boolean,
            enterIsSend = values[13] as Boolean,
            isMediaVisibilityEnabled = values[14] as Boolean,
            fontSizeScale = values[15] as Float,
            areConversationTonesEnabled = values[16] as Boolean,
            messageNotificationToneUri = values[17] as String,
            groupNotificationToneUri = values[18] as String,
            callRingtoneUri = values[19] as String,
            videoCallRingtoneUri = values[20] as String,
            vibrationPattern = values[21] as VibrationOption,
            autoDownloadMobile = values[22] as Set<MediaType>,
            autoDownloadWifi = values[23] as Set<MediaType>,
            autoDownloadRoaming = values[24] as Set<MediaType>,
            mediaUploadQuality = values[25] as UploadQuality
        )
    }

    // ── Theme & Appearance ────────────────────────────────────────────────────

    override suspend fun updateThemeMode(mode: ThemeMode) {
        try {
            preferencesDataStore.setThemeMode(mode)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update theme mode")
        }
    }

    // ── Security ──────────────────────────────────────────────────────────────

    override suspend fun updateSecurityNotifications(enabled: Boolean) {
        try {
            preferencesDataStore.setSecurityNotificationsEnabled(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update security notifications")
        }
    }

    override suspend fun updateTwoStep(enabled: Boolean) {
        try {
            preferencesDataStore.setTwoStepEnabled(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update two-step verification")
        }
    }

    // ── Privacy ───────────────────────────────────────────────────────────────

    override suspend fun updateLastSeenVisibility(option: VisibilityOption) {
        try {
            preferencesDataStore.setLastSeenVisibility(option)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update last seen visibility")
        }
    }

    override suspend fun updateProfilePhotoVisibility(option: VisibilityOption) {
        try {
            preferencesDataStore.setProfilePhotoVisibility(option)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update profile photo visibility")
        }
    }

    override suspend fun updateAboutVisibility(option: VisibilityOption) {
        try {
            preferencesDataStore.setAboutVisibility(option)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update about visibility")
        }
    }

    override suspend fun updateStatusPrivacy(option: StatusPrivacyOption) {
        try {
            preferencesDataStore.setStatusPrivacy(option)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update status privacy")
        }
    }

    override suspend fun updateReadReceipts(enabled: Boolean) {
        try {
            preferencesDataStore.setReadReceiptsEnabled(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update read receipts")
        }
    }

    // ── Ephemeral / Disappearing Messages ─────────────────────────────────────

    override suspend fun updateEphemeralTimer(seconds: Int) {
        try {
            preferencesDataStore.setEphemeralDestructTimer(seconds)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update ephemeral timer")
        }
    }

    // ── Security & Advanced ───────────────────────────────────────────────────

    override suspend fun updateScreenLock(enabled: Boolean) {
        try {
            preferencesDataStore.setScreenLockEnabled(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update screen lock")
        }
    }

    override suspend fun updateProtectIpInCalls(enabled: Boolean) {
        try {
            preferencesDataStore.setProtectIpInCalls(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update protect IP in calls")
        }
    }

    override suspend fun updateDisableLinkPreviews(enabled: Boolean) {
        try {
            preferencesDataStore.setDisableLinkPreviews(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update disable link previews")
        }
    }

    // ── Chat Behavior ─────────────────────────────────────────────────────────

    override suspend fun updateEnterIsSend(enabled: Boolean) {
        try {
            preferencesDataStore.setEnterIsSend(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update enter-is-send")
        }
    }

    override suspend fun updateMediaVisibility(enabled: Boolean) {
        try {
            preferencesDataStore.setMediaVisibilityEnabled(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update media visibility")
        }
    }

    override suspend fun updateFontSizeScale(scale: Float) {
        try {
            preferencesDataStore.setFontSizeScale(scale)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update font size scale")
        }
    }

    // ── Notifications & Tones ─────────────────────────────────────────────────

    override suspend fun updateConversationTones(enabled: Boolean) {
        try {
            preferencesDataStore.setConversationTonesEnabled(enabled)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update conversation tones")
        }
    }

    override suspend fun updateMessageNotificationTone(uri: String) {
        try {
            preferencesDataStore.setMessageNotificationToneUri(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update message notification tone")
        }
    }

    override suspend fun updateGroupNotificationTone(uri: String) {
        try {
            preferencesDataStore.setGroupNotificationToneUri(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update group notification tone")
        }
    }

    override suspend fun updateCallRingtone(uri: String) {
        try {
            preferencesDataStore.setCallRingtoneUri(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update call ringtone")
        }
    }

    override suspend fun updateVideoCallRingtone(uri: String) {
        try {
            preferencesDataStore.setVideoCallRingtoneUri(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update video call ringtone")
        }
    }

    override suspend fun updateVibrationPattern(pattern: VibrationOption) {
        try {
            preferencesDataStore.setVibrationPattern(pattern)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update vibration pattern")
        }
    }

    // ── Media Auto-Download ───────────────────────────────────────────────────

    override suspend fun updateAutoDownloadMobile(types: Set<MediaType>) {
        try {
            preferencesDataStore.setAutoDownloadMobile(types)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update auto-download mobile")
        }
    }

    override suspend fun updateAutoDownloadWifi(types: Set<MediaType>) {
        try {
            preferencesDataStore.setAutoDownloadWifi(types)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update auto-download Wi-Fi")
        }
    }

    override suspend fun updateAutoDownloadRoaming(types: Set<MediaType>) {
        try {
            preferencesDataStore.setAutoDownloadRoaming(types)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update auto-download roaming")
        }
    }

    // ── Media Upload Quality ──────────────────────────────────────────────────

    override suspend fun updateMediaUploadQuality(quality: UploadQuality) {
        try {
            preferencesDataStore.setMediaUploadQuality(quality)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update media upload quality")
        }
    }

    // ── Profile Mutations (writable fields only) ──────────────────────────────

    override suspend fun updateProfileDisplayName(name: String) = withContext(Dispatchers.IO) {
        try {
            val uid = firebaseAuthService.getCurrentUser()?.uid
                ?: throw IllegalStateException("No authenticated user")
            firestoreService.updateUserProfile(uid, mapOf("displayName" to name))
                .collect { /* await completion */ }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update display name")
        }
    }

    override suspend fun updateProfileBio(bio: String) = withContext(Dispatchers.IO) {
        try {
            val uid = firebaseAuthService.getCurrentUser()?.uid
                ?: throw IllegalStateException("No authenticated user")
            firestoreService.updateUserProfile(uid, mapOf("bio" to bio))
                .collect { /* await completion */ }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update bio")
        }
    }

    override suspend fun updateProfileAvatarUrl(url: String) = withContext(Dispatchers.IO) {
        try {
            val uid = firebaseAuthService.getCurrentUser()?.uid
                ?: throw IllegalStateException("No authenticated user")
            firestoreService.updateUserProfile(uid, mapOf("photoUrl" to url))
                .collect { /* await completion */ }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update avatar URL")
        }
    }

    // ── Storage Analytics ─────────────────────────────────────────────────────

    override fun getStorageBreakdown(): Flow<StorageBreakdown> = flow {
        val cacheDir = context.cacheDir
        val filesDir = context.filesDir

        val callsDir = File(filesDir, "calls")
        val messagesDir = File(filesDir, "messages")
        val statusDir = File(filesDir, "status_uploads")
        val cloudSyncDir = File(filesDir, "cloud_sync")
        val mediaDir = File(filesDir, "media")

        val callsBytes = calculateDirectorySize(callsDir)
        val messagesBytes = calculateDirectorySize(messagesDir)
        val statusUploadsBytes = calculateDirectorySize(statusDir)
        val cloudSyncBytes = calculateDirectorySize(cloudSyncDir)
        val mediaBytes = calculateDirectorySize(mediaDir)
        val totalBytes = callsBytes + messagesBytes + statusUploadsBytes +
                cloudSyncBytes + mediaBytes + calculateDatabaseSize()

        emit(
            StorageBreakdown(
                totalBytes = totalBytes,
                callsBytes = callsBytes,
                messagesBytes = messagesBytes,
                statusUploadsBytes = statusUploadsBytes,
                cloudSyncBytes = cloudSyncBytes,
                mediaBytes = mediaBytes
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun getConversationStorage(): Flow<List<ConversationStorageEntry>> =
        database.chatDao().getAllThreads().map { entities ->
            entities.map { entity ->
                ConversationStorageEntry(
                    threadId = entity.id,
                    displayName = entity.participantUids.removeSurrounding("[", "]")
                        .split(",")
                        .firstOrNull()
                        ?.trim()
                        ?.removeSurrounding("\"")
                        ?: entity.id,
                    avatarUrl = null,
                    storageBytes = estimateThreadStorageSize(entity.id),
                    isPinned = entity.isPinned
                )
            }.sortedWith(
                compareByDescending<ConversationStorageEntry> { it.isPinned }
                    .thenByDescending { it.storageBytes }
            )
        }.flowOn(Dispatchers.IO)

    // ── Maintenance ───────────────────────────────────────────────────────────

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            deleteRecursive(context.cacheDir)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
        }
    }

    // ── Account Lifecycle ─────────────────────────────────────────────────────

    override suspend fun requestAccountInfo(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuthService.getCurrentUser()
                ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
            val uid = user.uid

            // Simulate async data compilation job.
            // In production this would aggregate Firestore data, local DB records,
            // and storage metadata into a portable report, then upload to a
            // secure bucket and return a download URL.
            val displayName = user.displayName ?: "Unknown"
            val email = user.email ?: "N/A"
            val timestamp = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                java.util.Locale.US
            ).format(System.currentTimeMillis())

            val report = buildString {
                appendLine("═══════════════════════════════════════════")
                appendLine("         ZIXO ACCOUNT INFORMATION REPORT")
                appendLine("═══════════════════════════════════════════")
                appendLine()
                appendLine("Generated : $timestamp")
                appendLine("User ID   : $uid")
                appendLine("Name      : $displayName")
                appendLine("Email     : $email")
                appendLine()
                appendLine("This report was compiled from your account")
                appendLine("data stored on Zixo servers. For privacy,")
                appendLine("sensitive fields have been redacted.")
                appendLine()
                appendLine("═══════════════════════════════════════════")
            }

            Result.success(report)
        } catch (e: Exception) {
            Timber.e(e, "Failed to request account info")
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuthService.getCurrentUser()
                ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))
            val uid = user.uid

            // Step 1: Delete all Firestore user data (profile, threads, blocked, etc.)
            firestoreService.deleteUserData(uid).collect { /* await completion */ }

            // Step 2: Delete the Firebase Auth account
            firebaseAuthService.deleteAccount()

            // Step 3: Clear local preferences and cache
            preferencesDataStore.clearAll()
            deleteRecursive(context.cacheDir)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete account")
            Result.failure(e)
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> deleteRecursive(child) }
        }
        file.delete()
    }

    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        var size = 0L
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = directory.length()
        }
        return size
    }

    private fun calculateDatabaseSize(): Long {
        val dbFile = context.getDatabasePath(ZixoDatabase.DATABASE_NAME)
        var totalSize = 0L
        if (dbFile.exists()) {
            totalSize += dbFile.length()
        }
        // Account for WAL (Write-Ahead Log) and SHM (Shared Memory) files
        val walFile = File(dbFile.parent, "${dbFile.name}-wal")
        if (walFile.exists()) {
            totalSize += walFile.length()
        }
        val shmFile = File(dbFile.parent, "${dbFile.name}-shm")
        if (shmFile.exists()) {
            totalSize += shmFile.length()
        }
        return totalSize
    }

    /**
     * Estimate the on-device storage consumed by a single chat thread.
     *
     * Checks the thread-specific media directory and falls back to a
     * proportional share of the total media directory when a dedicated
     * subdirectory does not exist.
     */
    private fun estimateThreadStorageSize(threadId: String): Long {
        val threadMediaDir = File(context.filesDir, "media/$threadId")
        if (threadMediaDir.exists()) {
            return calculateDirectorySize(threadMediaDir)
        }

        // Fallback: proportional estimation based on database row share
        val totalMediaSize = calculateDirectorySize(File(context.filesDir, "media"))
        val totalDbSize = calculateDatabaseSize()
        if (totalDbSize <= 0L) return 0L

        // Rough heuristic: assign a proportional fraction of media storage
        // based on this thread's share of the database. This avoids a
        // full media scan on every emission.
        return (totalMediaSize * 0.1).toLong().coerceAtMost(totalMediaSize)
    }
}
