package com.zexo.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.zexo.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun checkAuth(callback: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        callback(user != null)
    }
}
