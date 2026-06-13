package com.zixo.app.data.model

data class CallRecord(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val type: String = "audio",
    val direction: String = "outgoing",
    val duration: Long = 0L,
    val timestamp: Long = 0L
) {
    fun isMissed(): Boolean = direction == "missed"
    fun isIncoming(): Boolean = direction == "incoming"
    fun isOutgoing(): Boolean = direction == "outgoing"
    fun isVideo(): Boolean = type == "video"
}
