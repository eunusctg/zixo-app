package com.zexo.app.ui.screens.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.data.model.AdminConfig
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "AdminViewModel"
        private const val ADMIN_CONFIG_DOC = "admin_config"
        private const val LANDING_CONFIG_DOC = "landing_config"
    }

    // ── Users ─────────────────────────────────────────────────────────
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers.asStateFlow()

    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()

    // ── Stats ─────────────────────────────────────────────────────────
    private val _totalUsers = MutableStateFlow(0)
    val totalUsers: StateFlow<Int> = _totalUsers.asStateFlow()

    private val _activeUsers = MutableStateFlow(0)
    val activeUsers: StateFlow<Int> = _activeUsers.asStateFlow()

    private val _totalChats = MutableStateFlow(0)
    val totalChats: StateFlow<Int> = _totalChats.asStateFlow()

    private val _totalCalls = MutableStateFlow(0)
    val totalCalls: StateFlow<Int> = _totalCalls.asStateFlow()

    // ── Admin Config ──────────────────────────────────────────────────
    private val _adminConfig = MutableStateFlow(AdminConfig())
    val adminConfig: StateFlow<AdminConfig> = _adminConfig.asStateFlow()

    // ── Landing Config ────────────────────────────────────────────────
    private val _landingConfig = MutableStateFlow(LandingConfig())
    val landingConfig: StateFlow<LandingConfig> = _landingConfig.asStateFlow()

    // ── UI State ──────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _notificationText = MutableStateFlow("")
    val notificationText: StateFlow<String> = _notificationText.asStateFlow()

    // ── Selected user for detail dialog ───────────────────────────────
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    // ── Delete confirmation ───────────────────────────────────────────
    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm.asStateFlow()

    init {
        loadDashboardStats()
        loadAdminConfig()
        loadLandingConfig()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Dashboard
    // ═══════════════════════════════════════════════════════════════════

    fun loadDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Total & active users
                val usersSnapshot = firestore.collection("users").get().await()
                val allUsers = usersSnapshot.documents.mapNotNull { doc ->
                    try {
                        User(
                            uid = doc.getString("uid") ?: return@mapNotNull null,
                            displayName = doc.getString("displayName") ?: "",
                            email = doc.getString("email") ?: "",
                            username = doc.getString("username") ?: "",
                            bio = doc.getString("bio") ?: "",
                            avatar = doc.getString("avatar") ?: "",
                            phone = doc.getString("phone") ?: "",
                            zixoNumber = doc.getString("zixoNumber") ?: "",
                            online = doc.getBoolean("online") ?: false,
                            lastSeen = doc.getLong("lastSeen") ?: 0L,
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            role = doc.getString("role") ?: "user",
                            fcmToken = doc.getString("fcmToken") ?: ""
                        )
                    } catch (e: Exception) { null }
                }
                _totalUsers.value = allUsers.size
                _activeUsers.value = allUsers.count { it.online }

                // Total chats
                val chatsSnapshot = firestore.collection("chats").get().await()
                _totalChats.value = chatsSnapshot.size()

                // Total calls
                val callsSnapshot = firestore.collection("calls").get().await()
                _totalCalls.value = callsSnapshot.size()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load dashboard stats", e)
            }
            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  User Management
    // ═══════════════════════════════════════════════════════════════════

    fun loadUsers() {
        val currentUid = authRepository.currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getAllUsers(currentUid, maxResults = 200)
                .onSuccess { users ->
                    _users.value = users
                    filterUsers()
                }
                .onFailure { Log.e(TAG, "Failed to load users", it) }
            _isLoading.value = false
        }
    }

    fun updateUserSearchQuery(query: String) {
        _userSearchQuery.value = query
        filterUsers()
    }

    private fun filterUsers() {
        val query = _userSearchQuery.value
        if (query.isBlank()) {
            _filteredUsers.value = _users.value
        } else {
            _filteredUsers.value = _users.value.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true) ||
                user.username.contains(query, ignoreCase = true) ||
                user.zixoNumber.contains(query, ignoreCase = true)
            }
        }
    }

    fun selectUser(user: User?) {
        _selectedUser.value = user
    }

    fun showDeleteConfirmation() {
        _showDeleteConfirm.value = true
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirm.value = false
    }

    fun toggleAdminRole(user: User) {
        viewModelScope.launch {
            val newRole = if (user.role == "admin") "user" else "admin"
            try {
                firestore.collection("users").document(user.uid)
                    .update("role", newRole).await()
                _message.value = "${user.displayName} is now $newRole"
                // Update local list
                _users.value = _users.value.map {
                    if (it.uid == user.uid) it.copy(role = newRole) else it
                }
                _selectedUser.value = user.copy(role = newRole)
                filterUsers()
            } catch (e: Exception) {
                _message.value = "Failed to update role: ${e.message}"
            }
        }
    }

    fun toggleUserDisabled(user: User) {
        viewModelScope.launch {
            val isDisabled = user.bio.contains("[DISABLED]", ignoreCase = true)
            val newBio = if (isDisabled) {
                user.bio.replace("[DISABLED]", "").trim()
            } else {
                "[DISABLED] ${user.bio}"
            }
            try {
                firestore.collection("users").document(user.uid)
                    .update("bio", newBio).await()
                _message.value = if (isDisabled) "${user.displayName} enabled" else "${user.displayName} disabled"
                _users.value = _users.value.map {
                    if (it.uid == user.uid) it.copy(bio = newBio) else it
                }
                _selectedUser.value = user.copy(bio = newBio)
                filterUsers()
            } catch (e: Exception) {
                _message.value = "Failed to toggle account: ${e.message}"
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(user.uid).delete().await()
                _message.value = "${user.displayName} deleted"
                _users.value = _users.value.filter { it.uid != user.uid }
                _selectedUser.value = null
                _showDeleteConfirm.value = false
                filterUsers()
            } catch (e: Exception) {
                _message.value = "Failed to delete user: ${e.message}"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Admin Config
    // ═══════════════════════════════════════════════════════════════════

    fun loadAdminConfig() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("admin").document(ADMIN_CONFIG_DOC).get().await()
                if (doc.exists()) {
                    _adminConfig.value = AdminConfig(
                        maintenanceMode = doc.getBoolean("maintenanceMode") ?: false,
                        forceUpdate = doc.getBoolean("forceUpdate") ?: false,
                        latestVersion = doc.getString("latestVersion") ?: "1.3.0",
                        minVersion = doc.getString("minVersion") ?: "1.0.0",
                        message = doc.getString("message") ?: "",
                        registrationEnabled = doc.getBoolean("registrationEnabled") ?: true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load admin config", e)
            }
        }
    }

    fun updateAdminConfig(config: AdminConfig) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val updates = mapOf(
                    "maintenanceMode" to config.maintenanceMode,
                    "forceUpdate" to config.forceUpdate,
                    "latestVersion" to config.latestVersion,
                    "minVersion" to config.minVersion,
                    "message" to config.message,
                    "registrationEnabled" to config.registrationEnabled
                )
                firestore.collection("admin").document(ADMIN_CONFIG_DOC)
                    .set(updates).await()
                _adminConfig.value = config
                _message.value = "Settings saved successfully"
            } catch (e: Exception) {
                _message.value = "Failed to save settings: ${e.message}"
            }
            _isSaving.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Landing Config
    // ═══════════════════════════════════════════════════════════════════

    fun loadLandingConfig() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("admin").document(LANDING_CONFIG_DOC).get().await()
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val features = (doc.get("features") as? List<String>) ?: emptyList()
                    _landingConfig.value = LandingConfig(
                        heroTitle = doc.getString("heroTitle") ?: "Welcome to Zexo",
                        heroSubtitle = doc.getString("heroSubtitle") ?: "Connect Freely",
                        heroDescription = doc.getString("heroDescription")
                            ?: "The modern messaging platform built for privacy and freedom.",
                        features = features,
                        ctaText = doc.getString("ctaText") ?: "Get Started"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load landing config", e)
            }
        }
    }

    fun updateLandingConfig(config: LandingConfig) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val updates = mapOf(
                    "heroTitle" to config.heroTitle,
                    "heroSubtitle" to config.heroSubtitle,
                    "heroDescription" to config.heroDescription,
                    "features" to config.features,
                    "ctaText" to config.ctaText
                )
                firestore.collection("admin").document(LANDING_CONFIG_DOC)
                    .set(updates).await()
                _landingConfig.value = config
                _message.value = "Landing page saved successfully"
            } catch (e: Exception) {
                _message.value = "Failed to save landing page: ${e.message}"
            }
            _isSaving.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Notifications
    // ═══════════════════════════════════════════════════════════════════

    fun updateNotificationText(text: String) {
        _notificationText.value = text
    }

    fun sendNotification() {
        val text = _notificationText.value.trim()
        if (text.isBlank()) {
            _message.value = "Notification text cannot be empty"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val notification = mapOf(
                    "title" to "Zexo",
                    "body" to text,
                    "timestamp" to System.currentTimeMillis(),
                    "sentBy" to (authRepository.currentUid ?: "admin")
                )
                firestore.collection("notifications").add(notification).await()
                _notificationText.value = ""
                _message.value = "Notification sent to all users"
            } catch (e: Exception) {
                _message.value = "Failed to send notification: ${e.message}"
            }
            _isSaving.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Message handling
    // ═══════════════════════════════════════════════════════════════════

    fun clearMessage() {
        _message.value = null
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helper
    // ═══════════════════════════════════════════════════════════════════

    fun isUserDisabled(user: User): Boolean {
        return user.bio.contains("[DISABLED]", ignoreCase = true)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "AdminViewModel cleared")
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Landing config model (local to admin screens)
// ═══════════════════════════════════════════════════════════════════════

data class LandingConfig(
    val heroTitle: String = "Welcome to Zexo",
    val heroSubtitle: String = "Connect Freely",
    val heroDescription: String = "The modern messaging platform built for privacy and freedom.",
    val features: List<String> = listOf(
        "End-to-end encrypted chats",
        "Free voice & video calls",
        "Share moments with status",
        "Cross-platform support"
    ),
    val ctaText: String = "Get Started"
)


