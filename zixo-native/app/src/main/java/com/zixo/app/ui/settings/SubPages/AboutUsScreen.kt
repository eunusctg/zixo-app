package com.zixo.app.ui.settings.SubPages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// About Us Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "About Zixo" page rendered with the Zixo Liquid Glass design
 * language.
 *
 * Sections:
 * 1. App Branding — icon, name, version, tagline
 * 2. About Zixo — description paragraph
 * 3. Core Features — feature list with emoji bullets
 * 4. Legal & Licenses — clickable navigation rows
 * 5. Footer — attribution and copyright
 */
@Composable
fun AboutUsScreen(
    onBackClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsOfServiceClick: () -> Unit = {},
    onLicensesClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            ZixoTopBar(
                title = "About Zixo",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Section 1: App Branding ──────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Zixo squircle icon
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(38.dp))
                            .background(NeonMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Z",
                            color = Color.Black,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App name
                    Text(
                        text = "Zixo",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Version
                    Text(
                        text = "Version 1.0.0 (Build 1)",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tagline
                    Text(
                        text = "Secure messaging & calls",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // ── Section 2: About Zixo ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("About Zixo")

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Zixo is a secure, end-to-end encrypted messenger built for " +
                                "privacy-first communication. Every message, call, and status " +
                                "update is protected with Signal Protocol encryption (X3DH key " +
                                "agreement + Double Ratchet), ensuring that only you and your " +
                                "contacts can read your conversations. With zero-trust contact " +
                                "verification, pure WebRTC calling, and offline-first sync, Zixo " +
                                "delivers enterprise-grade security in a beautifully designed " +
                                "experience. Your data stays yours — always.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── Section 3: Core Features ─────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("Core Features")

                    Spacer(modifier = Modifier.height(12.dp))

                    FeatureRow(
                        emoji = "🔐",
                        title = "End-to-End Encryption",
                        description = "Signal Protocol X3DH + Double Ratchet"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "📞",
                        title = "Secure Calls",
                        description = "Pure WebRTC with encrypted signaling"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "🔍",
                        title = "Zero-Trust Contacts",
                        description = "Communication only between verified mutual contacts"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "📱",
                        title = "Offline-First",
                        description = "Your messages sync when you're back online"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "🛡️",
                        title = "Biometric Lock",
                        description = "Fingerprint and face unlock support"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "🔑",
                        title = "Passkey Authentication",
                        description = "WebAuthn passwordless sign-in"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "💬",
                        title = "Disappearing Messages",
                        description = "Ephemeral timer for sensitive conversations"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow(
                        emoji = "🌐",
                        title = "Cloudflare Edge",
                        description = "Fast, secure backend infrastructure"
                    )
                }
            }

            // ── Section 4: Legal & Licenses ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    SectionLabel("Legal & Licenses")

                    Spacer(modifier = Modifier.height(8.dp))

                    LegalRow(
                        label = "Privacy Policy",
                        onClick = onPrivacyPolicyClick
                    )

                    LegalRow(
                        label = "Terms of Service",
                        onClick = onTermsOfServiceClick
                    )

                    LegalRow(
                        label = "Open Source Licenses",
                        onClick = onLicensesClick
                    )
                }
            }

            // ── Section 5: Footer ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with ❤️ by the Zixo Team",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "© 2024-2026 Zixo Inc. All rights reserved.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = NeonMint,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Feature Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp,
            modifier = Modifier.width(32.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legal Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LegalRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
