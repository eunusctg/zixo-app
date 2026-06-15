package com.zixo.app

import android.app.Application
import com.zixo.app.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

// ════════════════════════════════════════════════════════════════
// Zixo Application — Hilt entry point
// ════════════════════════════════════════════════════════════════

/**
 * Application class annotated with [@HiltAndroidApp][HiltAndroidApp]
 * to trigger Hilt's code generation and dependency injection.
 *
 * Initialization order:
 * 1. **Timber** logging — plants [Timber.DebugTree] in debug builds
 * 2. **Firebase** — auto-initialized by the `google-services` Gradle plugin;
 *    no manual setup required here
 *
 * No LiveKit, no manual Firebase initialization, no singleton references.
 * All services are provided through Hilt modules ([AppModule], [FirebaseModule]).
 */
@HiltAndroidApp
class ZixoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── Initialize Timber logging ────────────────────────────
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // ── Firebase is auto-initialized by google-services.json ─
        // No manual FirebaseApp.initializeApp() call needed when
        // the google-services Gradle plugin is applied.
    }
}
