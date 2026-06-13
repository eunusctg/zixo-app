package com.zixo.app.domain.repository

import com.zixo.app.domain.model.StatusUpdate
import com.zixo.app.domain.model.UserStatus
import kotlinx.coroutines.flow.StateFlow

interface StatusRepository {
    val userStatuses: StateFlow<List<UserStatus>>
    val myStatuses: StateFlow<List<StatusUpdate>>

    suspend fun createTextStatus(text: String, backgroundColor: String): Result<StatusUpdate>
    suspend fun createImageStatus(imageBytes: ByteArray, caption: String): Result<StatusUpdate>
    suspend fun deleteStatus(statusId: String): Result<Unit>
    suspend fun markStatusViewed(statusId: String): Result<Unit>
    suspend fun addStatusReaction(statusId: String, emoji: String): Result<Unit>
    fun startListening()
    fun stopListening()
}
