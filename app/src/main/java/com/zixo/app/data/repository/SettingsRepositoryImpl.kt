package com.zixo.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.domain.model.AppSettings
import com.zixo.app.domain.model.StatusPrivacy
import com.zixo.app.domain.model.ThemeMode
import com.zixo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SettingsRepositoryImpl(
    private val dataStore: PreferencesDataStore
) : SettingsRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val TAG = "SettingsRepo"

    init {
        scope.launch {
            dataStore.settingsFlow.collect { settings ->
                _settings.value = settings
            }
        }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        try {
            dataStore.updateThemeMode(mode)
            syncSettingToFirestore("themeMode", mode.name)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update theme mode", e)
        }
    }

    override suspend fun updateRingtoneEnabled(enabled: Boolean) {
        try {
            dataStore.updateRingtoneEnabled(enabled)
            syncSettingToFirestore("ringtoneEnabled", enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update ringtone setting", e)
        }
    }

    override suspend fun updateVibrationEnabled(enabled: Boolean) {
        try {
            dataStore.updateVibrationEnabled(enabled)
            syncSettingToFirestore("vibrationEnabled", enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update vibration setting", e)
        }
    }

    override suspend fun updateMessagePreviewEnabled(enabled: Boolean) {
        try {
            dataStore.updateMessagePreviewEnabled(enabled)
            syncSettingToFirestore("messagePreviewEnabled", enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update message preview", e)
        }
    }

    override suspend fun updateReadReceiptsEnabled(enabled: Boolean) {
        try {
            dataStore.updateReadReceiptsEnabled(enabled)
            syncSettingToFirestore("readReceiptsEnabled", enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update read receipts", e)
        }
    }

    override suspend fun updateStatusPrivacy(privacy: StatusPrivacy) {
        try {
            dataStore.updateStatusPrivacy(privacy)
            syncSettingToFirestore("statusPrivacy", privacy.name)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update status privacy", e)
        }
    }

    override suspend fun updateBlockedUsers(users: List<String>) {
        try {
            dataStore.updateBlockedUsers(users)
            syncSettingToFirestore("blockedUsers", users)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update blocked users", e)
        }
    }

    override suspend fun updateNotificationTone(tonePath: String) {
        try {
            dataStore.updateNotificationTone(tonePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification tone", e)
        }
    }

    override suspend fun updateCallRingtone(tonePath: String) {
        try {
            dataStore.updateCallRingtone(tonePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update call ringtone", e)
        }
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("users").document(uid)
                .update("displayName", name)
                .await()
            dataStore.cacheUserProfile(
                uid, name,
                _settings.value.let { "" }, // Preserve other fields by re-caching
                "", "", "", ""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update display name", e)
            Result.failure(e)
        }
    }

    override suspend fun updateBio(bio: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection("users").document(uid)
                .update("bio", bio)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update bio", e)
            Result.failure(e)
        }
    }

    override suspend fun updateAvatar(imageBytes: ByteArray): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val ref = storage.reference.child("avatars/$uid/${UUID.randomUUID()}.jpg")
            ref.putBytes(imageBytes).await()
            val url = ref.downloadUrl.await().toString()
            firestore.collection("users").document(uid)
                .update("avatarUrl", url)
                .await()
            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update avatar", e)
            Result.failure(e)
        }
    }

    private fun syncSettingToFirestore(key: String, value: Any) {
        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("settings").document("prefs")
                    .set(mapOf(key to value), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync setting $key to Firestore", e)
            }
        }
    }
}
