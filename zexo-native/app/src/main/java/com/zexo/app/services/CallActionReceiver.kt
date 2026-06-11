package com.zexo.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.zexo.app.ACTION_ANSWER_CALL" -> {
                // Handle answer
            }
            "com.zexo.app.ACTION_REJECT_CALL" -> {
                // Handle reject
            }
        }
    }
}
