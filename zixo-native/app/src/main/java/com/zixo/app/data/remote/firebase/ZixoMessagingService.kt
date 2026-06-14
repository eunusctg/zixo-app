package com.zixo.app.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zixo.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ZixoMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------------------------------------------------------------------------
    // Notification channel constants
    // ---------------------------------------------------------------------------

    companion object {
        const val CHANNEL_MESSAGES = "zixo_messages"
        const val CHANNEL_CALLS = "zixo_calls"

        private const val PREFS_NAME = "zixo_notification_prefs"
        private const val KEY_MESSAGE_PREVIEW_ENABLED = "message_preview_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

        /**
         * Create the notification channels. Safe to call multiple times.
         */
        fun createNotificationChannels(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages channel
            if (notificationManager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
                val messageAudioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build()

                val messageChannel = NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming chat messages"
                    enableLights(true)
                    enableVibration(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        messageAudioAttributes
                    )
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(messageChannel)
            }

            // Calls channel
            if (notificationManager.getNotificationChannel(CHANNEL_CALLS) == null) {
                val callAudioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_EVENT)
                    .build()

                val callChannel = NotificationChannel(
                    CHANNEL_CALLS,
                    "Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming voice and video calls"
                    enableLights(true)
                    enableVibration(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                        callAudioAttributes
                    )
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(callChannel)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ---------------------------------------------------------------------------
    // Token refresh
    // ---------------------------------------------------------------------------

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed: %s", token)

        // Persist the token locally for later use
        sharedPrefs.edit { putString("fcm_token", token) }

        // Send the new token to Firestore so the server can target this device
        val uid = sharedPrefs.getString("current_uid", null)
        if (uid != null) {
            sendTokenToServer(uid, token)
        } else {
            Timber.w("No current UID; FCM token will be uploaded on next sign-in")
        }
    }

    private fun sendTokenToServer(uid: String, token: String) {
        serviceScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .update(mapOf("fcmToken" to token))
                    .await()
                Timber.d("FCM token updated on server for user %s", uid)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update FCM token on server")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Incoming messages
    // ---------------------------------------------------------------------------

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Message received from: %s", remoteMessage.from)

        // Check if notifications are enabled
        val notificationsEnabled = sharedPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        if (!notificationsEnabled) {
            Timber.d("Notifications disabled by user preference; skipping")
            return
        }

        // Determine notification type
        val data = remoteMessage.data
        val type = data["type"] ?: "message"

        when (type) {
            "call" -> handleCallNotification(data)
            else -> handleMessageNotification(remoteMessage, data)
        }
    }

    // ---------------------------------------------------------------------------
    // Chat message notifications
    // ---------------------------------------------------------------------------

    private fun handleMessageNotification(
        remoteMessage: RemoteMessage,
        data: Map<String, String>
    ) {
        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "Zixo"
        val messagePreviewEnabled = sharedPrefs.getBoolean(KEY_MESSAGE_PREVIEW_ENABLED, true)

        val body = if (messagePreviewEnabled) {
            data["text"] ?: remoteMessage.notification?.body ?: "New message"
        } else {
            "New message"
        }

        val threadId = data["threadId"]
        val senderId = data["senderId"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "chat")
            putExtra("threadId", threadId)
            putExtra("senderId", senderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(com.zixo.app.R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (threadId?.hashCode() ?: System.currentTimeMillis().toInt())
        notificationManager.notify(notificationId, notification)
    }

    // ---------------------------------------------------------------------------
    // Call notifications
    // ---------------------------------------------------------------------------

    private fun handleCallNotification(data: Map<String, String>) {
        val callerName = data["callerName"] ?: "Unknown caller"
        val callType = data["callType"] ?: "audio" // audio | video
        val roomName = data["roomName"]
        val callerId = data["callerId"]

        val title = if (callType == "video") {
            "Video call from $callerName"
        } else {
            "Voice call from $callerName"
        }

        // Answer action
        val answerIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "call_answer")
            putExtra("roomName", roomName)
            putExtra("callType", callType)
            putExtra("callerId", callerId)
            putExtra("callerName", callerName)
            action = "ACTION_ANSWER_CALL"
        }
        val answerPendingIntent = PendingIntent.getActivity(
            this,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action
        val declineIntent = Intent(this, ZixoMessagingService::class.java).apply {
            action = "ACTION_DECLINE_CALL"
        }
        val declinePendingIntent = PendingIntent.getService(
            this,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(com.zixo.app.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Incoming $callType call")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(answerPendingIntent, true)
            .setContentIntent(answerPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline",
                declinePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                answerPendingIntent
            )
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_CALL, notification)
    }

    override fun onDeletedMessages() {
        Timber.d("Messages deleted on server")
    }

    companion object NotificationIds {
        const val NOTIFICATION_ID_CALL = 20001
    }
}
