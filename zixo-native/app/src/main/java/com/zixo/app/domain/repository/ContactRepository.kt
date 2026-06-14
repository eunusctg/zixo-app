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
     * @return A [ContactSearchResult] indicating the outcome of the search.
     */
    fun searchByZixoNumber(zixoNumber: String): Flow<ContactSearchResult>

    /**
     * Adds a contact by their UID after a successful search.
     *
     * This records a one-directional "add" in Firestore. The contact
     * is only considered "mutual" when the other user also adds back.
     * The [ContactModel.isMutual] flag is updated automatically when
     * both sides have confirmed.
     *
     * @param contactUid The UID of the user to add as a contact.
     * @return A [AddContactState] flow tracking the operation progress.
     */
    fun addContact(contactUid: String): Flow<AddContactState>

    /**
     * Observes the current user's complete contact list in real-time.
     *
     * Uses Firestore [addSnapshotListener] for continuous synchronization.
     * Changes to contact profiles (name, avatar, online status) propagate
     * instantly through the active Kotlin StateFlow pipeline.
     *
     * @return A flow emitting the current list of [ContactModel] entries.
     */
    fun observeContacts(): Flow<List<ContactModel>>

    /**
     * Observes a specific contact's real-time profile updates.
     *
     * @param contactUid The UID of the contact to observe.
     * @return A flow emitting the contact's current [ContactModel], or null.
     */
    fun observeContact(contactUid: String): Flow<ContactModel?>

    /**
     * Checks whether communication is allowed with a specific user.
     *
     * This is the primary gatekeeper for the zero-trust model. Before ANY
     * message, call, or status delivery, this method MUST be called to
     * verify that the target user is a verified mutual contact.
     *
     * @param targetUid The UID of the user to check communication access for.
     * @return A [CommunicationGate] indicating whether communication is allowed.
     */
    suspend fun checkCommunicationGate(targetUid: String): CommunicationGate

    /**
     * Removes a contact from the current user's contact list.
     *
     * This breaks the mutual relationship if it existed. After removal,
     * communication is immediately blocked by [checkCommunicationGate].
     *
     * @param contactUid The UID of the contact to remove.
     */
    suspend fun removeContact(contactUid: String)

    /**
     * Blocks a contact. Blocked contacts cannot send messages, call,
     * or view the user's status updates.
     *
     * @param contactUid The UID of the contact to block.
     */
    suspend fun blockContact(contactUid: String)

    /**
     * Unblocks a previously blocked contact.
     *
     * @param contactUid The UID of the contact to unblock.
     */
    suspend fun unblockContact(contactUid: String)

    /**
     * Observes the list of blocked contacts in real-time.
     *
     * @return A flow emitting the current list of blocked [ContactModel] entries.
     */
    fun observeBlockedContacts(): Flow<List<ContactModel>>

    /**
     * Pins or unpins a contact to the top of the contact list.
     *
     * @param contactUid The UID of the contact to pin/unpin.
     * @param isPinned Whether the contact should be pinned.
     */
    suspend fun setContactPinned(contactUid: String, isPinned: Boolean)

    /**
     * Mutes or unmutes notifications for a specific contact.
     *
     * @param contactUid The UID of the contact to mute/unmute.
     * @param isMuted Whether the contact should be muted.
     */
    suspend fun setContactMuted(contactUid: String, isMuted: Boolean)

    /**
     * Returns the total number of mutual contacts.
     *
     * @return A flow emitting the current mutual contact count.
     */
    fun getMutualContactCount(): Flow<Int>
}
