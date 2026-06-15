plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.zixo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zixo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "FIREBASE_API_KEY", "\"AIzaSyBgNhIaIG5jcRkQ7frreFjo1Cz8F3_JfPk\"")
            buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"zixo-call.firebaseapp.com\"")
            buildConfigField("String", "FIREBASE_DATABASE_URL", "\"https://zixo-call-default-rtdb.firebaseio.com\"")
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"zixo-call\"")
            buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"zixo-call.firebasestorage.app\"")
            buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"809372450511\"")
            buildConfigField("String", "FIREBASE_APP_ID", "\"1:809372450511:android:7910b1a9b8836c7666c1ba\"")
            buildConfigField("String", "FIREBASE_MEASUREMENT_ID", "\"G-L792VKMNTT\"")
            // Cloudflare Edge Worker endpoint for registration & passkey minting
            buildConfigField("String", "CLOUDFLARE_EDGE_URL", "\"https://zixo-edge.workers.dev\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "FIREBASE_API_KEY", "\"AIzaSyBgNhIaIG5jcRkQ7frreFjo1Cz8F3_JfPk\"")
            buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"zixo-call.firebaseapp.com\"")
            buildConfigField("String", "FIREBASE_DATABASE_URL", "\"https://zixo-call-default-rtdb.firebaseio.com\"")
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"zixo-call\"")
            buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"zixo-call.firebasestorage.app\"")
            buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"809372450511\"")
            buildConfigField("String", "FIREBASE_APP_ID", "\"1:809372450511:android:7910b1a9b8836c7666c1ba\"")
            buildConfigField("String", "FIREBASE_MEASUREMENT_ID", "\"G-L792VKMNTT\"")
            buildConfigField("String", "CLOUDFLARE_EDGE_URL", "\"https://zixo-edge.workers.dev\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Jetpack Compose, Graphics & Animation Core ──
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")

    // ── Core Platform Architecture & Persistent Engines ──
    implementation(libs.androidx.core.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ── Coroutines ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ── Hilt Dependency Injection ──
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ── DataStore Preferences ──
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Room Local Database ──
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Google Credential Manager (Native Sign-In & WebAuthn Passkeys API) ──
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ── WebRTC Android Native Library for Pure Peer-to-Peer Calling Media Engine ──
    implementation("io.github.webrtc-sdk:android:120.0.0")

    // ── Unified Firebase Enterprise Realtime Stack (100% Realtime Sockets Architecture) ──
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ── Networking (Cloudflare Edge Worker API) ──
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ── Image Loading (Coil 3) ──
    implementation(libs.coil.compose)
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    // ── Biometric Authentication ──
    implementation(libs.biometric)

    // ── QR Code Generation ──
    implementation(libs.zxing.core)

    // ── Serialization ──
    implementation(libs.kotlinx.serialization.json)

    // ── Logging ──
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ── Media3 ExoPlayer ──
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    // ── Compose Tooling (Debug) ──
    debugImplementation(libs.compose.ui.tooling)
}
