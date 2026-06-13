package com.zexo.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZixoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable offline persistence for Realtime Database
        try {
            FirebaseDatabase.getInstance("https://zixo-call-default-rtdb.firebaseio.com")
                .setPersistenceEnabled(true)
        } catch (_: Exception) {
            // Already initialized
        }
    }
}
