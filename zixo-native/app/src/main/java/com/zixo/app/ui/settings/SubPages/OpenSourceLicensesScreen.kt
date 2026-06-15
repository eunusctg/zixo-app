package com.zixo.app.ui.settings.SubPages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

/**
 * Open Source Licenses screen with Liquid Glass styling.
 *
 * Displays attribution and license information for all third-party
 * open source libraries used in the Zixo application, as required
 * by their respective license terms.
 */
@Composable
fun OpenSourceLicensesScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            ZixoTopBar(
                title = "Open Source Licenses",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Zixo is built with the following open source libraries. " +
                                "We are grateful to the open source community for making " +
                                "these projects available.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── License Entries ────────────────────────────────────────
            item {
                LicenseEntry("Kotlin", "JetBrains", "Apache 2.0",
                    "The programming language used for the entire Zixo application.")
            }
            item {
                LicenseEntry("Jetpack Compose", "Google", "Apache 2.0",
                    "Android's modern declarative UI toolkit for building native interfaces.")
            }
            item {
                LicenseEntry("Material Design 3", "Google", "Apache 2.0",
                    "Material Design components and theming system for Android.")
            }
            item {
                LicenseEntry("Hilt / Dagger", "Google", "Apache 2.0",
                    "Dependency injection framework built on Dagger for Android.")
            }
            item {
                LicenseEntry("Firebase SDK", "Google", "Apache 2.0",
                    "Authentication, Firestore, Realtime Database, Cloud Storage, and FCM.")
            }
            item {
                LicenseEntry("WebRTC (Android)", "Google / WebRTC", "BSD 3-Clause",
                    "Real-time peer-to-peer audio, video, and data communication library.")
            }
            item {
                LicenseEntry("Room", "Google", "Apache 2.0",
                    "SQLite abstraction layer for local offline-first data persistence.")
            }
            item {
                LicenseEntry("WorkManager", "Google", "Apache 2.0",
                    "Background task scheduling for deferrable, guaranteed execution.")
            }
            item {
                LicenseEntry("Coil", "Coil-kt", "Apache 2.0",
                    "Image loading library for Android backed by Kotlin coroutines.")
            }
            item {
                LicenseEntry("Media3 / ExoPlayer", "Google", "Apache 2.0",
                    "Media playback library for audio and video content.")
            }
            item {
                LicenseEntry("OkHttp", "Square", "Apache 2.0",
                    "HTTP client for Android and Java applications.")
            }
            item {
                LicenseEntry("Retrofit", "Square", "Apache 2.0",
                    "Type-safe HTTP client for Android and Java built on OkHttp.")
            }
            item {
                LicenseEntry("Kotlinx Serialization", "JetBrains", "Apache 2.0",
                    "Kotlin compiler plugin and runtime for multiplatform serialization.")
            }
            item {
                LicenseEntry("Kotlinx Coroutines", "JetBrains", "Apache 2.0",
                    "Library support for Kotlin coroutines with async/await patterns.")
            }
            item {
                LicenseEntry("DataStore", "Google", "Apache 2.0",
                    "Jetpack DataStore for key-value and typed object storage.")
            }
            item {
                LicenseEntry("ZXing", "Google", "Apache 2.0",
                    "Multi-format 1D/2D barcode image processing library for QR codes.")
            }
            item {
                LicenseEntry("AndroidX Biometric", "Google", "Apache 2.0",
                    "Biometric prompt API for fingerprint and face authentication.")
            }
            item {
                LicenseEntry("AndroidX Credentials", "Google", "Apache 2.0",
                    "Credential Manager API for sign-in and WebAuthn passkeys.")
            }
            item {
                LicenseEntry("Timber", "Jake Wharton", "Apache 2.0",
                    "Logger with a small API providing extension points for customization.")
            }

            // ── Apache 2.0 Full Text ───────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "APACHE LICENSE 2.0",
                        color = NeonMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
                                "you may not use this file except in compliance with the License. " +
                                "You may obtain a copy of the License at\n\n" +
                                "http://www.apache.org/licenses/LICENSE-2.0\n\n" +
                                "Unless required by applicable law or agreed to in writing, " +
                                "software distributed under the License is distributed on an " +
                                "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, " +
                                "either express or implied. See the License for the specific " +
                                "language governing permissions and limitations under the License.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ── BSD 3-Clause Full Text ─────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "BSD 3-CLAUSE LICENSE",
                        color = NeonMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Redistribution and use in source and binary forms, with or without " +
                                "modification, are permitted provided that the following conditions " +
                                "are met:\n\n" +
                                "1. Redistributions of source code must retain the above copyright " +
                                "notice, this list of conditions and the following disclaimer.\n\n" +
                                "2. Redistributions in binary form must reproduce the above copyright " +
                                "notice, this list of conditions and the following disclaimer in the " +
                                "documentation and/or other materials provided with the distribution.\n\n" +
                                "3. Neither the name of the copyright holder nor the names of its " +
                                "contributors may be used to endorse or promote products derived from " +
                                "this software without specific prior written permission.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseEntry(
    name: String,
    author: String,
    license: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = license,
                color = NeonMint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "by $author",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
