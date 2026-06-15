package com.zixo.app.data.mapper

import com.zixo.app.data.local.room.entity.ContactEntity
import com.zixo.app.data.local.room.entity.MessageEntity
import com.zixo.app.data.local.room.entity.StatusEntity
import com.zixo.app.data.local.room.entity.UserEntity
import timber.log.Timber

/**
 * Centralized mapping extensions between Firestore document maps
 * and Room entities. Eliminates scattered inline mapping code
 * in repository implementations.
 *
 * All functions are null-safe with default values and comprehensive
 * try-catch boundaries that return safe defaults on parse failure.
 */

// ════════════════════════════════════════════════════════════════
// Firestore Map → Room Entity Mappers
// ════════════════════════════════════════════════════════════════

/**
 * Converts a Firestore document map to a [MessageEntity].
 * Requires [chatId] since it's not stored in the message document.
 */
fun Map<String, Any?>.toMessageEntity(chatId: String): MessageEntity = try {
    MessageEntity(
        id = this["id"] as? String ?: "",
        chatId = chatId,
        senderId = this["senderId"] as? String ?: "",
        senderName = this["senderName"] as? String ?: "",
        senderAvatarUrl = this["senderAvatarUrl"] as? String ?: "",
        content = this["content"] as? String ?: "",
        messageType = this["messageType"] as? String ?: "TEXT",
        mediaUrl = this["mediaUrl"] as? String,
        thumbnailUrl = this["thumbnailUrl"] as? String,
        replyToId = this["replyToId"] as? String,
        forwardedFrom = this["forwardedFrom"] as? String,
        reactionsJson = this["reactionsJson"] as? String,
        isRead = this["isRead"] as? Boolean ?: false,
        isDelivered = this["isDelivered"] as? Boolean ?: false,
        isDeletedForMe = this["isDeletedForMe"] as? Boolean ?: false,
        isDeletedForEveryone = this["isDeletedForEveryone"] as? Boolean ?: false,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
        syncedAt = System.currentTimeMillis()
    )
} catch (e: Exception) {
    Timber.e(e, "DataMapper: Failed to map Firestore doc to MessageEntity")
    MessageEntity(
        id = "", chatId = chatId, senderId = "", senderName = "",
        senderAvatarUrl = "", content = "", messageType = "TEXT"
    )
}

/**
 * Converts a Firestore document map to a [ContactEntity].
 * The [docId] parameter is used as the entity's primary key.
 */
fun Map<String, Any?>.toContactEntity(docId: String): ContactEntity = try {
    ContactEntity(
        id = docId,
        userId = this["userId"] as? String ?: "",
        contactUserId = this["contactUserId"] as? String ?: docId,
        contactDisplayName = this["contactDisplayName"] as? String ?: "",
        contactUsername = this["contactUsername"] as? String ?: "",
        contactZixoNumber = this["contactZixoNumber"] as? String ?: "",
        contactAvatarUrl = this["contactAvatarUrl"] as? String ?: "",
        contactBio = this["contactBio"] as? String ?: "",
        isMutual = this["isMutual"] as? Boolean ?: false,
        isVerifiedContact = this["isVerifiedContact"] as? Boolean ?: true,
        isBlocked = this["isBlocked"] as? Boolean ?: false,
        isPinned = this["isPinned"] as? Boolean ?: false,
        isMuted = this["isMuted"] as? Boolean ?: false,
        addedAt = (this["addedAt"] as? Number)?.toLong() ?: 0L,
        mutualVerifiedAt = (this["mutualVerifiedAt"] as? Number)?.toLong(),
        lastSyncedAt = System.currentTimeMillis()
    )
} catch (e: Exception) {
    Timber.e(e, "DataMapper: Failed to map Firestore doc to ContactEntity")
    ContactEntity(
        id = docId, userId = "", contactUserId = docId,
        contactDisplayName = "", contactUsername = "",
        contactZixoNumber = "", contactAvatarUrl = "", contactBio = ""
    )
}

/**
 * Converts a Firestore document map to a [StatusEntity].
 */
fun Map<String, Any?>.toStatusEntity(): StatusEntity = try {
    StatusEntity(
        id = this["id"] as? String ?: "",
        userId = this["userId"] as? String ?: "",
        userName = this["userName"] as? String ?: "",
        userAvatarUrl = this["userAvatarUrl"] as? String ?: "",
        text = this["text"] as? String,
        mediaUrl = this["mediaUrl"] as? String,
        mediaType = this["mediaType"] as? String ?: "TEXT",
        backgroundColor = this["backgroundColor"] as? String,
        fontName = this["fontName"] as? String,
        visibility = this["visibility"] as? String ?: "ALL_CONTACTS",
        viewersJson = this["viewersJson"] as? String,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        expiresAt = (this["expiresAt"] as? Number)?.toLong() ?: 0L,
        isViewed = this["isViewed"] as? Boolean ?: false,
        isMyStatus = this["isMyStatus"] as? Boolean ?: false,
        lastSyncedAt = System.currentTimeMillis()
    )
} catch (e: Exception) {
    Timber.e(e, "DataMapper: Failed to map Firestore doc to StatusEntity")
    StatusEntity(
        id = "", userId = "", userName = "", userAvatarUrl = ""
    )
}

/**
 * Converts a Firestore document map to a [UserEntity].
 */
fun Map<String, Any?>.toUserEntity(): UserEntity = try {
    UserEntity(
        uid = this["uid"] as? String ?: "",
        displayName = this["displayName"] as? String ?: "",
        username = this["username"] as? String ?: "",
        zixoNumber = this["zixoNumber"] as? String ?: "",
        photoUrl = this["photoUrl"] as? String ?: "",
        bio = this["bio"] as? String ?: "",
        phoneNumber = this["phoneNumber"] as? String,
        hasPasskey = this["hasPasskey"] as? Boolean ?: false,
        passkeyCredentialId = this["passkeyCredentialId"] as? String,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        lastSeenAt = (this["lastSeenAt"] as? Number)?.toLong() ?: 0L,
        isOnline = this["isOnline"] as? Boolean ?: false,
        lastSyncedAt = System.currentTimeMillis()
    )
} catch (e: Exception) {
    Timber.e(e, "DataMapper: Failed to map Firestore doc to UserEntity")
    UserEntity(
        uid = "", displayName = "", username = "",
        zixoNumber = "", photoUrl = "", bio = ""
    )
}

// ════════════════════════════════════════════════════════════════
// Room Entity → Firestore Map Mappers
// ════════════════════════════════════════════════════════════════

fun MessageEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "chatId" to chatId,
    "senderId" to senderId,
    "senderName" to senderName,
    "content" to content,
    "messageType" to messageType,
    "mediaUrl" to mediaUrl,
    "isRead" to isRead,
    "isDelivered" to isDelivered,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

fun ContactEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "contactUserId" to contactUserId,
    "contactDisplayName" to contactDisplayName,
    "contactUsername" to contactUsername,
    "contactZixoNumber" to contactZixoNumber,
    "contactAvatarUrl" to contactAvatarUrl,
    "contactBio" to contactBio,
    "isMutual" to isMutual,
    "isVerifiedContact" to isVerifiedContact,
    "isBlocked" to isBlocked,
    "addedAt" to addedAt,
    "mutualVerifiedAt" to mutualVerifiedAt
)

fun UserEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "displayName" to displayName,
    "username" to username,
    "zixoNumber" to zixoNumber,
    "photoUrl" to photoUrl,
    "bio" to bio,
    "hasPasskey" to hasPasskey,
    "isOnline" to isOnline,
    "lastSeenAt" to lastSeenAt
)
