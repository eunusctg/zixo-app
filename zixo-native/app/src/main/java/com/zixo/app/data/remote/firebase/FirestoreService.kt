package com.zixo.app.data.remote.firebase

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.Session
import com.zixo.app.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore Service — All Firestore operations using continuous snapshot listeners.
 *
 * Provides:
 * - User profile CRUD with real-time listeners
 * - Contact list operations with mutual verification
 * - Thread/message operations (delegated to repositories)
 * - Call log operations with real-time listeners
 * - Session management
 * - File upload to Firebase Storage
 *
 * NO LiveKit references. All operations are pure Firestore/Firebase Storage.
 * All snapshot-based reads use addSnapshotListener for continuous real-time sync.
 * All operations run on Dispatchers.IO via the calling repository.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    // ── Collections ───────────────────────────────────────────────────────────

    private val usersCollection get() = firestore.collection("users")
    private fun blockedCollection(uid: String) = usersCollection.document(uid).collection("blocked")
    private fun callLogCollection(uid: String) = usersCollection.document(uid).collection("call_log")
    private fun sessionsCollection(uid: String) = usersCollection.document(uid).collection("sessions")

    // ── User Profile ──────────────────────────────────────────────────────────

    /**
     * Observe a user profile in real-time via addSnapshotListener.
     */
    fun getUserProfile(uid: String): Flow<User?> = callbackFlow {
        val docRef = usersCollection.document(uid)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Error observing user profile: %s", uid)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toUser())
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Update specific fields on an existing user profile.
     */
    fun updateUserProfile(uid: String, updates: Map<String, Any?>): Flow<Unit> = flow {
        usersCollection.document(uid).update(updates).await()
        Timber.d("User profile updated: %s", uid)
        emit(Unit)
    }

    /**
     * Create a brand-new user profile document.
     */
    fun createUserProfile(uid: String, user: User): Flow<Unit> = flow {
        usersCollection.document(uid).set(user.toFirestoreMap()).await()
        Timber.d("User profile created: %s", uid)
        emit(Unit)
    }

    // ── Online Status ─────────────────────────────────────────────────────────

    /**
     * Update the user's online / offline flag.
     */
    fun updateOnlineStatus(uid: String, isOnline: Boolean): Flow<Unit> = flow {
        usersCollection.document(uid).update(
            mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
        ).await()
        emit(Unit)
    }

    /**
     * Update only the last-seen timestamp.
     */
    fun updateLastSeen(uid: String): Flow<Unit> = flow {
        usersCollection.document(uid).update("lastSeen", System.currentTimeMillis()).await()
        emit(Unit)
    }

    // ── Blocking ──────────────────────────────────────────────────────────────

    /**
     * Observe the list of blocked user IDs in real-time.
     */
    fun getBlockedUsers(uid: String): Flow<List<String>> = callbackFlow {
        val subscription = blockedCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Error observing blocked users")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val blockedIds = snapshot?.documents?.mapNotNull { it.id } ?: emptyList()
            trySend(blockedIds)
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Block a user by adding their uid to the blocked sub-collection.
     */
    fun blockUser(uid: String, blockedUid: String): Flow<Unit> = flow {
        blockedCollection(uid).document(blockedUid)
            .set(mapOf("blockedAt" to System.currentTimeMillis())).await()
        emit(Unit)
    }

    /**
     * Unblock a user by removing their uid from the blocked sub-collection.
     */
    fun unblockUser(uid: String, blockedUid: String): Flow<Unit> = flow {
        blockedCollection(uid).document(blockedUid).delete().await()
        emit(Unit)
    }

    // ── Call Logs ─────────────────────────────────────────────────────────────

    /**
     * Observe call log entries for the given user in real-time.
     */
    fun getCallLogs(uid: String): Flow<List<CallLogEntry>> = callbackFlow {
        val subscription = callLogCollection(uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing call logs")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val calls = snapshot?.documents?.mapNotNull { it.toCallLogEntry() } ?: emptyList()
                trySend(calls)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Insert a call log entry into Firestore.
     */
    fun insertCallLog(uid: String, entry: CallLogEntry): Flow<Unit> = flow {
        callLogCollection(uid).document(entry.id).set(entry.toFirestoreMap()).await()
        emit(Unit)
    }

    /**
     * Clear the entire call history for the given user.
     */
    fun clearCallHistory(uid: String): Flow<Unit> = flow {
        val snapshot = callLogCollection(uid).get().await()
        for (doc in snapshot.documents) { doc.reference.delete().await() }
        emit(Unit)
    }

    // ── Account Deletion ──────────────────────────────────────────────────────

    /**
     * Delete all user data from Firestore (profile, contacts, blocked list, etc.).
     * Best-effort deletion; Cloud Functions should handle cascading sub-collection cleanup.
     */
    fun deleteUserData(uid: String): Flow<Unit> = flow {
        // Delete blocked sub-collection
        val blockedSnapshot = blockedCollection(uid).get().await()
        for (doc in blockedSnapshot.documents) { doc.reference.delete().await() }

        // Delete call log sub-collection
        val callLogSnapshot = callLogCollection(uid).get().await()
        for (doc in callLogSnapshot.documents) { doc.reference.delete().await() }

        // Delete sessions sub-collection
        val sessionsSnapshot = sessionsCollection(uid).get().await()
        for (doc in sessionsSnapshot.documents) { doc.reference.delete().await() }

        // Delete the user profile document
        usersCollection.document(uid).delete().await()

        Timber.d("User data deleted: %s", uid)
        emit(Unit)
    }

    // ── File Upload ───────────────────────────────────────────────────────────

    /**
     * Upload a file to Firebase Storage and return the download URL.
     */
    suspend fun uploadFile(storagePath: String, uri: Uri): String {
        val ref = storage.reference.child(storagePath)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // ── Account Data Export ───────────────────────────────────────────────────

    /**
     * Requests a GDPR-style account data export.
     */
    fun requestAccountInfo(uid: String): Flow<Unit> = flow {
        val exportRef = usersCollection.document(uid).collection("data_exports").document()
        exportRef.set(
            mapOf("requestedAt" to System.currentTimeMillis(), "status" to "PENDING")
        ).await()
        emit(Unit)
    }

    // ── Session Management ────────────────────────────────────────────────────

    /**
     * Observe all active sessions for the given user in real-time.
     */
    fun observeActiveSessions(uid: String): Flow<List<Session>> = callbackFlow {
        val subscription = sessionsCollection(uid)
            .orderBy("lastActive", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing sessions")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { it.toSession() } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Revoke a session by deleting it from Firestore.
     */
    fun revokeSession(uid: String, sessionId: String): Flow<Unit> = flow {
        sessionsCollection(uid).document(sessionId).delete().await()
        emit(Unit)
    }

    // ── Mapping Helpers ───────────────────────────────────────────────────────

    private fun DocumentSnapshot.toUser(): User? {
        return try {
            User(
                uid = id,
                displayName = getString("displayName") ?: return null,
                username = getString("username") ?: "",
                email = getString("email") ?: "",
                photoUrl = getString("photoUrl"),
                phoneNumber = getString("phoneNumber"),
                bio = getString("bio"),
                zixoNumber = getString("zixoNumber") ?: "",
                isOnline = getBoolean("isOnline") ?: false,
                lastSeen = getLong("lastSeen") ?: 0L,
                blockedUsers = (get("blockedUsers") as? List<String>) ?: emptyList(),
                fcmToken = getString("fcmToken"),
                createdAt = getLong("createdAt") ?: 0L,
                passkeyCredentialId = getString("passkeyCredentialId"),
                hasPasskey = getBoolean("hasPasskey") ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map DocumentSnapshot to User")
            null
        }
    }

    private fun DocumentSnapshot.toCallLogEntry(): CallLogEntry? {
        return try {
            val directionStr = getString("type") ?: "OUTGOING"
            val direction = try { com.zixo.app.domain.model.CallDirection.valueOf(directionStr) }
                catch (_: Exception) { com.zixo.app.domain.model.CallDirection.OUTGOING }

            val endReasonStr = getString("endReason") ?: "COMPLETED"
            val endReason = try { com.zixo.app.domain.model.CallEndReason.valueOf(endReasonStr) }
                catch (_: Exception) { com.zixo.app.domain.model.CallEndReason.COMPLETED }

            CallLogEntry(
                id = id,
                callId = getString("callId") ?: "",
                callerUid = getString("callerUid") ?: return null,
                calleeUid = getString("calleeUid") ?: return null,
                callerName = getString("callerName") ?: "",
                calleeName = getString("calleeName") ?: "",
                callerAvatar = getString("callerAvatar"),
                calleeAvatar = getString("calleeAvatar"),
                type = direction,
                isVideoCall = getBoolean("isVideoCall") ?: false,
                isGroupCall = getBoolean("isGroupCall") ?: false,
                duration = getLong("duration") ?: 0L,
                timestamp = getLong("timestamp") ?: 0L,
                endReason = endReason,
                threadId = getString("threadId") ?: "",
                isRead = getBoolean("isRead") ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map DocumentSnapshot to CallLogEntry")
            null
        }
    }

    private fun DocumentSnapshot.toSession(): Session? {
        return try {
            Session(
                id = id,
                deviceName = getString("deviceName") ?: "Unknown",
                deviceModel = getString("deviceModel") ?: "",
                osVersion = getString("osVersion") ?: "",
                appVersion = getString("appVersion") ?: "",
                ipAddress = getString("ipAddress"),
                lastActive = getLong("lastActive") ?: 0L,
                isActive = getBoolean("isActive") ?: false,
                createdAt = getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map DocumentSnapshot to Session")
            null
        }
    }
}

// ── Firestore Serialization Extensions ────────────────────────────────────────

private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
    "displayName" to displayName,
    "username" to username,
    "email" to email,
    "photoUrl" to photoUrl,
    "phoneNumber" to phoneNumber,
    "bio" to bio,
    "zixoNumber" to zixoNumber,
    "isOnline" to isOnline,
    "lastSeen" to lastSeen,
    "blockedUsers" to blockedUsers,
    "fcmToken" to fcmToken,
    "createdAt" to createdAt,
    "passkeyCredentialId" to passkeyCredentialId,
    "hasPasskey" to hasPasskey
)

private fun CallLogEntry.toFirestoreMap(): Map<String, Any?> = mapOf(
    "callId" to callId,
    "callerUid" to callerUid,
    "calleeUid" to calleeUid,
    "callerName" to callerName,
    "calleeName" to calleeName,
    "callerAvatar" to callerAvatar,
    "calleeAvatar" to calleeAvatar,
    "type" to type.name,
    "isVideoCall" to isVideoCall,
    "isGroupCall" to isGroupCall,
    "duration" to duration,
    "timestamp" to timestamp,
    "endReason" to endReason.name,
    "threadId" to threadId,
    "isRead" to isRead
)
