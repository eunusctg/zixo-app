package com.zixo.app.data.repository

import android.content.Context
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.data.local.room.ZixoDatabase
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.AppSettingsState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
        try { preferencesDataStore.setThemeMode(mode) }
        catch (e: Exception) { Timber.e(e, "Failed to update theme mode") }
    }

    // ── Security ──────────────────────────────────────────────────────────────

    override suspend fun updateSecurityNotifications(enabled: Boolean) {
        try { preferencesDataStore.setSecurityNotificationsEnabled(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update security notifications") }
    }

    override suspend fun updateTwoStep(enabled: Boolean) {
        try { preferencesDataStore.setTwoStepEnabled(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update two-step verification") }
    }

    // ── Privacy ───────────────────────────────────────────────────────────────

    override suspend fun updateLastSeenVisibility(option: VisibilityOption) {
        try { preferencesDataStore.setLastSeenVisibility(option) }
        catch (e: Exception) { Timber.e(e, "Failed to update last seen visibility") }
    }

    override suspend fun updateProfilePhotoVisibility(option: VisibilityOption) {
        try { preferencesDataStore.setProfilePhotoVisibility(option) }
        catch (e: Exception) { Timber.e(e, "Failed to update profile photo visibility") }
    }

    override suspend fun updateAboutVisibility(option: VisibilityOption) {
        try { preferencesDataStore.setAboutVisibility(option) }
        catch (e: Exception) { Timber.e(e, "Failed to update about visibility") }
    }

    override suspend fun updateStatusPrivacy(option: StatusPrivacyOption) {
        try { preferencesDataStore.setStatusPrivacy(option) }
        catch (e: Exception) { Timber.e(e, "Failed to update status privacy") }
    }

    override suspend fun updateReadReceipts(enabled: Boolean) {
        try { preferencesDataStore.setReadReceiptsEnabled(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update read receipts") }
    }

    // ── Ephemeral ─────────────────────────────────────────────────────────────

    override suspend fun updateEphemeralTimer(seconds: Int) {
        try { preferencesDataStore.setEphemeralDestructTimer(seconds) }
        catch (e: Exception) { Timber.e(e, "Failed to update ephemeral timer") }
    }

    // ── Security & Advanced ───────────────────────────────────────────────────

    override suspend fun updateScreenLock(enabled: Boolean) {
        try { preferencesDataStore.setScreenLockEnabled(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update screen lock") }
    }

    override suspend fun updateProtectIpInCalls(enabled: Boolean) {
        try { preferencesDataStore.setProtectIpInCalls(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update protect IP in calls") }
    }

    override suspend fun updateDisableLinkPreviews(enabled: Boolean) {
        try { preferencesDataStore.setDisableLinkPreviews(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update disable link previews") }
    }

    // ── Premium / Freemium Billing ───────────────────────────────────────────

    override suspend fun updateIncomingPstnEnabled(enabled: Boolean) {
        try {
            preferencesDataStore.setIncomingPstnEnabled(enabled)
            // Update Firestore user profile field
            val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
            firestoreService.updateUserProfile(uid, mapOf("isIncomingPstnEnabled" to enabled))
                .flowOn(Dispatchers.IO)
                .collect {}
        } catch (e: Exception) {
            Timber.e(e, "Failed to update incoming PSTN setting")
        }
    }

    override suspend fun isPremiumSubscriber(): Boolean {
        return try {
            val uid = firebaseAuthService.getCurrentUser()?.uid ?: return false
            val profile = firestoreService.getUserProfile(uid).flowOn(Dispatchers.IO).first()
            // Check the premium flag from the User model
            profile?.let { false } ?: false // Default to false until Firestore model is updated
        } catch (e: Exception) {
            Timber.e(e, "Failed to check premium subscriber status")
            false
        }
    }

    // ── Chat Behavior ─────────────────────────────────────────────────────────

    override suspend fun updateEnterIsSend(enabled: Boolean) {
        try { preferencesDataStore.setEnterIsSend(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update enter-is-send") }
    }

    override suspend fun updateMediaVisibility(enabled: Boolean) {
        try { preferencesDataStore.setMediaVisibilityEnabled(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update media visibility") }
    }

    override suspend fun updateFontSizeScale(scale: Float) {
        try { preferencesDataStore.setFontSizeScale(scale) }
        catch (e: Exception) { Timber.e(e, "Failed to update font size scale") }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    override suspend fun updateConversationTones(enabled: Boolean) {
        try { preferencesDataStore.setConversationTonesEnabled(enabled) }
        catch (e: Exception) { Timber.e(e, "Failed to update conversation tones") }
    }

    override suspend fun updateMessageNotificationTone(uri: String) {
        try { preferencesDataStore.setMessageNotificationToneUri(uri) }
        catch (e: Exception) { Timber.e(e, "Failed to update message notification tone") }
    }

    override suspend fun updateGroupNotificationTone(uri: String) {
        try { preferencesDataStore.setGroupNotificationToneUri(uri) }
        catch (e: Exception) { Timber.e(e, "Failed to update group notification tone") }
    }

    override suspend fun updateCallRingtone(uri: String) {
        try { preferencesDataStore.setCallRingtoneUri(uri) }
        catch (e: Exception) { Timber.e(e, "Failed to update call ringtone") }
    }

    override suspend fun updateVideoCallRingtone(uri: String) {
        try { preferencesDataStore.setVideoCallRingtoneUri(uri) }
        catch (e: Exception) { Timber.e(e, "Failed to update video call ringtone") }
    }

    override suspend fun updateVibrationPattern(pattern: VibrationOption) {
        try { preferencesDataStore.setVibrationPattern(pattern) }
        catch (e: Exception) { Timber.e(e, "Failed to update vibration pattern") }
    }

    // ── Media Auto-Download ───────────────────────────────────────────────────

    override suspend fun updateAutoDownloadMobile(types: Set<MediaType>) {
        try { preferencesDataStore.setAutoDownloadMobile(types) }
        catch (e: Exception) { Timber.e(e, "Failed to update auto-download mobile") }
    }

    override suspend fun updateAutoDownloadWifi(types: Set<MediaType>) {
        try { preferencesDataStore.setAutoDownloadWifi(types) }
        catch (e: Exception) { Timber.e(e, "Failed to update auto-download Wi-Fi") }
    }

    override suspend fun updateAutoDownloadRoaming(types: Set<MediaType>) {
        try { preferencesDataStore.setAutoDownloadRoaming(types) }
        catch (e: Exception) { Timber.e(e, "Failed to update auto-download roaming") }
    }

    // ── Media Upload Quality ──────────────────────────────────────────────────

    override suspend fun updateMediaUploadQuality(quality: UploadQuality) {
        try { preferencesDataStore.setMediaUploadQuality(quality) }
        catch (e: Exception) { Timber.e(e, "Failed to update media upload quality") }
    }

    // ── Profile Mutations ─────────────────────────────────────────────────────

    override suspend fun updateProfileDisplayName(name: String) {
        withContext(Dispatchers.IO) {
            try {
                val uid = firebaseAuthService.getCurrentUser()?.uid
                    ?: throw IllegalStateException("No authenticated user")
                firestoreService.updateUserProfile(uid, mapOf("displayName" to name))
            } catch (e: Exception) { Timber.e(e, "Failed to update display name") }
        }
    }

    override suspend fun updateProfileBio(bio: String) {
        withContext(Dispatchers.IO) {
            try {
                val uid = firebaseAuthService.getCurrentUser()?.uid
                    ?: throw IllegalStateException("No authenticated user")
                firestoreService.updateUserProfile(uid, mapOf("bio" to bio))
            } catch (e: Exception) { Timber.e(e, "Failed to update bio") }
        }
    }

    override suspend fun updateProfileAvatarUrl(url: String) {
        withContext(Dispatchers.IO) {
            try {
                val uid = firebaseAuthService.getCurrentUser()?.uid
                    ?: throw IllegalStateException("No authenticated user")
                firestoreService.updateUserProfile(uid, mapOf("photoUrl" to url))
            } catch (e: Exception) { Timber.e(e, "Failed to update avatar URL") }
        }
    }

    // ── Storage Analytics ─────────────────────────────────────────────────────

    override fun getStorageBreakdown(): Flow<StorageBreakdown> = flow {
        val filesDir = context.filesDir
        val dirs = mapOf(
            "calls" to File(filesDir, "calls"),
            "messages" to File(filesDir, "messages"),
            "status" to File(filesDir, "status_uploads"),
            "cloudSync" to File(filesDir, "cloud_sync"),
            "media" to File(filesDir, "media")
        )

        val sizes = dirs.mapValues { (_, dir) -> calculateDirectorySize(dir) }
        val dbSize = calculateDatabaseSize()
        val totalBytes = sizes.values.sum() + dbSize

        emit(StorageBreakdown(
            totalBytes = totalBytes,
            callsBytes = sizes["calls"] ?: 0L,
            messagesBytes = sizes["messages"] ?: 0L,
            statusUploadsBytes = sizes["status"] ?: 0L,
            cloudSyncBytes = sizes["cloudSync"] ?: 0L,
            mediaBytes = sizes["media"] ?: 0L
        ))
    }.flowOn(Dispatchers.IO)

    // ── Maintenance ───────────────────────────────────────────────────────────

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        try { deleteRecursive(context.cacheDir) }
        catch (e: Exception) { Timber.e(e, "Failed to clear cache") }
    }

    // ── Account Lifecycle ─────────────────────────────────────────────────────

    override suspend fun requestAccountInfo(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuthService.getCurrentUser()
                ?: return@withContext Result.failure(IllegalStateException("No authenticated user"))

            val report = buildString {
                appendLine("═══════════════════════════════════════════")
                appendLine("         ZIXO ACCOUNT INFORMATION REPORT")
                appendLine("═══════════════════════════════════════════")
                appendLine()
                appendLine("Generated : ${System.currentTimeMillis()}")
                appendLine("User ID   : ${user.uid}")
                appendLine("Name      : ${user.displayName ?: "Unknown"}")
                appendLine("Email     : ${user.email ?: "N/A"}")
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

            firestoreService.deleteUserData(user.uid)
            firebaseAuthService.deleteAccount()
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
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursive(it) }
        file.delete()
    }

    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        var size = 0L
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
            }
        } else {
            size = directory.length()
        }
        return size
    }

    private fun calculateDatabaseSize(): Long {
        val dbFile = context.getDatabasePath(ZixoDatabase.DATABASE_NAME)
        var totalSize = 0L
        if (dbFile.exists()) totalSize += dbFile.length()
        File(dbFile.parent, "${dbFile.name}-wal").takeIf { it.exists() }?.let { totalSize += it.length() }
        File(dbFile.parent, "${dbFile.name}-shm").takeIf { it.exists() }?.let { totalSize += it.length() }
        return totalSize
    }
}
