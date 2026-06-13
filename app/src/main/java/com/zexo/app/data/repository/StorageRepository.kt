package com.zexo.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadAvatar(uid: String, imageUri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("avatars/$uid.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to upload avatar"))
        }
    }

    suspend fun uploadChatMedia(chatId: String, fileUri: Uri, fileName: String): Result<String> {
        return try {
            val ref = storage.reference.child("chats/$chatId/$fileName")
            ref.putFile(fileUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to upload media"))
        }
    }
}
