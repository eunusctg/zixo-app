package com.zexo.app.ui.screens.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.data.model.User
import com.zexo.app.data.model.UserSettings
import com.zexo.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class SettingsUiState(
    val currentUser: User? = null,
    val settings: UserSettings = UserSettings(),
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val error: String? = null,
    val appVersion: String = "1.6.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val SETTINGS_COLLECTION = "user_settings"

        // DataStore keys
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val KEY_SHOW_ONLINE_STATUS = booleanPreferencesKey("show_online_status")
        val KEY_SHOW_LAST_SEEN = booleanPreferencesKey("show_last_seen")
        val KEY_READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val KEY_TYPING_INDICATORS = booleanPreferencesKey("typing_indicators")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Exposed for theme observation in MainActivity
    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val currentUid: String? get() = authRepository.currentUid

    init {
        loadUserProfile()
        loadSettings()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Load data
    // ═══════════════════════════════════════════════════════════════════

    private fun loadUserProfile() {
        val uid = currentUid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.getUserProfile(uid)
                .onSuccess { user ->
                    _uiState.update { it.copy(currentUser = user) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to load user profile", e)
                }
        }
    }

    private fun loadSettings() {
        val uid = currentUid ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Try DataStore first for fast local read
            try {
                val dataStore = getDataStore()
                val prefs = dataStore.data.first()
                val localSettings = UserSettings(
                    darkTheme = prefs[KEY_DARK_THEME] ?: true,
                    notifications = prefs[KEY_NOTIFICATIONS] ?: true,
                    biometricLock = prefs[KEY_BIOMETRIC_LOCK] ?: false,
                    showOnlineStatus = prefs[KEY_SHOW_ONLINE_STATUS] ?: true,
                    showLastSeen = prefs[KEY_SHOW_LAST_SEEN] ?: true,
                    readReceipts = prefs[KEY_READ_RECEIPTS] ?: true,
                    typingIndicators = prefs[KEY_TYPING_INDICATORS] ?: true
                )
                _uiState.update { it.copy(settings = localSettings, isLoading = false) }
                _darkTheme.value = localSettings.darkTheme
            } catch (e: Exception) {
                Log.w(TAG, "DataStore read failed, trying Firestore", e)
            }

            // Then merge with Firestore (source of truth)
            try {
                val doc = firestore.collection(SETTINGS_COLLECTION).document(uid).get().await()
                if (doc.exists()) {
                    val remoteSettings = UserSettings(
                        darkTheme = doc.getBoolean("darkTheme") ?: true,
                        notifications = doc.getBoolean("notifications") ?: true,
                        biometricLock = doc.getBoolean("biometricLock") ?: false,
                        showOnlineStatus = doc.getBoolean("showOnlineStatus") ?: true,
                        showLastSeen = doc.getBoolean("showLastSeen") ?: true,
                        readReceipts = doc.getBoolean("readReceipts") ?: true,
                        typingIndicators = doc.getBoolean("typingIndicators") ?: true
                    )
                    _uiState.update { it.copy(settings = remoteSettings, isLoading = false) }
                    _darkTheme.value = remoteSettings.darkTheme
                    // Sync back to DataStore
                    saveToDataStore(remoteSettings)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore settings read failed", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Settings update actions
    // ═══════════════════════════════════════════════════════════════════

    fun setDarkTheme(enabled: Boolean) {
        updateSetting { it.copy(darkTheme = enabled) }
        _darkTheme.value = enabled
    }

    fun setNotifications(enabled: Boolean) {
        updateSetting { it.copy(notifications = enabled) }
    }

    fun setBiometricLock(enabled: Boolean) {
        updateSetting { it.copy(biometricLock = enabled) }
    }

    fun setShowOnlineStatus(enabled: Boolean) {
        updateSetting { it.copy(showOnlineStatus = enabled) }
    }

    fun setShowLastSeen(enabled: Boolean) {
        updateSetting { it.copy(showLastSeen = enabled) }
    }

    fun setReadReceipts(enabled: Boolean) {
        updateSetting { it.copy(readReceipts = enabled) }
    }

    fun setTypingIndicators(enabled: Boolean) {
        updateSetting { it.copy(typingIndicators = enabled) }
    }

    private fun updateSetting(transform: (UserSettings) -> UserSettings) {
        val current = _uiState.value.settings
        val updated = transform(current)
        _uiState.update { it.copy(settings = updated) }
        saveSettings(updated)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Persist settings
    // ═══════════════════════════════════════════════════════════════════

    private fun saveSettings(settings: UserSettings) {
        val uid = currentUid ?: return

        // Save to DataStore immediately
        viewModelScope.launch(Dispatchers.IO) {
            saveToDataStore(settings)
        }

        // Save to Firestore
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "darkTheme" to settings.darkTheme,
                    "notifications" to settings.notifications,
                    "biometricLock" to settings.biometricLock,
                    "showOnlineStatus" to settings.showOnlineStatus,
                    "showLastSeen" to settings.showLastSeen,
                    "readReceipts" to settings.readReceipts,
                    "typingIndicators" to settings.typingIndicators,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(SETTINGS_COLLECTION).document(uid).set(data).await()
                Log.d(TAG, "Settings saved to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings to Firestore", e)
            }
        }
    }

    private suspend fun saveToDataStore(settings: UserSettings) {
        try {
            val dataStore = getDataStore()
            dataStore.edit { prefs ->
                prefs[KEY_DARK_THEME] = settings.darkTheme
                prefs[KEY_NOTIFICATIONS] = settings.notifications
                prefs[KEY_BIOMETRIC_LOCK] = settings.biometricLock
                prefs[KEY_SHOW_ONLINE_STATUS] = settings.showOnlineStatus
                prefs[KEY_SHOW_LAST_SEEN] = settings.showLastSeen
                prefs[KEY_READ_RECEIPTS] = settings.readReceipts
                prefs[KEY_TYPING_INDICATORS] = settings.typingIndicators
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to DataStore", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Logout
    // ═══════════════════════════════════════════════════════════════════

    fun logout() {
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
                .onSuccess {
                    _uiState.update { it.copy(isLoggingOut = false, logoutSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoggingOut = false, error = e.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DataStore access via Application context
    // ═══════════════════════════════════════════════════════════════════

    private lateinit var _appContext: Context

    fun setAppContext(context: Context) {
        _appContext = context.applicationContext
    }

    private fun getDataStore(): DataStore<Preferences> {
        return _appContext.settingsDataStore
    }
}
