package com.zixo.app.domain.model

/**
 * Represents a user profile stored in Firestore.
 * All fields are dynamic and fetched from the authenticated user's session state.
 *
 * Username and ZixoNumber are system-generated on account registration
 * and must remain strictly read-only throughout the interface lifecycle.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",               // System-generated, read-only
    val email: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val zixoNumber: String = "",             // System-generated 8-digit, read-only
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val blockedUsers: List<String> = emptyList(),
    val fcmToken: String? = null,
    val createdAt: Long = 0L,
    val passkeyCredentialId: String? = null, // WebAuthn passkey credential ID
    val hasPasskey: Boolean = false          // Whether the user has registered a passkey
) {
    /**
     * Format the 8-digit Zixo number as two 4-digit blocks separated by a space.
     * e.g., "12345678" → "1234 5678"
     */
    val formattedZixoNumber: String
        get() = if (zixoNumber.length == 8) {
            "${zixoNumber.substring(0, 4)} ${zixoNumber.substring(4, 8)}"
        } else {
            zixoNumber
        }
}
