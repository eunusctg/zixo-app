package com.zixo.app.data.repository

import com.zixo.app.data.local.room.dao.CallLogDao
import com.zixo.app.data.local.room.entity.toDomain
import com.zixo.app.data.local.room.entity.toEntity
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import com.zixo.app.domain.model.CallFilter
import com.zixo.app.domain.model.CallLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val callLogDao: CallLogDao,
    private val firestoreService: FirestoreService,
    private val firebaseAuthService: FirebaseAuthService
) {

    /**
     * Observes all call log entries from the local Room database,
     * ordered by timestamp descending.
     */
    fun getAllCalls(): Flow<List<CallLogEntry>> =
        callLogDao.getAllCalls().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Observes call log entries filtered by the specified [CallFilter].
     *
     * @param filter The filter to apply (e.g., [CallFilter.ALL], [CallFilter.MISSED]).
     * @return A [Flow] of filtered [CallLogEntry] lists.
     */
    fun getCallsByFilter(filter: CallFilter): Flow<List<CallLogEntry>> =
        when (filter) {
            CallFilter.ALL -> callLogDao.getAllCalls()
            CallFilter.INCOMING -> callLogDao.getCallsByType("INCOMING")
            CallFilter.OUTGOING -> callLogDao.getCallsByType("OUTGOING")
            CallFilter.MISSED -> callLogDao.getCallsByType("MISSED")
        }.map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Inserts a call log entry into both the local Room database and Firestore.
     *
     * @param entry The [CallLogEntry] to insert.
     */
    suspend fun insertCall(entry: CallLogEntry) {
        callLogDao.insertCall(entry.toEntity())
        val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
        firestoreService.insertCallLog(uid, entry).first()
    }

    /**
     * Clears the entire call history from both the local Room database and Firestore.
     */
    suspend fun clearCallHistory() {
        callLogDao.deleteAllCalls()
        val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
        firestoreService.clearCallHistory(uid).first()
    }

    /**
     * Performs a full sync of call log entries from Firestore into the local
     * Room database, replacing all local entries with the remote data.
     */
    suspend fun syncCallsFromRemote() {
        val uid = firebaseAuthService.getCurrentUser()?.uid ?: return
        val remoteCalls = firestoreService.getCallLogs(uid).first()
        val entities = remoteCalls.map { it.toEntity() }
        callLogDao.deleteAllCalls()
        callLogDao.insertAll(entities)
    }
}
