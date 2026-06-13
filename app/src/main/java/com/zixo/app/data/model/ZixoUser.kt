package com.zixo.app.data.model

data class ZixoUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val username: String = "",
    val bio: String = "",
    val avatar: String = "",
    val phone: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L,
    val zixoNumber: String = "",
    val publicKey: String = "",
    val role: String = "user",
    val blockedUsers: List<String> = emptyList(),
    val lastSeenVisibility: String = "everyone",
    val readReceipts: Boolean = true,
    val onlineStatus: Boolean = true
)
