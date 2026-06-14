package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zixo.app.domain.model.CallDirection
import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.CallTechnology
import java.time.Instant

@Entity(
    tableName = "call_log",
    indices = [
        Index(value = ["timestamp"], name = "index_call_log_timestamp"),
        Index(value = ["type"], name = "index_call_log_type")
    ]
)
data class CallLogEntity(
    @PrimaryKey
    val id: String,
    val callerUid: String,
    val calleeUid: String,
    val callerName: String,
    val calleeName: String,
    val callerAvatar: String?,
    val calleeAvatar: String?,
    val type: String, // CallDirection name
    val callType: String, // CallTechnology name
    val duration: Long = 0L,
    val timestamp: Long, // Epoch millis
    val isRead: Boolean = false
)

fun CallLogEntity.toDomain(): CallLogEntry = CallLogEntry(
    id = id,
    callerUid = callerUid,
    calleeUid = calleeUid,
    callerName = callerName,
    calleeName = calleeName,
    callerAvatar = callerAvatar,
    calleeAvatar = calleeAvatar,
    type = CallDirection.valueOf(type),
    callType = CallTechnology.valueOf(callType),
    duration = duration,
    timestamp = Instant.ofEpochMilli(timestamp),
    isRead = isRead
)

fun CallLogEntry.toEntity(): CallLogEntity = CallLogEntity(
    id = id,
    callerUid = callerUid,
    calleeUid = calleeUid,
    callerName = callerName,
    calleeName = calleeName,
    callerAvatar = callerAvatar,
    calleeAvatar = calleeAvatar,
    type = type.name,
    callType = callType.name,
    duration = duration,
    timestamp = timestamp.toEpochMilli(),
    isRead = isRead
)
