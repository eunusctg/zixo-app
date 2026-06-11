package com.zexo.app.services

import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.zexo.app.R

class ZexoFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "ZexoFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Save token to Firestore
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
                .addOnFailureListener { Log.e(TAG, "Failed to update FCM token", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")
        
        val data = message.data
        when (data["type"]) {
            "call" -> {
                // Show incoming call notification
                val callerName = data["callerName"] ?: "Unknown"
                val callType = data["callType"] ?: "audio"
                showCallNotification(callerName, callType)
            }
            "message" -> {
                // Show message notification
                val senderName = data["senderName"] ?: "Someone"
                val text = data["text"] ?: ""
                showMessageNotification(senderName, text)
            }
        }
    }

    private fun showCallNotification(callerName: String, callType: String) {
        // Call notifications are handled by CallActivity
    }

    private fun showMessageNotification(senderName: String, text: String) {
        val notification = NotificationCompat.Builder(this, "zixo_messages")
            .setContentTitle(senderName)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setAutoCancel(true)
            .build()
        
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
