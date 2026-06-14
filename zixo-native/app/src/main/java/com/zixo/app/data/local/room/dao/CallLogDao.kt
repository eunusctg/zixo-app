package com.zixo.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zixo.app.data.local.room.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_log ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE type = :type ORDER BY timestamp DESC")
    fun getCallsByType(type: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE type = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCalls(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(calls: List<CallLogEntity>)

    @Query("DELETE FROM call_log WHERE id = :id")
    suspend fun deleteCall(id: String)

    @Query("DELETE FROM call_log")
    suspend fun deleteAllCalls()

    @Query("SELECT COUNT(*) FROM call_log")
    fun getCallCount(): Flow<Int>
}
