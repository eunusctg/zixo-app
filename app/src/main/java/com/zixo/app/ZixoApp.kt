package com.zixo.app

import android.app.Application
import com.google.firebase.FirebaseApp

class ZixoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase init failure should not crash the app
        }
    }
}
