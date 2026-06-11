package com.zexo.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    fun signInWithEmail(email: String, password: String, callback: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            callback(result)
        }
    }
    
    fun signUpWithEmail(email: String, password: String, displayName: String, callback: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, password, displayName)
            callback(result)
        }
    }
    
    fun signInWithGoogle(idToken: String, callback: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            callback(result)
        }
    }
    
    fun resetPassword(email: String, callback: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            callback(result)
        }
    }
}
