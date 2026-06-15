package com.zixo.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.MyStatusState
import com.zixo.app.domain.model.StatusContentType
import com.zixo.app.domain.model.StatusGroupModel
import com.zixo.app.domain.model.StatusModel
import com.zixo.app.domain.model.StatusReaction
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [StatusRepository].
 *
 * Manages ephemeral status updates using Firestore for storage and
 * Firebase Storage for media uploads. Enforces zero-trust delivery:
 * statuses are ONLY delivered to verified mutual contacts.
 *
 * Auto-cleanup: statuses older than 24 hours are filtered out on
 * every emission. Uses continuous snapshot listeners for real-time
 * sync via [callbackFlow].
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
 */
@Singleton
class StatusRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth,
    private val contactRepository: ContactRepository
) : StatusRepository {

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    private val statusesCollection get() = firestore.collection("statuses")
    private val usersCollection get() = firestore.collection("users")

    private val twentyFourHoursMs: Long = 24 * 60 * 60 * 1000L

    // ── Get My Statuses ───────────────────────────────────────────────────────

    override fun getMyStatuses(): Flow<MyStatusState> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(MyStatusState())
            close()
            return@callbackFlow
        }

        val subscription = statusesCollection
            .whereEqualTo("senderUid", myUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing my statuses")
                    trySend(MyStatusState(errorMessage = error.localizedMessage))
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val cutoff = now - twentyFourHoursMs

                val myStatuses = snapshot?.documents?.mapNotNull { doc ->
                    try { mapToStatusModel(doc) }
                    catch (e: Exception) {
                        Timber.e(e, "Failed to map my status: %s", doc.id)
                        null
                    }
                }?.filter { it.createdAt >= cutoff && !it.isExpired }
                    ?: emptyList()

                trySend(MyStatusState(myStatuses = myStatuses))
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Get Contact Statuses ──────────────────────────────────────────────────

    override fun getContactStatuses(): Flow<List<StatusGroupModel>> = callbackFlow<StatusFeedData?> {
        val myUid = currentUid
        if (myUid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val subscription = statusesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing status feed")
                    trySend(null)
                    return@addSnapshotListener
                }

                try {
                    val now = System.currentTimeMillis()
                    val cutoff = now - twentyFourHoursMs

                    val allStatuses = snapshot?.documents?.mapNotNull { doc ->
                        try { mapToStatusModel(doc) }
                        catch (e: Exception) {
                            Timber.e(e, "Failed to map status: %s", doc.id)
                            null
                        }
                    }?.filter { status ->
                        status.senderUid != myUid &&
                            status.createdAt >= cutoff &&
                            !status.isExpired
                    } ?: emptyList()

                    trySend(StatusFeedData(allStatuses, myUid))
                } catch (e: Exception) {
                    Timber.e(e, "Error processing status feed")
                    trySend(null)
                }
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO).map { data ->
        if (data == null) return@map emptyList<StatusGroupModel>()

        // Zero-trust: only deliver statuses from verified mutual contacts
        val mutualContacts = contactRepository.getContacts().first()
            .filter { it.isMutual && !it.isBlocked }
        val mutualUids = mutualContacts.map { it.contactUserId }.toSet()

        val visibleStatuses = data.statuses.filter { status ->
            mutualUids.contains(status.senderUid)
        }

        groupStatusesBySender(visibleStatuses, mutualContacts)
    }.flowOn(Dispatchers.IO)

    override fun observeContactStatusesRealtime(): Flow<List<StatusGroupModel>> =
        getContactStatuses()

    // ── Post Status ───────────────────────────────────────────────────────────

    override fun postStatus(status: StatusModel): Flow<Result<StatusModel>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            val statusId = status.id.ifBlank { UUID.randomUUID().toString() }
            val now = System.currentTimeMillis()

            var mediaUrl = status.mediaUrl
            if (status.type != StatusContentType.TEXT && !status.mediaUrl.isNullOrBlank()) {
                // Upload media to Firebase Storage
                val fileExtension = when (status.type) {
                    StatusContentType.IMAGE -> "image.jpg"
                    StatusContentType.VIDEO -> "video.mp4"
                    else -> "media"
                }
                val storagePath = "statuses/$myUid/$statusId/$fileExtension"
                val fileUri = Uri.parse(status.mediaUrl)
                val storageRef = storage.reference.child(storagePath)
                storageRef.putFile(fileUri).await()
                mediaUrl = storageRef.downloadUrl.await().toString()
            }

            val statusData = hashMapOf(
                "id" to statusId,
                "senderUid" to myUid,
                "senderDisplayName" to status.senderDisplayName,
                "senderAvatarUrl" to status.senderAvatarUrl,
                "senderZixoNumber" to status.senderZixoNumber,
                "type" to status.type.name,
                "textContent" to status.textContent,
                "mediaUrl" to mediaUrl,
                "mediaMimeType" to status.mediaMimeType,
                "mediaThumbnailUrl" to status.mediaThumbnailUrl,
                "backgroundColor" to status.backgroundColor,
                "fontName" to status.fontName,
                "shapeType" to status.shapeType?.name,
                "emoji3dCode" to status.emoji3dCode,
                "caption" to status.caption,
                "createdAt" to now,
                "expiresAt" to now + twentyFourHoursMs,
                "viewedByUids" to emptyList<String>(),
                "reactions" to emptyList<Map<String, Any>>(),
                "isExpired" to false
            )

            statusesCollection.document(statusId).set(statusData).await()

            val postedStatus = status.copy(
                id = statusId,
                createdAt = now,
                expiresAt = now + twentyFourHoursMs,
                mediaUrl = mediaUrl
            )

            Timber.d("Status posted: %s", statusId)
            emit(Result.success(postedStatus))
        } catch (e: Exception) {
            Timber.e(e, "Failed to post status")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Delete Status ─────────────────────────────────────────────────────────

    override fun deleteStatus(statusId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            val doc = statusesCollection.document(statusId).get().await()
            val senderUid = doc.getString("senderUid") ?: ""
            if (senderUid != myUid) {
                emit(Result.failure(SecurityException("Cannot delete another user's status")))
                return@flow
            }

            statusesCollection.document(statusId).delete().await()
            Timber.d("Status deleted: %s", statusId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete status: %s", statusId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Mark Status Viewed ────────────────────────────────────────────────────

    override fun markStatusViewed(statusId: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            val doc = statusesCollection.document(statusId).get().await()
            if (!doc.exists()) {
                emit(Result.failure(IllegalStateException("Status not found")))
                return@flow
            }

            @Suppress("UNCHECKED_CAST")
            val viewedByUids = (doc.get("viewedByUids") as? List<String>) ?: emptyList()

            if (!viewedByUids.contains(myUid)) {
                statusesCollection.document(statusId)
                    .update("viewedByUids", viewedByUids + myUid)
                    .await()
            }

            Timber.d("Status viewed: %s", statusId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark status viewed: %s", statusId)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── React To Status ───────────────────────────────────────────────────────

    override fun reactToStatus(statusId: String, emoji: String): Flow<Result<Unit>> = flow {
        try {
            val myUid = currentUid ?: throw IllegalStateException("Not authenticated")

            val doc = statusesCollection.document(statusId).get().await()
            if (!doc.exists()) {
                emit(Result.failure(IllegalStateException("Status not found")))
                return@flow
            }

            // Zero-trust: verify mutual contact before allowing reaction
            val senderUid = doc.getString("senderUid") ?: ""
            if (senderUid != myUid) {
                var gateResult: CommunicationGate? = null
                contactRepository.verifyMutualContact(senderUid).collect { gateResult = it }
                if (gateResult !is CommunicationGate.Allowed) {
                    Timber.w("Cannot react to status: not a mutual contact")
                    emit(Result.failure(SecurityException("Not a mutual contact")))
                    return@flow
                }
            }

            @Suppress("UNCHECKED_CAST")
            val existingReactions = (doc.get("reactions") as? List<Map<String, Any>>)
                ?: emptyList()

            val myProfile = usersCollection.document(myUid).get().await()
            val newReaction = mapOf(
                "uid" to myUid,
                "displayName" to (myProfile.getString("displayName") ?: ""),
                "emoji" to emoji,
                "timestamp" to System.currentTimeMillis()
            )

            statusesCollection.document(statusId)
                .update("reactions", existingReactions + newReaction)
                .await()

            Timber.d("Status reaction added: %s to %s", emoji, statusId)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to react to status")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private data class StatusFeedData(
        val statuses: List<StatusModel>,
        val myUid: String
    )

    private fun groupStatusesBySender(
        statuses: List<StatusModel>,
        mutualContacts: List<com.zixo.app.domain.model.ContactModel>
    ): List<StatusGroupModel> {
        val contactMap = mutualContacts.associateBy { it.contactUserId }

        return statuses
            .groupBy { it.senderUid }
            .map { (senderUid, senderStatuses) ->
                val contact = contactMap[senderUid]
                val latestTimestamp = senderStatuses.maxOfOrNull { it.createdAt } ?: 0L
                val hasUnviewed = senderStatuses.any { !it.isExpired }

                StatusGroupModel(
                    senderUid = senderUid,
                    senderDisplayName = contact?.contactDisplayName
                        ?: senderStatuses.firstOrNull()?.senderDisplayName ?: "",
                    senderAvatarUrl = contact?.contactAvatarUrl
                        ?: senderStatuses.firstOrNull()?.senderAvatarUrl ?: "",
                    senderZixoNumber = contact?.contactZixoNumber
                        ?: senderStatuses.firstOrNull()?.senderZixoNumber ?: "",
                    statuses = senderStatuses.sortedByDescending { it.createdAt },
                    hasUnviewedStatuses = hasUnviewed,
                    latestTimestamp = latestTimestamp
                )
            }
            .sortedByDescending { it.latestTimestamp }
    }

    private fun mapToStatusModel(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): StatusModel {
        val typeStr = doc.getString("type") ?: "TEXT"
        val contentType = try { StatusContentType.valueOf(typeStr) }
            catch (_: Exception) { StatusContentType.TEXT }

        @Suppress("UNCHECKED_CAST")
        val viewedByUids = (doc.get("viewedByUids") as? List<String>)?.toSet() ?: emptySet()

        @Suppress("UNCHECKED_CAST")
        val reactionsRaw = doc.get("reactions") as? List<Map<String, Any>> ?: emptyList()

        val reactions = reactionsRaw.map { data ->
            StatusReaction(
                uid = data["uid"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                emoji = data["emoji"] as? String ?: "",
                timestamp = data["timestamp"] as? Long ?: 0L
            )
        }

        return StatusModel(
            id = doc.id,
            senderUid = doc.getString("senderUid") ?: "",
            senderDisplayName = doc.getString("senderDisplayName") ?: "",
            senderAvatarUrl = doc.getString("senderAvatarUrl") ?: "",
            senderZixoNumber = doc.getString("senderZixoNumber") ?: "",
            type = contentType,
            textContent = doc.getString("textContent"),
            mediaUrl = doc.getString("mediaUrl"),
            mediaMimeType = doc.getString("mediaMimeType"),
            mediaThumbnailUrl = doc.getString("mediaThumbnailUrl"),
            backgroundColor = doc.getString("backgroundColor"),
            fontName = doc.getString("fontName"),
            shapeType = doc.getString("shapeType")?.let {
                try { com.zixo.app.domain.model.StatusShapeType.valueOf(it) }
                catch (_: Exception) { null }
            },
            emoji3dCode = doc.getString("emoji3dCode"),
            caption = doc.getString("caption"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            expiresAt = doc.getLong("expiresAt") ?: 0L,
            viewedByUids = viewedByUids,
            reactions = reactions,
            isExpired = doc.getBoolean("isExpired") ?: false
        )
    }
}
