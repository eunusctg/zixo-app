package com.zixo.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Zero-Trust Contact Model — Enforces mutual contact verification
 * across all communication channels in the Zixo application.
 *
 * No user may send messages, initiate calls, or view status updates
 * from any user who is not a verified mutual contact. This model
 * represents the bidirectional relationship binding that must exist
 * before any communication channel is unlocked.
 */

/**
 * Represents a verified mutual contact relationship.
 *
 * A contact is only considered "mutual" when both [userId] and
 * [contactUserId] have explicitly added each other. The [isMutual]
 * flag is enforced at the repository boundary — any attempt to
 * communicate with a non-mutual contact is blocked before reaching
 * the UI or signaling layers.
 */
data class ContactModel(
    val id: String = "",                       // Composite key: "${lesserUid}_${greaterUid}"
    val userId: String = "",                   // The owning user's UID
    val contactUserId: String = "",            // The contact's UID
    val contactDisplayName: String = "",       // Contact's display name (denormalized from profile)
    val contactUsername: String = "",          // System-generated username (read-only)
    val contactZixoNumber: String = "",        // System-generated 8-digit number (read-only)
    val contactAvatarUrl: String = "",         // Contact avatar URL
    val contactBio: String = "",               // Contact bio/about
    val isMutual: Boolean = false,             // True only when BOTH users have added each other
    val addedAt: Long = 0L,                    // Timestamp when THIS user added the contact
    val mutualVerifiedAt: Long? = null,        // Timestamp when mutual status was confirmed
    val isBlocked: Boolean = false,            // Whether this contact has been blocked
    val isPinned: Boolean = false,             // Whether this contact is pinned to top
    val isMuted: Boolean = false,              // Whether notifications are muted for this contact
    val lastSeenTimestamp: Long = 0L,          // Contact's last seen timestamp
    val isOnline: Boolean = false              // Contact's real-time online status
) {
    /**
     * Format the contact's 8-digit Zixo number for display.
     * e.g., "12345678" → "1234 5678"
     */
    val formattedZixoNumber: String
        get() = if (contactZixoNumber.length == 8) {
            "${contactZixoNumber.substring(0, 4)} ${contactZixoNumber.substring(4, 8)}"
        } else {
            contactZixoNumber
        }
}

/**
 * Represents the result of a Zixo Number search query.
 *
 * The search system is zero-trust: users can ONLY find other users
 * by entering an exact 8-digit Zixo Number. No text search, username
 * search, or phone number lookup is permitted.
 */
sealed class ContactSearchResult {
    /** No search has been initiated yet. */
    data object Idle : ContactSearchResult()

    /** The search query is in progress. */
    data object Searching : ContactSearchResult()

    /** The Zixo Number was not found in the system. */
    data object NotFound : ContactSearchResult()

    /** The query does not match a valid 8-digit Zixo Number format. */
    data class InvalidFormat(val message: String = "Enter a valid 8-digit Zixo Number") : ContactSearchResult()

    /**
     * A matching user profile was found.
     *
     * The [previewProfile] contains only the information that is
     * visible to non-contacts (display name, avatar, Zixo Number).
     * Sensitive fields like email and phone number are excluded.
     */
    data class Found(val previewProfile: ContactPreviewProfile) : ContactSearchResult()

    /** The search failed due to a network or server error. */
    data class Error(val message: String) : ContactSearchResult()
}

/**
 * A limited profile preview returned when a user is found via Zixo Number search.
 *
 * This is intentionally restricted to only the information that should
 * be visible to a non-contact user before they decide to add the contact.
 * Email, phone number, and other sensitive fields are NEVER exposed.
 */
data class ContactPreviewProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val zixoNumber: String = "",
    val avatarUrl: String = "",
    val bio: String = ""
) {
    val formattedZixoNumber: String
        get() = if (zixoNumber.length == 8) {
            "${zixoNumber.substring(0, 4)} ${zixoNumber.substring(4, 8)}"
        } else {
            zixoNumber
        }
}

/**
 * Represents the state of an "Add Contact" operation.
 */
sealed class AddContactState {
    data object Idle : AddContactState()
    data object Adding : AddContactState()
    data class Success(val contact: ContactModel) : AddContactState()
    data class AlreadyAdded(val contact: ContactModel) : AddContactState()
    data class Error(val message: String) : AddContactState()
}

/**
 * Communication gate check result — enforced at the repository boundary.
 *
 * Before ANY message, call, or status delivery, the repository must
 * verify that the target user is a mutual contact. This sealed class
 * represents the outcome of that verification.
 */
sealed class CommunicationGate {
    /** Communication is allowed — the users are verified mutual contacts. */
    data class Allowed(val contact: ContactModel) : CommunicationGate()

    /** Communication is blocked — the users are NOT mutual contacts. */
    data class Blocked(val reason: String) : CommunicationGate()

    /** The gate check failed due to a network or database error. */
    data class Error(val message: String) : CommunicationGate()
}
