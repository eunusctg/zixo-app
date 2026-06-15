package com.zixo.app.data.remote.webrtc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zixo.app.MainActivity
import com.zixo.app.R
import timber.log.Timber

/**
 * Mandatory Foreground Service for active WebRTC calls.
 *
 * This service is flagged with FOREGROUND_SERVICE_TYPE_CAMERA,
 * FOREGROUND_SERVICE_TYPE_MICROPHONE, and FOREGROUND_SERVICE_TYPE_PHONE_CALL.
 * This guarantees Android does not terminate the media stream when a user
 * backgrounds the application during an active call.
 *
 * ## Key Design Decisions:
 * - Uses all three foreground service types (camera | microphone | phoneCall)
 *   to ensure the system keeps both media streams alive
 * - Shows a persistent notification with "End Call" action
 * - Runs the WebRTC session within this service scope
 * - Cleanly stops when the call ends
 *
 * All call lifecycle operations run on Dispatchers.IO, never blocking Main Thread.
 */
class CallForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "zixo_ongoing_call"
        const val NOTIFICATION_ID = 30001

        // Intent extras
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALL_ID = "extra_call_id"

        // Call types
        const val CALL_TYPE_AUDIO = "audio"
        const val CALL_TYPE_VIDEO = "video"

        /**
         * Start the foreground service for an ongoing WebRTC call.
         *
         * The service type is dynamically set based on the call type:
         * - Audio calls: PHONE_CALL | MICROPHONE
         * - Video calls: PHONE_CALL | CAMERA | MICROPHONE
         *
         * @param context    Application or activity context.
         * @param callType   One of [CALL_TYPE_AUDIO] or [CALL_TYPE_VIDEO].
         * @param callerName Display name of the remote participant.
         * @param callId     The WebRTC call ID.
         */
        fun start(
            context: Context,
            callType: String = CALL_TYPE_AUDIO,
            callerName: String = "",
            callId: String = ""
        ) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALL_ID, callId)
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
         * Create the notification channel for ongoing calls.
         * Safe to call multiple times; the system ignores duplicates.
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Ongoing Call",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shown during an active WebRTC voice or video call"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    // ── Service Lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        Timber.d("CallForegroundService created (WebRTC, CAMERA+MIC+PHONE_CALL)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Handle end call action
        if (intent.action == ACTION_END_CALL) {
            Timber.d("End call action received via notification")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CALL_TYPE_AUDIO
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: ""
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""

        val notification = buildNotification(callType, callerName, callId)

        // Start foreground with the appropriate service types
        // This ensures Android keeps the camera and microphone streams alive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+: Must specify foreground service types at start time
            val serviceTypes = if (callType == CALL_TYPE_VIDEO) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceTypes)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29-33: Service type declared in manifest, start normally
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        Timber.d("WebRTC call foreground service started (type=%s, caller=%s, serviceTypes=CAMERA+MIC+PHONE)", callType, callerName)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.d("CallForegroundService destroyed")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("Task removed during call; stopping foreground service")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    // ── Notification Builder ──────────────────────────────────────────────────

    private fun buildNotification(
        callType: String,
        callerName: String,
        callId: String
    ): Notification {
        val isVideo = callType == CALL_TYPE_VIDEO
        val title = if (callerName.isNotBlank()) {
            if (isVideo) "Video call with $callerName" else "Voice call with $callerName"
        } else {
            if (isVideo) "Video call in progress" else "Voice call in progress"
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "call")
            putExtra("callId", callId)
            putExtra("callType", callType)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_END_CALL
            putExtra(EXTRA_CALL_ID, callId)
        }
        val endCallPendingIntent = PendingIntent.getService(
            this, 1, endCallIntent,
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

    private val ACTION_END_CALL = "com.zixo.app.ACTION_END_CALL"
}
