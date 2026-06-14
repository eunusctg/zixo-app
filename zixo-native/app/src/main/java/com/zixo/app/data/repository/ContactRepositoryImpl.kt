package com.zixo.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zixo.app.domain.model.AddContactState
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.ContactModel
import com.zixo.app.domain.model.ContactPreviewProfile
import com.zixo.app.domain.model.ContactSearchResult
import com.zixo.app.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [ContactRepository].
 *
 * Manages the zero-trust contact model using Firestore sub-collections
 * under `users/{uid}/contacts/`. All Firebase operations run on
 * [Dispatchers.IO] and are wrapped in try/catch to prevent crashes.
 *
 * Communication is ONLY allowed between verified mutual contacts. The
 * [checkCommunicationGate] method is the primary boundary that enforces
 * this rule across all messaging, calling, and status features.
 */
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ContactRepository {

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    private fun contactsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("contacts")

    private fun usersCollection() = firestore.collection("users")

    // ── Search ────────────────────────────────────────────────────────────────

    override fun searchByZixoNumber(zixoNumber: String): Flow<ContactSearchResult> = flow {
        if (!zixoNumber.matches(Regex("^\\d{8}$"))) {
            emit(ContactSearchResult.InvalidFormat())
            return@flow
        }

        val myUid = currentUid
        if (myUid == null) {
            emit(ContactSearchResult.Error("Not authenticated"))
            return@flow
        }

        try {
            val snapshot = usersCollection()
                .whereEqualTo("zixoNumber", zixoNumber)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                emit(ContactSearchResult.NotFound)
                return@flow
            }

            val doc = snapshot.documents[0]
            val foundUid = doc.id

            if (foundUid == myUid) {
                emit(ContactSearchResult.NotFound)
                return@flow
            }

            val preview = ContactPreviewProfile(
                uid = foundUid,
                displayName = doc.getString("displayName") ?: "",
                username = doc.getString("username") ?: "",
                zixoNumber = doc.getString("zixoNumber") ?: "",
                avatarUrl = doc.getString("photoUrl") ?: "",
                bio = doc.getString("bio") ?: ""
            )

            emit(ContactSearchResult.Found(preview))
        } catch (e: Exception) {
            Timber.e(e, "Failed to search by Zixo Number: %s", zixoNumber)
            emit(ContactSearchResult.Error(e.localizedMessage ?: "Search failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Add Contact ───────────────────────────────────────────────────────────

    override fun addContact(contactUid: String): Flow<AddContactState> = flow {
        val myUid = currentUid
        if (myUid == null) {
            emit(AddContactState.Error("Not authenticated"))
            return@flow
        }

        emit(AddContactState.Adding)

        try {
            val myContactDoc = contactsCollection(myUid).document(contactUid).get().await()
            if (myContactDoc.exists()) {
                val existing = mapToContactModel(myContactDoc, myUid)
                emit(AddContactState.AlreadyAdded(existing))
                return@flow
            }

            val contactProfile = usersCollection().document(contactUid).get().await()
            val myProfile = usersCollection().document(myUid).get().await()

            val reverseDoc = contactsCollection(contactUid).document(myUid).get().await()
            val isMutual = reverseDoc.exists()

            val now = System.currentTimeMillis()

            val myContactData = hashMapOf(
                "userId" to myUid,
                "contactUserId" to contactUid,
                "contactDisplayName" to (contactProfile.getString("displayName") ?: ""),
                "contactUsername" to (contactProfile.getString("username") ?: ""),
                "contactZixoNumber" to (contactProfile.getString("zixoNumber") ?: ""),
                "contactAvatarUrl" to (contactProfile.getString("photoUrl") ?: ""),
                "contactBio" to (contactProfile.getString("bio") ?: ""),
                "isMutual" to isMutual,
                "addedAt" to now,
                "mutualVerifiedAt" to if (isMutual) now else null,
                "isBlocked" to false,
                "isPinned" to false,
                "isMuted" to false
            )

            val reverseContactData = hashMapOf(
                "userId" to contactUid,
                "contactUserId" to myUid,
                "contactDisplayName" to (myProfile.getString("displayName") ?: ""),
                "contactUsername" to (myProfile.getString("username") ?: ""),
                "contactZixoNumber" to (myProfile.getString("zixoNumber") ?: ""),
                "contactAvatarUrl" to (myProfile.getString("photoUrl") ?: ""),
                "contactBio" to (myProfile.getString("bio") ?: ""),
                "isMutual" to true,
                "addedAt" to now,
                "mutualVerifiedAt" to now,
                "isBlocked" to false,
                "isPinned" to false,
                "isMuted" to false
            )

            val batch = firestore.batch()
            batch.set(contactsCollection(myUid).document(contactUid), myContactData)
            batch.set(contactsCollection(contactUid).document(myUid), reverseContactData)

            if (isMutual) {
                batch.update(contactsCollection(contactUid).document(myUid), "isMutual", true)
                batch.update(
                    contactsCollection(contactUid).document(myUid),
                    "mutualVerifiedAt", now
                )
            }

            batch.commit().await()

            val contact = ContactModel(
                id = generateCompositeKey(myUid, contactUid),
                userId = myUid,
                contactUserId = contactUid,
                contactDisplayName = contactProfile.getString("displayName") ?: "",
                contactUsername = contactProfile.getString("username") ?: "",
                contactZixoNumber = contactProfile.getString("zixoNumber") ?: "",
                contactAvatarUrl = contactProfile.getString("photoUrl") ?: "",
                contactBio = contactProfile.getString("bio") ?: "",
                isMutual = isMutual,
                addedAt = now,
                mutualVerifiedAt = if (isMutual) now else null
            )

            emit(AddContactState.Success(contact))
        } catch (e: Exception) {
            Timber.e(e, "Failed to add contact: %s", contactUid)
            emit(AddContactState.Error(e.localizedMessage ?: "Failed to add contact"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Observe Contacts ──────────────────────────────────────────────────────

    override fun observeContacts(): Flow<List<ContactModel>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = contactsCollection(myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing contacts")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val contacts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToContactModel(doc, myUid)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map contact document: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(contacts)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    override fun observeContact(contactUid: String): Flow<ContactModel?> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val subscription = contactsCollection(myUid).document(contactUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing contact: %s", contactUid)
                    trySend(null)
                    return@addSnapshotListener
                }

                trySend(
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            mapToContactModel(snapshot, myUid)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to map contact document: %s", contactUid)
                            null
                        }
                    } else {
                        null
                    }
                )
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Communication Gate ────────────────────────────────────────────────────

    override suspend fun checkCommunicationGate(targetUid: String): CommunicationGate =
        withContext(Dispatchers.IO) {
            val myUid = currentUid
            if (myUid == null) {
                return@withContext CommunicationGate.Error("Not authenticated")
            }

            try {
                val myContactDoc = contactsCollection(myUid).document(targetUid).get().await()
                if (!myContactDoc.exists()) {
                    return@withContext CommunicationGate.Blocked(
                        "Not a contact — communication denied"
                    )
                }

                val isMutualFromMySide = myContactDoc.getBoolean("isMutual") ?: false

                val reverseDoc = contactsCollection(targetUid).document(myUid).get().await()
                val isMutualFromOtherSide = reverseDoc.exists() &&
                    (reverseDoc.getBoolean("isMutual") ?: false)

                if (isMutualFromMySide && isMutualFromOtherSide) {
                    val contact = mapToContactModel(myContactDoc, myUid)
                    return@withContext CommunicationGate.Allowed(contact)
                }

                return@withContext CommunicationGate.Blocked(
                    "Mutual contact verification failed — communication denied"
                )
            } catch (e: Exception) {
                Timber.e(e, "Communication gate check failed for: %s", targetUid)
                return@withContext CommunicationGate.Error(
                    e.localizedMessage ?: "Gate check failed"
                )
            }
        }

    // ── Remove Contact ────────────────────────────────────────────────────────

    override suspend fun removeContact(contactUid: String) {
        try {
            val myUid = currentUid ?: return

            val batch = firestore.batch()
            batch.delete(contactsCollection(myUid).document(contactUid))
            batch.update(
                contactsCollection(contactUid).document(myUid),
                "isMutual", false,
                "mutualVerifiedAt", null
            )
            batch.commit().await()

            Timber.d("Contact removed: %s", contactUid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove contact: %s", contactUid)
        }
    }

    // ── Block / Unblock ───────────────────────────────────────────────────────

    override suspend fun blockContact(contactUid: String) {
        try {
            val myUid = currentUid ?: return
            contactsCollection(myUid).document(contactUid)
                .update("isBlocked", true)
                .await()
            Timber.d("Contact blocked: %s", contactUid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to block contact: %s", contactUid)
        }
    }

    override suspend fun unblockContact(contactUid: String) {
        try {
            val myUid = currentUid ?: return
            contactsCollection(myUid).document(contactUid)
                .update("isBlocked", false)
                .await()
            Timber.d("Contact unblocked: %s", contactUid)
        } catch (e: Exception) {
            Timber.e(e, "Failed to unblock contact: %s", contactUid)
        }
    }

    override fun observeBlockedContacts(): Flow<List<ContactModel>> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = contactsCollection(myUid)
            .whereEqualTo("isBlocked", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing blocked contacts")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val blocked = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapToContactModel(doc, myUid)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to map blocked contact: %s", doc.id)
                        null
                    }
                } ?: emptyList()

                trySend(blocked)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Pin / Mute ────────────────────────────────────────────────────────────

    override suspend fun setContactPinned(contactUid: String, isPinned: Boolean) {
        try {
            val myUid = currentUid ?: return
            contactsCollection(myUid).document(contactUid)
                .update("isPinned", isPinned)
                .await()
            Timber.d("Contact pin state updated: %s → %s", contactUid, isPinned)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set contact pinned: %s", contactUid)
        }
    }

    override suspend fun setContactMuted(contactUid: String, isMuted: Boolean) {
        try {
            val myUid = currentUid ?: return
            contactsCollection(myUid).document(contactUid)
                .update("isMuted", isMuted)
                .await()
            Timber.d("Contact mute state updated: %s → %s", contactUid, isMuted)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set contact muted: %s", contactUid)
        }
    }

    // ── Mutual Contact Count ──────────────────────────────────────────────────

    override fun getMutualContactCount(): Flow<Int> = callbackFlow {
        val myUid = currentUid
        if (myUid == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val subscription = contactsCollection(myUid)
            .whereEqualTo("isMutual", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error counting mutual contacts")
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private fun mapToContactModel(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        ownerUid: String
    ): ContactModel {
        return ContactModel(
            id = generateCompositeKey(ownerUid, doc.id),
            userId = ownerUid,
            contactUserId = doc.id,
            contactDisplayName = doc.getString("contactDisplayName") ?: "",
            contactUsername = doc.getString("contactUsername") ?: "",
            contactZixoNumber = doc.getString("contactZixoNumber") ?: "",
            contactAvatarUrl = doc.getString("contactAvatarUrl") ?: "",
            contactBio = doc.getString("contactBio") ?: "",
            isMutual = doc.getBoolean("isMutual") ?: false,
            addedAt = doc.getLong("addedAt") ?: 0L,
            mutualVerifiedAt = doc.getLong("mutualVerifiedAt"),
            isBlocked = doc.getBoolean("isBlocked") ?: false,
            isPinned = doc.getBoolean("isPinned") ?: false,
            isMuted = doc.getBoolean("isMuted") ?: false
        )
    }

    private fun generateCompositeKey(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }
}
