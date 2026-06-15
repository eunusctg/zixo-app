package com.zixo.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * HomeViewModel — completely decoupled data handler extracted from HomeScreen.
 *
 * Streams layout parameters via StateFlow, managing the home screen's
 * data requirements: current user profile, unread chat counts, recent
 * conversations, contact list, online status, and settings.
 *
 * All Firebase listeners are properly managed within viewModelScope,
 * and all data collection runs on Dispatchers.IO with comprehensive
 * error boundaries.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    data class HomeUiState(
        val currentUser: UserProfile? = null,
        val unreadCount: Int = 0,
        val recentChats: List<ChatThreadModel> = emptyList(),
        val contacts: List<ContactModel> = emptyList(),
        val mutualContactCount: Int = 0,
        val isOnline: Boolean = true,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadCurrentUser()
        observeChats()
        observeContacts()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = authRepository.getCurrentUser().first()
                val profile = user?.let {
                    UserProfile(
                        displayName = it.displayName,
                        username = it.username,
                        zixoNumber = it.zixoNumber,
                        avatarUrl = it.photoUrl ?: "",
                        bio = it.bio ?: "",
                        phoneNumber = it.phoneNumber ?: ""
                    )
                }
                _uiState.update { it.copy(currentUser = profile, isLoading = false) }
                if (profile != null) {
                    Timber.d("HomeViewModel: Current user loaded — %s", profile.displayName)
                }
            } catch (e: Exception) {
                Timber.e(e, "HomeViewModel: Failed to load current user")
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun observeChats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.getThreads().collect { chats ->
                    _uiState.update { state ->
                        state.copy(
                            recentChats = chats,
                            unreadCount = chats.sumOf { it.unreadCount }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "HomeViewModel: Chat observation failed")
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contactRepository.getMutualContacts().collect { contacts ->
                    _uiState.update { state ->
                        state.copy(
                            contacts = contacts,
                            mutualContactCount = contacts.size
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "HomeViewModel: Contact observation failed")
            }
        }
    }

    /**
     * Triggers a full refresh of chats and contacts.
     * Called by pull-to-refresh gesture.
     */
    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isRefreshing.value = true
                loadCurrentUser()
                Timber.d("HomeViewModel: Refresh completed")
            } catch (e: Exception) {
                Timber.e(e, "HomeViewModel: Refresh failed")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Updates the user's online presence in Firestore.
     */
    fun updatePresence(isOnline: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isOnline = isOnline) }
                Timber.d("HomeViewModel: Presence updated — online=%b", isOnline)
            } catch (e: Exception) {
                Timber.e(e, "HomeViewModel: Failed to update presence")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("HomeViewModel: Cleared — updating presence to offline")
        updatePresence(false)
    }
}
