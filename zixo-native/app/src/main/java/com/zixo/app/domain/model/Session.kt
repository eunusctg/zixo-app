package com.zixo.app.domain.model

import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * Represents an active user session across devices.
 * Stored in Firestore under users/{uid}/sessions/{sessionId}.
 *
 * Updated from stub to include full Firestore serialization,
 * device detection factory, and expiration logic.
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
) {
    /**
     * Converts this session to a Firestore-compatible map.
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "deviceName" to deviceName,
        "deviceModel" to deviceModel,
        "osVersion" to osVersion,
        "appVersion" to appVersion,
        "ipAddress" to ipAddress,
        "lastActive" to lastActive,
        "isActive" to isActive,
        "createdAt" to createdAt
    )

    /**
     * Checks if this session has expired (older than 30 days).
     */
    fun isExpired(): Boolean {
        val expiryMs = SESSION_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastActive > expiryMs
    }

    companion object {
        private const val SESSION_EXPIRY_DAYS = 30

        /**
         * Reconstructs a Session from a Firestore document map.
         */
        fun fromMap(map: Map<String, Any?>): Session = try {
            Session(
                id = map["id"] as? String ?: "",
                deviceName = map["deviceName"] as? String ?: "Unknown",
                deviceModel = map["deviceModel"] as? String ?: "Unknown",
                osVersion = map["osVersion"] as? String ?: "",
                appVersion = map["appVersion"] as? String ?: "",
                ipAddress = map["ipAddress"] as? String,
                lastActive = (map["lastActive"] as? Number)?.toLong() ?: 0L,
                isActive = map["isActive"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Session from map")
            Session(
                id = "", deviceName = "Unknown", deviceModel = "Unknown",
                osVersion = "", appVersion = "", lastActive = 0L,
                isActive = false, createdAt = 0L
            )
        }

        /**
         * Creates a Session from the current device context.
         * Uses Build constants and package manager for device identification.
         */
        fun fromDevice(context: Context, sessionId: String): Session = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"
            Session(
                id = sessionId,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                deviceModel = Build.MODEL,
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = versionName,
                lastActive = System.currentTimeMillis(),
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create Session from device")
            Session(
                id = sessionId,
                deviceName = "Unknown Device",
                deviceModel = Build.MODEL,
                osVersion = "Android ${Build.VERSION.RELEASE}",
                appVersion = "1.0.0",
                lastActive = System.currentTimeMillis(),
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        }
    }
}
