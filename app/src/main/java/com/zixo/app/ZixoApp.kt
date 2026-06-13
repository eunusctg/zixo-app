package com.zixo.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZixoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        try {
            FirebaseDatabase.getInstance("https://zixo-call-default-rtdb.firebaseio.com")
                .setPersistenceEnabled(true)
        } catch (_: Exception) {
            // Already initialized
        }
    }
}
