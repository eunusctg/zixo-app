package com.zixo.app.domain.repository

import com.zixo.app.domain.model.AuthState
import com.zixo.app.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<UserProfile?>

    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshUserProfile(): Result<UserProfile>
    suspend fun updateOnlineStatus(isOnline: Boolean)
    fun getCurrentUserId(): String?
}
