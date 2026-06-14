package com.zixo.app.ui.screens.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zixo.app.data.repository.UserRepository
import com.zixo.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val bio: String = "",
    val photoUrl: String? = null,
    val zixoNumber: String = "",
    val username: String = "",
    val phoneNumber: String? = null,
    val selectedImageUri: Uri? = null,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // Track original values for change detection
    private var originalDisplayName: String = ""
    private var originalBio: String = ""
    private var originalPhotoUrl: String? = null

    private val currentUserFlow = userRepository.getCurrentUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            currentUserFlow.collect { user ->
                if (user != null) {
                    originalDisplayName = user.displayName
                    originalBio = user.bio ?: ""
                    originalPhotoUrl = user.photoUrl

                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            displayName = user.displayName,
                            bio = user.bio ?: "",
                            photoUrl = user.photoUrl,
                            zixoNumber = user.zixoNumber,
                            username = user.username,
                            phoneNumber = user.phoneNumber,
                            hasChanges = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onDisplayNameChange(name: String) {
        if (name.length <= DISPLAY_NAME_MAX_LENGTH) {
            _uiState.update {
                it.copy(
                    displayName = name,
                    hasChanges = computeHasChanges(name, it.bio, it.selectedImageUri),
                    saveError = null
                )
            }
        }
    }

    fun onBioChange(bio: String) {
        if (bio.length <= BIO_MAX_LENGTH) {
            _uiState.update {
                it.copy(
                    bio = bio,
                    hasChanges = computeHasChanges(it.displayName, bio, it.selectedImageUri),
                    saveError = null
                )
            }
        }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                hasChanges = computeHasChanges(it.displayName, it.bio, uri),
                saveError = null
            )
        }
    }

    fun onSaveChanges() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(saveError = "Display name cannot be empty") }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveError = null, saveSuccess = false) }

        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any?>()

                if (state.displayName != originalDisplayName) {
                    updates["displayName"] = state.displayName
                }
                if (state.bio != originalBio) {
                    updates["bio"] = state.bio.ifBlank { null }
                }

                // Upload new avatar if selected
                if (state.selectedImageUri != null) {
                    try {
                        val downloadUrl = userRepository.uploadAvatar(state.selectedImageUri).first()
                        updates["photoUrl"] = downloadUrl
                    } catch (_: Exception) {
                        // Avatar upload failed, continue with other updates
                    }
                }

                if (updates.isNotEmpty()) {
                    userRepository.updateUserProfile(updates).first()
                }

                // Update original values to reflect saved state
                originalDisplayName = state.displayName
                originalBio = state.bio
                originalPhotoUrl = state.photoUrl

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasChanges = false,
                        saveSuccess = true,
                        selectedImageUri = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = e.localizedMessage ?: "Failed to save changes"
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    private fun computeHasChanges(displayName: String, bio: String, selectedImageUri: Uri?): Boolean {
        return displayName != originalDisplayName ||
                bio != originalBio ||
                selectedImageUri != null
    }

    companion object {
        const val DISPLAY_NAME_MAX_LENGTH = 30
        const val BIO_MAX_LENGTH = 100
    }
}
