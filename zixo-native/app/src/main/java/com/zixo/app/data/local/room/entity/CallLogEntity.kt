package com.zixo.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zixo.app.domain.model.CallDirection
import com.zixo.app.domain.model.CallLogEntry

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
    val callId: String = "",
    val callerUid: String,
    val calleeUid: String,
    val callerName: String,
    val calleeName: String,
    val callerAvatar: String?,
    val calleeAvatar: String?,
    val type: String,            // CallDirection name
    val isVideoCall: Boolean = false,
    val isGroupCall: Boolean = false,
    val duration: Long = 0L,
    val timestamp: Long,         // Epoch millis
    val endReason: String = "COMPLETED",
    val threadId: String = "",
    val isRead: Boolean = false
)

fun CallLogEntity.toDomain(): CallLogEntry = CallLogEntry(
    id = id,
    callId = callId,
    callerUid = callerUid,
    calleeUid = calleeUid,
    callerName = callerName,
    calleeName = calleeName,
    callerAvatar = callerAvatar,
    calleeAvatar = calleeAvatar,
    type = try { CallDirection.valueOf(type) } catch (_: Exception) { CallDirection.OUTGOING },
    isVideoCall = isVideoCall,
    isGroupCall = isGroupCall,
    duration = duration,
    timestamp = timestamp,
    endReason = try { com.zixo.app.domain.model.CallEndReason.valueOf(endReason) }
        catch (_: Exception) { com.zixo.app.domain.model.CallEndReason.COMPLETED },
    threadId = threadId,
    isRead = isRead
)

fun CallLogEntry.toEntity(): CallLogEntity = CallLogEntity(
    id = id,
    callId = callId,
    callerUid = callerUid,
    calleeUid = calleeUid,
    callerName = callerName,
    calleeName = calleeName,
    callerAvatar = callerAvatar,
    calleeAvatar = calleeAvatar,
    type = type.name,
    isVideoCall = isVideoCall,
    isGroupCall = isGroupCall,
    duration = duration,
    timestamp = timestamp,
    endReason = endReason.name,
    threadId = threadId,
    isRead = isRead
)
