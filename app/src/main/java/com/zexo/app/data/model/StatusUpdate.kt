package com.zexo.app.data.model

data class StatusUpdate(
    val statusId: String = "",
    val creatorZixoNumber: String = "",
    val creatorDisplayName: String = "",
    val creatorAvatarUrl: String = "",
    val timestamp: Long = 0L,
    val textContent: String? = null,
    val mediaUrl: String? = null,
    val overlaySymbols: List<String> = emptyList(),
    val backgroundColorHex: String = "#1A2A32",
    val reactions: Map<String, Int> = emptyMap(),
    val viewedBy: List<String> = emptyList(),
    val expiresInHours: Int = 24,
    val isExpired: Boolean = false
)
