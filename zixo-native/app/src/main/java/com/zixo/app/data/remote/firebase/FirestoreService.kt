package com.zixo.app.data.remote.firebase

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.CallDirection
import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.CallTechnology
import com.zixo.app.domain.model.ChatThread
import com.zixo.app.domain.model.Message
import com.zixo.app.domain.model.MessageType
import com.zixo.app.domain.model.Session
import com.zixo.app.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    // ---------------------------------------------------------------------------
    // Collections
    // ---------------------------------------------------------------------------

    private val usersCollection get() = firestore.collection("users")
    private fun threadsCollection(uid: String) =
        usersCollection.document(uid).collection("threads")
    private fun messagesCollection(threadId: String) = firestore.collection("threads")
        .document(threadId)
        .collection("messages")
    private fun blockedCollection(uid: String) = usersCollection.document(uid)
        .collection("blocked")
    private fun callLogCollection(uid: String) = usersCollection.document(uid)
        .collection("call_log")
    private fun sessionsCollection(uid: String) = usersCollection.document(uid)
        .collection("sessions")

    // ---------------------------------------------------------------------------
    // User profile
    // ---------------------------------------------------------------------------

    /**
     * Observe a user profile in real-time.
     */
    fun getUserProfile(uid: String): Flow<User?> = callbackFlow {
        val docRef = usersCollection.document(uid)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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
        emit(Unit)
    }

    /**
     * Create a brand-new user profile document.
     */
    fun createUserProfile(uid: String, user: User): Flow<Unit> = flow {
        usersCollection.document(uid).set(user.toFirestoreMap()).await()
        emit(Unit)
    }

    // ---------------------------------------------------------------------------
    // Chat threads
    // ---------------------------------------------------------------------------

    /**
     * Observe the list of chat threads for the given user in real-time,
     * ordered by the most recent message timestamp (descending).
     */
    fun getChatThreads(uid: String): Flow<List<ChatThread>> = callbackFlow {
        val subscription = threadsCollection(uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val threads = snapshot?.documents?.mapNotNull { it.toChatThread() }
                    ?: emptyList()
                trySend(threads)
            }
        awaitClose { subscription.remove() }
    }

    // ---------------------------------------------------------------------------
    // Messages
    // ---------------------------------------------------------------------------

    /**
     * Observe the most recent [limit] messages in a thread in real-time,
     * ordered by timestamp ascending.
     */
    fun getMessages(threadId: String, limit: Long = 50): Flow<List<Message>> = callbackFlow {
        val subscription = messagesCollection(threadId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toMessage() }
                    ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Send a message to a thread.
     */
    fun sendMessage(threadId: String, message: Message): Flow<Unit> = flow {
        messagesCollection(threadId).add(message.toFirestoreMap()).await()

        // Update the thread's last-message metadata
        val threadUpdates = mapOf(
            "lastMessage" to message.content,
            "lastMessageTimestamp" to message.timestamp.toEpochMilli()
        )
        firestore.collection("threads").document(threadId).update(threadUpdates).await()
        emit(Unit)
    }

    // ---------------------------------------------------------------------------
    // Online status
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // Blocking
    // ---------------------------------------------------------------------------

    /**
     * Observe the list of blocked user IDs in real-time.
     */
    fun getBlockedUsers(uid: String): Flow<List<String>> = callbackFlow {
        val subscription = blockedCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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

    // ---------------------------------------------------------------------------
    // Account deletion
    // ---------------------------------------------------------------------------

    /**
     * Delete all user data from Firestore (profile, threads, blocked list).
     * This is a best-effort deletion; it does not recursively delete every
     * sub-collection in threads/messages. A Cloud Function should handle
     * full cascading deletion for production.
     */
    fun deleteUserData(uid: String): Flow<Unit> = flow {
        // Delete blocked sub-collection documents
        val blockedSnapshot = blockedCollection(uid).get().await()
        for (doc in blockedSnapshot.documents) {
            doc.reference.delete().await()
        }

        // Delete thread references
        val threadsSnapshot = threadsCollection(uid).get().await()
        for (doc in threadsSnapshot.documents) {
            doc.reference.delete().await()
        }

        // Delete the user profile document
        usersCollection.document(uid).delete().await()
        emit(Unit)
    }

    // ---------------------------------------------------------------------------
    // Call logs
    // ---------------------------------------------------------------------------

    /**
     * Observe the list of call log entries for the given user in real-time,
     * ordered by timestamp descending.
     */
    fun getCallLogs(uid: String): Flow<List<CallLogEntry>> = callbackFlow {
        val subscription = callLogCollection(uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val calls = snapshot?.documents?.mapNotNull { it.toCallLogEntry() }
                    ?: emptyList()
                trySend(calls)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Insert a call log entry into Firestore.
     */
    fun insertCallLog(uid: String, entry: CallLogEntry): Flow<Unit> = flow {
        callLogCollection(uid).document(entry.id)
            .set(entry.toFirestoreMap()).await()
        emit(Unit)
    }

    /**
     * Clear the entire call history for the given user.
     */
    fun clearCallHistory(uid: String): Flow<Unit> = flow {
        val snapshot = callLogCollection(uid).get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
        emit(Unit)
    }

    // ---------------------------------------------------------------------------
    // File upload
    // ---------------------------------------------------------------------------

    /**
     * Upload a file to Firebase Storage and return the download URL.
     *
     * @param storagePath The path in Firebase Storage (e.g., "avatars/uid/profile.jpg").
     * @param uri The local URI of the file to upload.
     * @return The download URL string.
     */
    suspend fun uploadFile(storagePath: String, uri: Uri): String {
        val ref = storage.reference.child(storagePath)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // ---------------------------------------------------------------------------
    // Session management
    // ---------------------------------------------------------------------------

    /**
     * Observe all active sessions for the given user in real-time.
     */
    fun observeActiveSessions(uid: String): Flow<List<Session>> = callbackFlow {
        val subscription = sessionsCollection(uid)
            .orderBy("lastActive", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { it.toSession() }
                    ?: emptyList()
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

    // ---------------------------------------------------------------------------
    // Mapping helpers – DocumentSnapshot → Domain model
    // ---------------------------------------------------------------------------

    private fun DocumentSnapshot.toUser(): User? {
        return try {
            User(
                uid = id,
                displayName = getString("displayName") ?: return null,
                email = getString("email") ?: "",
                photoUrl = getString("photoUrl"),
                phoneNumber = getString("phoneNumber"),
                isOnline = getBoolean("isOnline") ?: false,
                lastSeen = getLong("lastSeen") ?: 0L,
                createdAt = getLong("createdAt") ?: 0L,
                bio = getString("bio"),
                fcmToken = getString("fcmToken")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map DocumentSnapshot to User")
            null
        }
    }

    private fun DocumentSnapshot.toChatThread(): ChatThread? {
        return try {
            ChatThread(
                id = id,
                participantUids = (get("participantUids") as? List<String>)
                    ?: emptyList(),
                lastMessage = getString("lastMessage"),
                lastMessageTimestamp = getLong("lastMessageTimestamp")
                    ?.let { Instant.ofEpochMilli(it) },
                unreadCount = getLong("unreadCount")?.toInt() ?: 0,
                isPinned = getBoolean("isPinned") ?: false,
                isMuted = getBoolean("isMuted") ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map DocumentSnapshot to ChatThread")
            null
        }
    }

    private fun DocumentSnapshot.toMessage(): Message? {
        return try {
            val typeString = getString("type") ?: "TEXT"
            Message(
                id = id,
                threadId = getString("threadId") ?: return null,
                senderUid = getString("senderUid") ?: return null,
                content = getString("content") ?: "",
                timestamp = getLong("timestamp")?.let { Instant.ofEpochMilli(it) }
                    ?: Instant.now(),
                isRead = getBoolean("isRead") ?: false,
                type = runCatching { MessageType.valueOf(typeString) }
                    .getOrDefault(MessageType.TEXT),
                mediaUrl = getString("mediaUrl")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to map DocumentSnapshot to Message")
            null
        }
    }

    private fun DocumentSnapshot.toCallLogEntry(): CallLogEntry? {
        return try {
            CallLogEntry(
                id = id,
                callerUid = getString("callerUid") ?: return null,
                calleeUid = getString("calleeUid") ?: return null,
                callerName = getString("callerName") ?: "",
                calleeName = getString("calleeName") ?: "",
                callerAvatar = getString("callerAvatar"),
                calleeAvatar = getString("calleeAvatar"),
                type = getString("type")?.let { runCatching { CallDirection.valueOf(it) }.getOrNull() }
                    ?: CallDirection.OUTGOING,
                callType = getString("callType")?.let { runCatching { CallTechnology.valueOf(it) }.getOrNull() }
                    ?: CallTechnology.WEBRTC_AUDIO,
                duration = getLong("duration") ?: 0L,
                timestamp = getLong("timestamp")?.let { Instant.ofEpochMilli(it) }
                    ?: Instant.now(),
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

// ---------------------------------------------------------------------------
// Domain model extensions for Firestore serialization
// ---------------------------------------------------------------------------

private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
    "displayName" to displayName,
    "email" to email,
    "photoUrl" to photoUrl,
    "phoneNumber" to phoneNumber,
    "isOnline" to isOnline,
    "lastSeen" to lastSeen,
    "createdAt" to createdAt,
    "bio" to bio,
    "fcmToken" to fcmToken
)

private fun Message.toFirestoreMap(): Map<String, Any?> = mapOf(
    "threadId" to threadId,
    "senderUid" to senderUid,
    "content" to content,
    "timestamp" to timestamp.toEpochMilli(),
    "type" to type.name,
    "isRead" to isRead,
    "mediaUrl" to mediaUrl
)

private fun CallLogEntry.toFirestoreMap(): Map<String, Any?> = mapOf(
    "callerUid" to callerUid,
    "calleeUid" to calleeUid,
    "callerName" to callerName,
    "calleeName" to calleeName,
    "callerAvatar" to callerAvatar,
    "calleeAvatar" to calleeAvatar,
    "type" to type.name,
    "callType" to callType.name,
    "duration" to duration,
    "timestamp" to timestamp.toEpochMilli(),
    "isRead" to isRead
)
