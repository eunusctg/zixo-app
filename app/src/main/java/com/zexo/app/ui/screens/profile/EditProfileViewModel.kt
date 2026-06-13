package com.zexo.app.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.model.ZixoUser
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.StorageRepository
import com.zexo.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val phone: String = "",
    val zixoNumber: String = "",
    val avatarUrl: String = "",
    val selectedImageUri: Uri? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        authRepository.currentUser.value?.let { user ->
            _uiState.value = EditProfileUiState(
                displayName = user.displayName,
                username = user.username,
                bio = user.bio,
                phone = user.phone,
                zixoNumber = user.zixoNumber,
                avatarUrl = user.avatar
            )
        }

        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                user?.let {
                    if (_uiState.value.displayName.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            displayName = it.displayName,
                            username = it.username,
                            bio = it.bio,
                            phone = it.phone,
                            zixoNumber = it.zixoNumber,
                            avatarUrl = it.avatar
                        )
                    }
                }
            }
        }
    }

    fun onDisplayNameChange(name: String) {
        if (name.length <= 30) {
            _uiState.value = _uiState.value.copy(displayName = name)
        }
    }

    fun onBioChange(bio: String) {
        if (bio.length <= 100) {
            _uiState.value = _uiState.value.copy(bio = bio)
        }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun saveChanges() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.value = state.copy(error = "Display name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val uid = authRepository.firebaseUser?.uid ?: return@launch

            var avatarUrl = state.avatarUrl
            if (state.selectedImageUri != null) {
                storageRepository.uploadAvatar(uid, state.selectedImageUri!!).getOrNull()?.let {
                    avatarUrl = it
                }
            }

            val updates = mapOf(
                "displayName" to state.displayName,
                "bio" to state.bio,
                "avatar" to avatarUrl
            )

            val result = userRepository.updateUserProfile(uid, updates)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = result.isSuccess,
                error = result.exceptionOrNull()?.message,
                avatarUrl = avatarUrl
            )
        }
    }
}
