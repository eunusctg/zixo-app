package com.zixo.app.domain.model

/**
 * Call Log Model — Pure WebRTC call history (NO LiveKit).
 *
 * All call entries use WebRTC with Firebase Realtime DB signaling.
 * The SIP/LiveKit technology types have been removed.
 */

data class CallLogEntry(
    val id: String = "",
    val callId: String = "",
    val callerUid: String = "",
    val calleeUid: String = "",
    val callerName: String = "",
    val calleeName: String = "",
    val callerAvatar: String? = null,
    val calleeAvatar: String? = null,
    val type: CallDirection = CallDirection.OUTGOING,
    val isVideoCall: Boolean = false,
    val callType: CallTechnology = CallTechnology.WEBRTC_AUDIO,
    val isGroupCall: Boolean = false,
    val duration: Long = 0L,             // Duration in seconds
    val timestamp: Long = 0L,            // Epoch milliseconds
    val endReason: CallEndReason = CallEndReason.COMPLETED,
    val threadId: String = "",
    val isRead: Boolean = false
)

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallFilter {
    ALL,
    MISSED,
    OUTGOING,
    INCOMING
}

enum class CallTechnology {
    WEBRTC_AUDIO,
    WEBRTC_VIDEO,
    SIP
}
