package com.zixo.app.domain.usecase

import com.zixo.app.domain.model.AddContactState
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.ContactSearchResult
import com.zixo.app.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean Architecture Use Case for contact management.
 *
 * Serves as the single interaction point between ViewModels and the
 * [ContactRepository], ensuring decoupled testability and enforcing
 * the zero-trust contact verification model.
 *
 * All operations execute on [Dispatchers.IO] to prevent Main Thread blocking.
 * Comprehensive try-catch boundaries prevent any repository-level exception
 * from propagating as an unhandled crash.
 */
@Singleton
class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * Streams the current user's full contact list.
     * Backed by Firestore snapshot listeners for real-time updates.
     */
    operator fun invoke(): Flow<List<ContactModel>> = try {
        contactRepository.getContacts().flowOn(Dispatchers.IO)
    } catch (e: Exception) {
        Timber.e(e, "GetContactsUseCase: Failed to stream contacts")
        kotlinx.coroutines.flow.flowOf(emptyList())
    }

    /**
     * Streams mutual contacts only — contacts where both sides have
     * verified each other. Required for zero-trust communication gating.
     */
    fun getMutualContacts(): Flow<List<ContactModel>> = try {
        contactRepository.getMutualContacts().flowOn(Dispatchers.IO)
    } catch (e: Exception) {
        Timber.e(e, "GetContactsUseCase: Failed to stream mutual contacts")
        kotlinx.coroutines.flow.flowOf(emptyList())
    }

    /**
     * Searches for a user by their exact 8-digit Zixo Number.
     * Email-based lookups are explicitly forbidden by privacy architecture.
     *
     * @param query Must match regex ^\d{8}$ or returns [ContactSearchResult.InvalidFormat].
     */
    fun invokeSearch(query: String): Flow<ContactSearchResult> = try {
        contactRepository.searchByZixoNumber(query).flowOn(Dispatchers.IO)
    } catch (e: Exception) {
        Timber.e(e, "GetContactsUseCase: Search failed for query: %s", query)
        kotlinx.coroutines.flow.flowOf(ContactSearchResult.Error(e.localizedMessage ?: "Search failed"))
    }

    /**
     * Initiates an atomic mutual contact addition via Firestore Batch writes.
     * Both users' /contacts/ subcollections are written simultaneously.
     * If either write fails, the entire transaction rolls back.
     *
     * @param targetUid The UID of the user to add as a contact.
     */
    suspend fun invokeAddContact(targetUid: String): Flow<AddContactState> = try {
        contactRepository.addContact(targetUid).flowOn(Dispatchers.IO)
    } catch (e: Exception) {
        Timber.e(e, "GetContactsUseCase: Add contact failed for UID: %s", targetUid)
        kotlinx.coroutines.flow.flowOf(AddContactState.Error(e.localizedMessage ?: "Add contact failed"))
    }

    /**
     * Verifies that two users are mutual contacts before allowing
     * communication (messaging, calling, status viewing).
     * This is the primary zero-trust enforcement boundary.
     */
    suspend fun invokeVerifyMutualContact(targetUserId: String): Boolean = try {
        contactRepository.verifyMutualContact(targetUserId)
    } catch (e: Exception) {
        Timber.e(e, "GetContactsUseCase: Mutual verification failed for: %s", targetUserId)
        false
    }

    /**
     * Blocks or unblocks a contact. Blocked contacts cannot send messages
     * or initiate calls through the zero-trust gate.
     */
    suspend fun invokeSetBlocked(contactUserId: String, isBlocked: Boolean): Result<Unit> = try {
        contactRepository.setBlocked(contactUserId, isBlocked)
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "GetContactsUseCase: Block toggle failed")
        Result.failure(e)
    }
}
