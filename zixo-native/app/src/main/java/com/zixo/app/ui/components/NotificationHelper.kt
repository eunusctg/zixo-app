package com.zixo.app.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zixo.app.MainActivity
import com.zixo.app.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized notification management for the Zixo messenger.
 *
 * Creates notification channels, manages notification sound/vibration
 * configuration, and provides typed methods for message, call, and
 * group notifications with proper grouping, person avatars, and actions.
 *
 * Handles Android 13+ POST_NOTIFICATIONS permission gracefully.
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_MESSAGES = "zixo_messages"
        const val CHANNEL_CALLS = "zixo_calls"
        const val CHANNEL_GROUP = "zixo_group"
        const val CHANNEL_STATUS = "zixo_status"

        const val NOTIFICATION_ID_MESSAGE = 10001
        const val NOTIFICATION_ID_CALL = 20001
        const val NOTIFICATION_ID_GROUP = 30001

        const val ACTION_ANSWER_CALL = "com.zixo.app.ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.zixo.app.DECLINE_CALL"
        const val ACTION_MARK_READ = "com.zixo.app.MARK_READ"
    }

    /**
     * Creates all notification channels. Must be called on app startup.
     */
    fun createChannels() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val messageChannel = NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming chat messages"
                    enableLights(true)
                    lightColor = 0xFF00E676.toInt()
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                    setShowBadge(true)
                }

                val callChannel = NotificationChannel(
                    CHANNEL_CALLS,
                    "Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming audio and video calls"
                    enableLights(true)
                    lightColor = 0xFF00E676.toInt()
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setShowBadge(true)
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), audioAttributes)
                }

                val groupChannel = NotificationChannel(
                    CHANNEL_GROUP,
                    "Group Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Group chat messages"
                    enableLights(true)
                    lightColor = 0xFF00E676.toInt()
                    setShowBadge(true)
                }

                val statusChannel = NotificationChannel(
                    CHANNEL_STATUS,
                    "Status Updates",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "New status from contacts"
                    setShowBadge(false)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannels(
                    listOf(messageChannel, callChannel, groupChannel, statusChannel)
                )
            }
            Timber.d("NotificationHelper: All channels created")
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to create notification channels")
        }
    }

    /**
     * Shows a message notification with reply action.
     */
    fun showMessageNotification(
        title: String,
        body: String,
        chatId: String,
        senderUid: String
    ) {
        try {
            if (!hasNotificationPermission()) return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("chatId", chatId)
                putExtra("senderUid", senderUid)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_MESSAGE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup("zixo_messages_${chatId}")
                .setColor(0xFF00E676.toInt())
                .build()

            notificationManager.notify(NOTIFICATION_ID_MESSAGE + chatId.hashCode() % 1000, notification)
            Timber.d("NotificationHelper: Message notification shown for %s", title)
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to show message notification")
        }
    }

    /**
     * Shows a call notification with answer/decline actions.
     */
    fun showCallNotification(
        title: String,
        callId: String,
        isVideo: Boolean
    ) {
        try {
            if (!hasNotificationPermission()) return

            val answerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("callId", callId)
                putExtra("action", ACTION_ANSWER_CALL)
            }

            val declineIntent = Intent(ACTION_DECLINE_CALL).apply {
                putExtra("callId", callId)
            }

            val answerPendingIntent = PendingIntent.getActivity(
                context, 0, answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val declinePendingIntent = PendingIntent.getBroadcast(
                context, 1, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callType = if (isVideo) "Video Call" else "Audio Call"
            val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Incoming $callType")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(answerPendingIntent, true)
                .addAction(0, "Answer", answerPendingIntent)
                .addAction(0, "Decline", declinePendingIntent)
                .setColor(0xFF00E676.toInt())
                .build()

            notificationManager.notify(NOTIFICATION_ID_CALL, notification)
            Timber.d("NotificationHelper: Call notification shown for %s", title)
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to show call notification")
        }
    }

    /**
     * Shows a group notification with sender info.
     */
    fun showGroupNotification(
        groupName: String,
        senderName: String,
        body: String,
        chatId: String
    ) {
        try {
            if (!hasNotificationPermission()) return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("chatId", chatId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_GROUP,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_GROUP)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(groupName)
                .setContentText("$senderName: $body")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$senderName: $body"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup("zixo_group_$chatId")
                .setColor(0xFF00E676.toInt())
                .build()

            notificationManager.notify(NOTIFICATION_ID_GROUP + chatId.hashCode() % 1000, notification)
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to show group notification")
        }
    }

    fun dismissNotification(id: Int) {
        try {
            notificationManager.cancel(id)
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to dismiss notification")
        }
    }

    fun dismissAll() {
        try {
            notificationManager.cancelAll()
            Timber.d("NotificationHelper: All notifications dismissed")
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to dismiss all notifications")
        }
    }

    fun setNotificationSound(uri: Uri?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .getNotificationChannel(CHANNEL_MESSAGES)
                channel?.setSound(uri, channel.audioAttributes)
            }
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to set notification sound")
        }
    }

    fun setVibrationPattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .getNotificationChannel(CHANNEL_MESSAGES)
                channel?.vibrationPattern = pattern
            }
        } catch (e: Exception) {
            Timber.e(e, "NotificationHelper: Failed to set vibration pattern")
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
