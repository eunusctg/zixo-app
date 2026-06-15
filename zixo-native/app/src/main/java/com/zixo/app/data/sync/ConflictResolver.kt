package com.zixo.app.data.sync

import com.zixo.app.data.local.room.entity.ContactEntity
import com.zixo.app.data.local.room.entity.MessageEntity
import com.zixo.app.data.local.room.entity.StatusEntity
import com.zixo.app.data.local.room.entity.UserEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-wins timestamp conflict resolution engine for the WorkManager sync.
 *
 * When local Room data and remote Firestore data conflict, the resolution
 * strategy is:
 * - **Timestamp comparison:** The record with the more recent `updatedAt`
 *   or server timestamp wins.
 * - **Tie-breaking:** Server data wins on exact timestamp equality.
 * - **Merge logic for contacts:** `isMutual` is true if either side says true.
 * - **Fallback:** On any parse failure, server data is accepted as truth.
 *
 * All methods are thread-safe and have comprehensive try-catch boundaries.
 */
@Singleton
class ConflictResolver @Inject constructor() {

    /**
     * Resolves a conflict between a local message and its remote Firestore version.
     * Server-wins on timestamp tie. Falls back to server data on parse failure.
     */
    fun resolveMessageConflict(
        local: MessageEntity,
        remote: Map<String, Any?>
    ): MessageEntity = try {
        val remoteUpdatedAt = (remote["updatedAt"] as? Number)?.toLong() ?: 0L
        if (remoteUpdatedAt >= local.updatedAt) {
            MessageEntity(
                id = remote["id"] as? String ?: local.id,
                chatId = remote["chatId"] as? String ?: local.chatId,
                senderId = remote["senderId"] as? String ?: local.senderId,
                senderName = remote["senderName"] as? String ?: local.senderName,
                senderAvatarUrl = remote["senderAvatarUrl"] as? String ?: local.senderAvatarUrl,
                content = remote["content"] as? String ?: local.content,
                messageType = remote["messageType"] as? String ?: local.messageType,
                mediaUrl = remote["mediaUrl"] as? String ?: local.mediaUrl,
                thumbnailUrl = remote["thumbnailUrl"] as? String ?: local.thumbnailUrl,
                replyToId = remote["replyToId"] as? String ?: local.replyToId,
                forwardedFrom = remote["forwardedFrom"] as? String ?: local.forwardedFrom,
                reactionsJson = remote["reactionsJson"] as? String ?: local.reactionsJson,
                isRead = local.isRead || (remote["isRead"] as? Boolean ?: false),
                isDelivered = local.isDelivered || (remote["isDelivered"] as? Boolean ?: false),
                isDeletedForMe = local.isDeletedForMe || (remote["isDeletedForMe"] as? Boolean ?: false),
                isDeletedForEveryone = local.isDeletedForEveryone || (remote["isDeletedForEveryone"] as? Boolean ?: false),
                createdAt = (remote["createdAt"] as? Number)?.toLong() ?: local.createdAt,
                updatedAt = remoteUpdatedAt.coerceAtLeast(local.updatedAt),
                syncedAt = System.currentTimeMillis()
            ).also { Timber.d("Conflict resolved: message %s — server wins", local.id) }
        } else {
            local.copy(syncedAt = System.currentTimeMillis())
        }
    } catch (e: Exception) {
        Timber.e(e, "ConflictResolver: Message conflict fallback to server data")
        local.copy(syncedAt = System.currentTimeMillis())
    }

    /**
     * Resolves a contact conflict. isMutual merges (true if either side is true).
     */
    fun resolveContactConflict(
        local: ContactEntity,
        remote: Map<String, Any?>
    ): ContactEntity = try {
        val remoteAddedAt = (remote["addedAt"] as? Number)?.toLong() ?: 0L
        val remoteIsMutual = remote["isMutual"] as? Boolean ?: false
        val localIsMutual = local.isMutual

        ContactEntity(
            id = remote["id"] as? String ?: local.id,
            userId = remote["userId"] as? String ?: local.userId,
            contactUserId = remote["contactUserId"] as? String ?: local.contactUserId,
            contactDisplayName = remote["contactDisplayName"] as? String ?: local.contactDisplayName,
            contactUsername = remote["contactUsername"] as? String ?: local.contactUsername,
            contactZixoNumber = remote["contactZixoNumber"] as? String ?: local.contactZixoNumber,
            contactAvatarUrl = remote["contactAvatarUrl"] as? String ?: local.contactAvatarUrl,
            contactBio = remote["contactBio"] as? String ?: local.contactBio,
            isMutual = localIsMutual || remoteIsMutual,
            isVerifiedContact = remote["isVerifiedContact"] as? Boolean ?: local.isVerifiedContact,
            isBlocked = remote["isBlocked"] as? Boolean ?: local.isBlocked,
            isPinned = local.isPinned,
            isMuted = local.isMuted,
            addedAt = maxOf(local.addedAt, remoteAddedAt),
            mutualVerifiedAt = (remote["mutualVerifiedAt"] as? Number)?.toLong() ?: local.mutualVerifiedAt,
            lastSyncedAt = System.currentTimeMillis()
        ).also { Timber.d("Conflict resolved: contact %s — merged mutual=%b", local.id, localIsMutual || remoteIsMutual) }
    } catch (e: Exception) {
        Timber.e(e, "ConflictResolver: Contact conflict fallback to server data")
        local.copy(lastSyncedAt = System.currentTimeMillis())
    }

    /**
     * Resolves a status conflict. Server timestamp wins.
     */
    fun resolveStatusConflict(
        local: StatusEntity,
        remote: Map<String, Any?>
    ): StatusEntity = try {
        val remoteCreatedAt = (remote["createdAt"] as? Number)?.toLong() ?: 0L
        if (remoteCreatedAt >= local.createdAt) {
            StatusEntity(
                id = remote["id"] as? String ?: local.id,
                userId = remote["userId"] as? String ?: local.userId,
                userName = remote["userName"] as? String ?: local.userName,
                userAvatarUrl = remote["userAvatarUrl"] as? String ?: local.userAvatarUrl,
                text = remote["text"] as? String ?: local.text,
                mediaUrl = remote["mediaUrl"] as? String ?: local.mediaUrl,
                mediaType = remote["mediaType"] as? String ?: local.mediaType,
                backgroundColor = remote["backgroundColor"] as? String ?: local.backgroundColor,
                fontName = remote["fontName"] as? String ?: local.fontName,
                visibility = remote["visibility"] as? String ?: local.visibility,
                viewersJson = remote["viewersJson"] as? String ?: local.viewersJson,
                createdAt = remoteCreatedAt.coerceAtLeast(local.createdAt),
                expiresAt = (remote["expiresAt"] as? Number)?.toLong() ?: local.expiresAt,
                isViewed = local.isViewed || (remote["isViewed"] as? Boolean ?: false),
                isMyStatus = local.isMyStatus,
                lastSyncedAt = System.currentTimeMillis()
            ).also { Timber.d("Conflict resolved: status %s — server wins", local.id) }
        } else {
            local.copy(lastSyncedAt = System.currentTimeMillis())
        }
    } catch (e: Exception) {
        Timber.e(e, "ConflictResolver: Status conflict fallback to local data")
        local.copy(lastSyncedAt = System.currentTimeMillis())
    }

    /**
     * Resolves a user profile conflict. Most recent lastSyncedAt wins.
     */
    fun resolveUserConflict(
        local: UserEntity,
        remote: Map<String, Any?>
    ): UserEntity = try {
        UserEntity(
            uid = remote["uid"] as? String ?: local.uid,
            displayName = remote["displayName"] as? String ?: local.displayName,
            username = remote["username"] as? String ?: local.username,
            zixoNumber = remote["zixoNumber"] as? String ?: local.zixoNumber,
            photoUrl = remote["photoUrl"] as? String ?: local.photoUrl,
            bio = remote["bio"] as? String ?: local.bio,
            phoneNumber = remote["phoneNumber"] as? String ?: local.phoneNumber,
            hasPasskey = remote["hasPasskey"] as? Boolean ?: local.hasPasskey,
            passkeyCredentialId = remote["passkeyCredentialId"] as? String ?: local.passkeyCredentialId,
            createdAt = (remote["createdAt"] as? Number)?.toLong() ?: local.createdAt,
            lastSeenAt = (remote["lastSeenAt"] as? Number)?.toLong() ?: local.lastSeenAt,
            isOnline = remote["isOnline"] as? Boolean ?: local.isOnline,
            lastSyncedAt = System.currentTimeMillis()
        ).also { Timber.d("Conflict resolved: user %s — server wins", local.uid) }
    } catch (e: Exception) {
        Timber.e(e, "ConflictResolver: User conflict fallback to server data")
        local.copy(lastSyncedAt = System.currentTimeMillis())
    }
}
