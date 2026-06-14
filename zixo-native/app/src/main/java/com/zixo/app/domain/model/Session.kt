package com.zixo.app.domain.model

/**
 * Represents an active user session across devices.
 * Stored in Firestore under users/{uid}/sessions/{sessionId}.
 */
data class Session(
    val id: String,
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
    val ipAddress: String? = null,
    val lastActive: Long,
    val isActive: Boolean = true,
    val createdAt: Long
)
