package com.zixo.app.domain.repository

import com.zixo.app.domain.model.AddContactState
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.ContactSearchResult
import kotlinx.coroutines.flow.Flow

/**
 * Contact Repository Interface — Zero-Trust Contact Management
 *
 * Enforces the contact-gated communication whitelist:
 * - Users can ONLY find other users by exact 8-digit Zixo Number
 * - Communication is ONLY allowed between verified mutual contacts
 * - Non-contact requests are blocked at the repository boundary
 *   before reaching any UI or signaling layers
 *
 * All operations run on Dispatchers.IO and never block the Main Thread.
 */
interface ContactRepository {

    /**
     * Searches for a user by their exact 8-digit Zixo Number.
     *
     * This is the ONLY search mechanism available. No text name search,
     * username search, or phone number lookup is permitted by design.
     *
     * @param zixoNumber The exact 8-digit Zixo Number to search for.
     * @return A [ContactSearchResult] flow indicating the outcome of the search.
     */
    fun searchByZixoNumber(zixoNumber: String): Flow<ContactSearchResult>

    /**
     * Adds a contact by their UID after a successful search.
     *
     * This performs an atomic two-way Firestore Batch write:
     * - Writes a verified link to the current user's /contacts/ subcollection
     * - Writes a reverse verified link to the target user's /contacts/ subcollection
     *
     * If either write fails, the entire transaction rolls back to prevent
     * single-sided sync states. The contact is only considered "mutual"
     * when the other user also adds back.
     *
     * @param targetUid The UID of the user to add as a contact.
     * @return An [AddContactState] flow tracking the operation progress.
     */
    fun addContact(targetUid: String): Flow<AddContactState>

    /**
     * Adds a contact by their 8-digit Zixo Number.
     *
     * This is the primary user-facing entry point for adding contacts.
     * It combines the search (by Zixo Number) and the atomic mutual
     * write into a single operation, eliminating the need for the caller
     * to first search then add separately.
     *
     * If the Zixo Number format is invalid, not found, or the user tries
     * to add their own number, an appropriate error is returned.
     *
     * @param currentUserId The currently authenticated user's UID.
     * @param zixoNumber The exact 8-digit Zixo Number of the target user.
     * @return A [Result] containing the [ContactModel] on success, or an exception on failure.
     */
    suspend fun addContactByZixoNumber(currentUserId: String, zixoNumber: String): Result<ContactModel>

    /**
     * Removes a contact from the current user's contact list.
     * Breaks the mutual relationship if it existed.
     *
     * @param contactUid The UID of the contact to remove.
     * @return A flow emitting Result success or failure.
     */
    fun removeContact(contactUid: String): Flow<Result<Unit>>

    /**
     * Observes the current user's complete contact list in real-time.
     * Uses Firestore addSnapshotListener for continuous synchronization.
     *
     * @return A flow emitting the current list of [ContactModel] entries.
     */
    fun getContacts(): Flow<List<ContactModel>>

    /**
     * Observes the current user's contacts in real-time with continuous
     * Firestore snapshot listeners. Alias for [getContacts] with explicit
     * naming to indicate real-time behavior.
     *
     * @return A flow emitting the current list of [ContactModel] entries.
     */
    fun observeContactsRealtime(): Flow<List<ContactModel>>

    /**
     * Checks whether communication is allowed with a specific user.
     *
     * This is the primary gatekeeper for the zero-trust model. Before ANY
     * message, call, or status delivery, this method MUST be called to
     * verify that the target user is a verified mutual contact.
     *
     * @param targetUid The UID of the user to check communication access for.
     * @return A [CommunicationGate] flow indicating whether communication is allowed.
     */
    fun verifyMutualContact(targetUid: String): Flow<CommunicationGate>

    /**
     * Blocks a contact. Blocked contacts cannot send messages, call,
     * or view the user's status updates.
     *
     * @param contactUid The UID of the contact to block.
     * @return A flow emitting Result success or failure.
     */
    fun blockContact(contactUid: String): Flow<Result<Unit>>

    /**
     * Unblocks a previously blocked contact.
     *
     * @param contactUid The UID of the contact to unblock.
     * @return A flow emitting Result success or failure.
     */
    fun unblockContact(contactUid: String): Flow<Result<Unit>>

    /**
     * Observes the list of blocked contacts in real-time.
     *
     * @return A flow emitting the current list of blocked [ContactModel] entries.
     */
    fun getBlockedContacts(): Flow<List<ContactModel>>
}
