package com.zixo.app.data.repository

import android.content.Context
import com.zixo.app.data.local.datastore.UserPreferences
import com.zixo.app.data.local.room.ZixoDatabase
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.AudioProfile
import com.zixo.app.domain.model.AutoDownloadMedia
import com.zixo.app.domain.model.DefaultCallType
import com.zixo.app.domain.model.FontSize
import com.zixo.app.domain.model.LastSeenVisibility
import com.zixo.app.domain.model.MediaCompressionProfile
import com.zixo.app.domain.model.SelfDestructTimer
import com.zixo.app.domain.model.Session
import com.zixo.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class StorageInfo(
    val totalMB: Double,
    val cacheMB: Double,
    val mediaMB: Double,
    val databaseMB: Double
)

@Singleton
class SettingsRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val database: ZixoDatabase,
    private val firestoreService: FirestoreService,
    private val firebaseAuthService: FirebaseAuthService,
    @ApplicationContext private val context: Context
) {

    // ── Theme & Appearance ──────────────────────────────────────────────────

    val themeMode: Flow<ThemeMode> = userPreferences.themeMode

    suspend fun setThemeMode(mode: ThemeMode) = userPreferences.setThemeMode(mode)

    val fontSize: Flow<FontSize> = userPreferences.fontSize

    suspend fun setFontSize(size: FontSize) = userPreferences.setFontSize(size)

    val chatWallpaper: Flow<String> = userPreferences.chatWallpaper

    suspend fun setChatWallpaper(assetPath: String) = userPreferences.setChatWallpaper(assetPath)

    // ── Privacy ─────────────────────────────────────────────────────────────

    val lastSeenVisibility: Flow<LastSeenVisibility> = userPreferences.lastSeenVisibility

    suspend fun setLastSeenVisibility(visibility: LastSeenVisibility) =
        userPreferences.setLastSeenVisibility(visibility)

    val onlineStatusEnabled: Flow<Boolean> = userPreferences.onlineStatusEnabled

    suspend fun setOnlineStatusEnabled(enabled: Boolean) =
        userPreferences.setOnlineStatusEnabled(enabled)

    val readReceipts: Flow<Boolean> = userPreferences.readReceiptsEnabled

    suspend fun setReadReceipts(enabled: Boolean) =
        userPreferences.setReadReceiptsEnabled(enabled)

    val screenLockEnabled: Flow<Boolean> = userPreferences.screenLockEnabled

    suspend fun setScreenLockEnabled(enabled: Boolean) =
        userPreferences.setScreenLockEnabled(enabled)

    val messagePreviewEnabled: Flow<Boolean> = userPreferences.messagePreviewEnabled

    suspend fun setMessagePreviewEnabled(enabled: Boolean) =
        userPreferences.setMessagePreviewEnabled(enabled)

    val appSwitcherPrivacyBlur: Flow<Boolean> = userPreferences.appSwitcherPrivacyBlur

    suspend fun setAppSwitcherPrivacyBlur(enabled: Boolean) =
        userPreferences.setAppSwitcherPrivacyBlur(enabled)

    // ── Notifications & DND ─────────────────────────────────────────────────

    val dndEnabled: Flow<Boolean> = userPreferences.dndEnabled

    suspend fun setDndEnabled(enabled: Boolean) = userPreferences.setDndEnabled(enabled)

    val notificationTone: Flow<String> = userPreferences.notificationTone

    suspend fun setNotificationTone(uri: String) = userPreferences.setNotificationTone(uri)

    // ── Media ───────────────────────────────────────────────────────────────

    val autoDownloadMedia: Flow<AutoDownloadMedia> = userPreferences.autoDownloadMedia

    suspend fun setAutoDownloadMedia(mode: AutoDownloadMedia) =
        userPreferences.setAutoDownloadMedia(mode)

    val mediaCompressionProfile: Flow<MediaCompressionProfile> =
        userPreferences.mediaCompressionProfile

    suspend fun setMediaCompressionProfile(profile: MediaCompressionProfile) =
        userPreferences.setMediaCompressionProfile(profile)

    // ── Calling ─────────────────────────────────────────────────────────────

    val defaultCallType: Flow<DefaultCallType> = userPreferences.defaultCallType

    suspend fun setDefaultCallType(callType: DefaultCallType) =
        userPreferences.setDefaultCallType(callType)

    val noiseSuppressionEnabled: Flow<Boolean> = userPreferences.noiseSuppressionEnabled

    suspend fun setNoiseSuppressionEnabled(enabled: Boolean) =
        userPreferences.setNoiseSuppressionEnabled(enabled)

    val liveKitUrl: Flow<String> = userPreferences.liveKitUrl

    suspend fun setLiveKitUrl(url: String) = userPreferences.setLiveKitUrl(url)

    val sipOutboundPrefix: Flow<String> = userPreferences.sipOutboundPrefix

    suspend fun setSipOutboundPrefix(prefix: String) =
        userPreferences.setSipOutboundPrefix(prefix)

    val simulcastEnabled: Flow<Boolean> = userPreferences.simulcastEnabled

    suspend fun setSimulcastEnabled(enabled: Boolean) =
        userPreferences.setSimulcastEnabled(enabled)

    val forceTurnRelay: Flow<Boolean> = userPreferences.forceTurnRelay

    suspend fun setForceTurnRelay(enabled: Boolean) =
        userPreferences.setForceTurnRelay(enabled)

    val audioProfile: Flow<AudioProfile> = userPreferences.audioProfile

    suspend fun setAudioProfile(profile: AudioProfile) =
        userPreferences.setAudioProfile(profile)

    // ── Self-Destruct & Security ────────────────────────────────────────────

    val selfDestructDefault: Flow<SelfDestructTimer> = userPreferences.selfDestructDefault

    suspend fun setSelfDestructDefault(timer: SelfDestructTimer) =
        userPreferences.setSelfDestructDefault(timer)

    // ── Debug ───────────────────────────────────────────────────────────────

    val debugLoggingEnabled: Flow<Boolean> = userPreferences.debugLoggingEnabled

    suspend fun setDebugLoggingEnabled(enabled: Boolean) =
        userPreferences.setDebugLoggingEnabled(enabled)

    // ── Storage & Maintenance ───────────────────────────────────────────────

    /**
     * Clears the application's cache directory, including all cached images,
     * HTTP responses, and temporary files.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        deleteRecursive(context.cacheDir)
    }

    /**
     * Calculates and returns storage usage information for the application,
     * including total, cache, media, and database sizes in megabytes.
     */
    fun getStorageUsage(): Flow<StorageInfo> = flow {
        val cacheDir = context.cacheDir
        val filesDir = context.filesDir

        val cacheSize = calculateDirectorySize(cacheDir)
        val mediaSize = calculateDirectorySize(File(filesDir, "media"))
        val databaseSize = calculateDatabaseSize()
        val appFilesSize = calculateDirectorySize(filesDir)
        val totalSize = cacheSize + appFilesSize + databaseSize

        emit(
            StorageInfo(
                totalMB = bytesToMB(totalSize),
                cacheMB = bytesToMB(cacheSize),
                mediaMB = bytesToMB(mediaSize),
                databaseMB = bytesToMB(databaseSize)
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Compacts the Room database to reclaim unused space and optimize performance.
     */
    suspend fun vacuumDatabase() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL("VACUUM")
    }

    /**
     * Exports application debug logs to a ZIP file.
     * The ZIP contains logcat output, the current database snapshot,
     * and shared preferences for diagnostic purposes.
     *
     * @return A [Flow] that emits the absolute path of the created ZIP file.
     */
    fun exportDebugLogs(): Flow<String> = flow {
        val exportDir = File(context.cacheDir, "debug_exports").apply {
            if (!exists()) mkdirs()
        }

        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(System.currentTimeMillis())

        // Capture logcat output for this process
        val logcatFile = File(exportDir, "logcat_$timestamp.txt")
        val process = Runtime.getRuntime().exec(
            "logcat -d -v threadtime --pid=${android.os.Process.myPid()}"
        )
        process.inputStream.bufferedReader().use { reader ->
            logcatFile.bufferedWriter().use { writer ->
                reader.forEachLine { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
        }

        // Create ZIP containing the logcat, database, and shared preferences
        val zipFile = File(exportDir, "zixo_debug_$timestamp.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            // Add logcat file
            addToZip(zipOut, logcatFile, "logcat.txt")

            // Add database file
            val dbFile = context.getDatabasePath(ZixoDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                addToZip(zipOut, dbFile, "${ZixoDatabase.DATABASE_NAME}.db")
            }

            // Add shared preferences
            val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")
            if (sharedPrefsDir.exists()) {
                sharedPrefsDir.listFiles()?.forEach { prefFile ->
                    if (prefFile.name.endsWith(".xml")) {
                        addToZip(zipOut, prefFile, "shared_prefs/${prefFile.name}")
                    }
                }
            }
        }

        // Clean up temporary logcat file
        logcatFile.delete()

        emit(zipFile.absolutePath)
    }.flowOn(Dispatchers.IO)

    // ── Session Management ──────────────────────────────────────────────────

    /**
     * Revokes an active session, forcing the device associated with
     * [sessionId] to re-authenticate.
     *
     * @param sessionId The ID of the session to revoke.
     * @return A [Flow] that emits [Unit] once the session has been revoked.
     */
    fun revokeSession(sessionId: String): Flow<Unit> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("No authenticated user")
        firestoreService.revokeSession(uid, sessionId).first()
        emit(Unit)
    }

    /**
     * Observes all active sessions for the current user.
     *
     * @return A [Flow] of [Session] lists.
     */
    fun getActiveSessions(): Flow<List<Session>> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
        if (uid != null) {
            firestoreService.observeActiveSessions(uid).collect { sessions ->
                emit(sessions)
            }
        } else {
            emit(emptyList())
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
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

    private fun bytesToMB(bytes: Long): Double {
        return String.format("%.2f", bytes / (1024.0 * 1024.0)).toDouble()
    }

    private fun addToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        zipOut.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                zipOut.write(buffer, 0, bytesRead)
            }
        }
        zipOut.closeEntry()
    }
}
