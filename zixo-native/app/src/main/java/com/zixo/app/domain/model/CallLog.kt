package com.zixo.app.domain.model

import java.time.Instant

data class CallLogEntry(
    val id: String,
    val callerUid: String,
    val calleeUid: String,
    val callerName: String,
    val calleeName: String,
    val callerAvatar: String?,
    val calleeAvatar: String?,
    val type: CallDirection,
    val callType: CallTechnology,
    val duration: Long = 0L,
    val timestamp: Instant,
    val isRead: Boolean = false
)

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallTechnology {
    SIP,
    WEBRTC_AUDIO,
    WEBRTC_VIDEO
}

enum class CallFilter {
    ALL,
    MISSED,
    OUTGOING,
    INCOMING
}
