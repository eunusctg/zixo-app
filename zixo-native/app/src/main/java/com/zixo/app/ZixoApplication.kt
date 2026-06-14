package com.zixo.app

import android.app.Application
import android.util.Log
import com.zixo.app.data.local.datastore.UserPreferences
import com.zixo.app.data.remote.firebase.FirebaseAuthService
import com.zixo.app.data.remote.firebase.FirestoreService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ZixoApplication : Application() {

    @Inject
    lateinit var firebaseAuthService: FirebaseAuthService

    @Inject
    lateinit var firestoreService: FirestoreService

    @Inject
    lateinit var userPreferences: UserPreferences

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeApp()
    }

    private fun initializeApp() {
        // Configure strict mode for debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Update the user's online status and last-seen timestamp on app start
        applicationScope.launch {
            try {
                val currentUser = firebaseAuthService.getCurrentUser()
                if (currentUser != null) {
                    firestoreService.updateOnlineStatus(currentUser.uid, true).first()
                    firestoreService.updateLastSeen(currentUser.uid).first()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update online status on start", e)
            }
        }
    }

    private fun enableStrictMode() {
        android.os.StrictMode.setThreadPolicy(
            android.os.StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        android.os.StrictMode.setVmPolicy(
            android.os.StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    companion object {
        private const val TAG = "ZixoApplication"

        @Volatile
        private lateinit var instance: ZixoApplication

        fun getInstance(): ZixoApplication = instance
    }
}
