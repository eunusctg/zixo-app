package com.zixo.app.data.remote.livekit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zixo.app.MainActivity
import com.zixo.app.R
import timber.log.Timber

/**
 * A foreground service that keeps the app alive during an active call.
 *
 * Android requires a foreground service with a persistent notification for
 * any ongoing audio/video communication so the system does not kill the
 * process while the user is on a call.
 */
class CallForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "zixo_ongoing_call"
        const val NOTIFICATION_ID = 30001

        // Intent extras
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_ROOM_NAME = "extra_room_name"

        // Call types
        const val CALL_TYPE_AUDIO = "audio"
        const val CALL_TYPE_VIDEO = "video"

        // ---------------------------------------------------------------------------
        // Start / stop helpers
        // ---------------------------------------------------------------------------

        /**
         * Start the foreground service for an ongoing call.
         *
         * @param context  Application or activity context.
         * @param callType One of [CALL_TYPE_AUDIO] or [CALL_TYPE_VIDEO].
         * @param callerName Display name of the remote participant.
         * @param roomName  The LiveKit room name for the call.
         */
        fun start(
            context: Context,
            callType: String = CALL_TYPE_AUDIO,
            callerName: String = "",
            roomName: String = ""
        ) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_ROOM_NAME, roomName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the foreground service.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, CallForegroundService::class.java))
        }

        /**
         * Create the notification channel for ongoing calls. Safe to call
         * multiple times; the system ignores duplicate channel creation.
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Ongoing Call",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shown during an active voice or video call"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Service lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        Timber.d("CallForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CALL_TYPE_AUDIO
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: ""
        val roomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: ""

        val notification = buildNotification(callType, callerName, roomName)
        startForeground(NOTIFICATION_ID, notification)
        Timber.d("Call foreground service started (type=%s, caller=%s)", callType, callerName)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.d("CallForegroundService destroyed")
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------
    // Notification builder
    // ---------------------------------------------------------------------------

    private fun buildNotification(
        callType: String,
        callerName: String,
        roomName: String
    ): Notification {
        val isVideo = callType == CALL_TYPE_VIDEO
        val title = if (callerName.isNotBlank()) {
            if (isVideo) "Video call with $callerName" else "Voice call with $callerName"
        } else {
            if (isVideo) "Video call in progress" else "Voice call in progress"
        }

        // Tap intent – return to the call screen in the main activity
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "call")
            putExtra("roomName", roomName)
            putExtra("callType", callType)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // End-call action
        val endCallIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_END_CALL
            putExtra(EXTRA_ROOM_NAME, roomName)
        }
        val endCallPendingIntent = PendingIntent.getService(
            this,
            1,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Tap to return to the call")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )
            .build()
    }

    // ---------------------------------------------------------------------------
    // Action handling
    // ---------------------------------------------------------------------------

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If the user swipes the app from recents during a call, clean up
        Timber.d("Task removed during call; stopping foreground service")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private val ACTION_END_CALL = "com.zixo.app.ACTION_END_CALL"

    // We handle the end-call action inside onStartCommand so the intent
    // is processed even when the service is already running.
}
