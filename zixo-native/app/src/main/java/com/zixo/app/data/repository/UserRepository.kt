package com.zixo.app.data.repository

import android.net.Uri
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val firebaseAuthService: FirebaseAuthService
) {

    /**
     * Observes the current authenticated user's profile from Firestore.
     * Automatically switches to a new user profile when the auth state changes.
     * Returns null if no user is currently authenticated.
     */
    fun getCurrentUserProfile(): Flow<User?> =
        firebaseAuthService.authStateFlow().flatMapLatest { firebaseUser ->
            if (firebaseUser != null) {
                firestoreService.getUserProfile(firebaseUser.uid)
            } else {
                flowOf(null)
            }
        }

    /**
     * Updates the current user's profile fields in Firestore.
     *
     * @param updates A map of field names to their new values. Null values will
     *                delete the corresponding field from the Firestore document.
     * @return A [Flow] that emits [Unit] once the update is complete.
     */
    fun updateUserProfile(updates: Map<String, Any?>): Flow<Unit> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot update profile: no authenticated user")
        firestoreService.updateUserProfile(uid, updates).first()
        emit(Unit)
    }

    /**
     * Observes a specific user's public profile from Firestore in real-time.
     *
     * @param uid The user ID of the profile to observe.
     * @return A [Flow] of the [User] profile, or null if not found.
     */
    fun getUserProfile(uid: String): Flow<User?> =
        firestoreService.getUserProfile(uid)

    /**
     * Updates the current user's online status in Firestore.
     * Typically called with `true` on app foreground and `false` on background.
     *
     * @param isOnline Whether the user is currently online.
     * @return A [Flow] that emits [Unit] once the status update is complete.
     */
    fun updateOnlineStatus(isOnline: Boolean): Flow<Unit> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot update online status: no authenticated user")
        firestoreService.updateOnlineStatus(uid, isOnline).first()
        emit(Unit)
    }

    /**
     * Uploads a profile avatar image to Firebase Storage and returns the
     * download URL upon completion. Also updates the user's photoUrl field
     * in Firestore.
     *
     * @param imageUri The local URI of the image to upload.
     * @return A [Flow] that emits the download URL string once the upload completes.
     */
    fun uploadAvatar(imageUri: Uri): Flow<String> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot upload avatar: no authenticated user")
        val downloadUrl = firestoreService.uploadFile("avatars/$uid/profile.jpg", imageUri)
        firestoreService.updateUserProfile(uid, mapOf("photoUrl" to downloadUrl)).first()
        emit(downloadUrl)
    }

    /**
     * Observes the list of blocked user IDs for the current user in real-time.
     *
     * @return A [Flow] of blocked user ID strings.
     */
    fun getBlockedUsers(): Flow<List<String>> = flow {
        val uid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot get blocked users: no authenticated user")
        firestoreService.getBlockedUsers(uid).collect { blockedList ->
            emit(blockedList)
        }
    }

    /**
     * Blocks a user by adding their UID to the current user's blocked list
     * in Firestore.
     *
     * @param uid The user ID of the user to block.
     * @return A [Flow] that emits [Unit] once the operation completes.
     */
    fun blockUser(uid: String): Flow<Unit> = flow {
        val currentUid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot block user: no authenticated user")
        firestoreService.blockUser(currentUid, uid).first()
        emit(Unit)
    }

    /**
     * Unblocks a user by removing their UID from the current user's blocked
     * list in Firestore.
     *
     * @param uid The user ID of the user to unblock.
     * @return A [Flow] that emits [Unit] once the operation completes.
     */
    fun unblockUser(uid: String): Flow<Unit> = flow {
        val currentUid = firebaseAuthService.getCurrentUser()?.uid
            ?: throw IllegalStateException("Cannot unblock user: no authenticated user")
        firestoreService.unblockUser(currentUid, uid).first()
        emit(Unit)
    }
}
