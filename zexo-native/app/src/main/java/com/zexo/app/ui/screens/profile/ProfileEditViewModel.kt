package com.zexo.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val user: User? = null,
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val phone: String = "",
    val zixoNumber: String = "",
    val avatar: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    val currentUid: String? get() = authRepository.currentUid

    fun loadProfile() {
        val uid = currentUid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.getUserProfile(uid).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        displayName = user.displayName,
                        username = user.username,
                        bio = user.bio,
                        phone = user.phone,
                        zixoNumber = user.zixoNumber,
                        avatar = user.avatar,
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun updateBio(bio: String) {
        _uiState.update { it.copy(bio = bio) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun saveProfile() {
        val uid = currentUid ?: return
        val state = _uiState.value

        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(error = "Display name cannot be empty") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val updates = mapOf<String, Any>(
                "displayName" to state.displayName.trim(),
                "username" to state.username.trim(),
                "bio" to state.bio.trim(),
                "phone" to state.phone.trim()
            )

            authRepository.updateUserProfile(uid, updates).onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
