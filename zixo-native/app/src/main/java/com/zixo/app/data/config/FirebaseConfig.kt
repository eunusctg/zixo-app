package com.zixo.app.data.config

/**
 * Firebase configuration constants and path builders.
 * Centralizes all Firestore/RTDB collection names, field names,
 * and path construction to eliminate magic strings across the codebase.
 */
object FirebaseConfig {

    // ── Collection Names ──────────────────────────────────────────
    const val USERS_COLLECTION = "users"
    const val CONTACTS_SUBCOLLECTION = "contacts"
    const val CHATS_COLLECTION = "chats"
    const val MESSAGES_SUBCOLLECTION = "messages"
    const val CALLS_COLLECTION = "calls"
    const val STATUSES_COLLECTION = "statuses"
    const val SIGNALING_PATH = "signaling"
    const val ICE_CANDIDATES_PATH = "iceCandidates"
    const val SESSIONS_SUBCOLLECTION = "sessions"
    const val PREKEYS_SUBCOLLECTION = "preKeys"

    // ── Field Names ───────────────────────────────────────────────
    const val ZIXO_NUMBER_FIELD = "zixoNumber"
    const val TIMESTAMP_FIELD = "timestamp"
    const val PARTICIPANTS_FIELD = "participants"
    const val LAST_MESSAGE_FIELD = "lastMessage"
    const val IS_ONLINE_FIELD = "isOnline"
    const val LAST_SEEN_FIELD = "lastSeenAt"

    // ── Sync Parameters ───────────────────────────────────────────
    const val SYNC_BATCH_SIZE = 50
    const val STATUS_EXPIRY_HOURS = 24
    const val SESSION_EXPIRY_DAYS = 30
    const val MAX_MESSAGE_SIZE_BYTES = 10 * 1024 * 1024

    // ── ICE Servers ───────────────────────────────────────────────
    val ICE_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302",
        "stun:stun3.l.google.com:19302",
        "stun:stun4.l.google.com:19302"
    )

    // ── Path Builders ─────────────────────────────────────────────
    fun userPath(uid: String) = "$USERS_COLLECTION/$uid"
    fun contactsPath(uid: String) = "$USERS_COLLECTION/$uid/$CONTACTS_SUBCOLLECTION"
    fun chatPath(chatId: String) = "$CHATS_COLLECTION/$chatId"
    fun messagesPath(chatId: String) = "$CHATS_COLLECTION/$chatId/$MESSAGES_SUBCOLLECTION"
    fun signalingPath(callId: String) = "$SIGNALING_PATH/$callId"
    fun iceCandidatesPath(callId: String) = "$SIGNALING_PATH/$callId/$ICE_CANDIDATES_PATH"
    fun sessionsPath(uid: String) = "$USERS_COLLECTION/$uid/$SESSIONS_SUBCOLLECTION"
    fun preKeysPath(uid: String) = "$USERS_COLLECTION/$uid/$PREKEYS_SUBCOLLECTION"
}
