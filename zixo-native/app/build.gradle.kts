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
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image Loading
    implementation(libs.coil.compose)

    // LiveKit
    implementation(libs.livekit.android)

    // Biometric
    implementation(libs.biometric)

    // QR Code
    implementation(libs.zxing.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
