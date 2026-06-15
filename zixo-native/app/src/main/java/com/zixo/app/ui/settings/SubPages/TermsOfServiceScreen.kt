package com.zixo.app.ui.settings.SubPages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

/**
 * Terms of Service screen with Liquid Glass styling.
 *
 * Displays the complete legal terms governing the use of Zixo,
 * structured into clearly labeled sections for readability.
 */
@Composable
fun TermsOfServiceScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            ZixoTopBar(
                title = "Terms of Service",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Effective Date ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Effective Date: January 1, 2026",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Welcome to Zixo. By accessing or using the Zixo application " +
                                "(\"Service\"), you agree to be bound by these Terms of Service " +
                                "(\"Terms\"). If you do not agree to these Terms, you may not " +
                                "access or use the Service. These Terms constitute a legally " +
                                "binding agreement between you and Zixo Inc.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── 1. Acceptance of Terms ─────────────────────────────────
            item {
                TermsSection(
                    title = "1. Acceptance of Terms",
                    content = "By creating an account, downloading, installing, or using the " +
                            "Zixo Service, you acknowledge that you have read, understood, and " +
                            "agree to be bound by these Terms and our Privacy Policy. You must " +
                            "be at least 13 years of age to use the Service. If you are under " +
                            "the age of majority in your jurisdiction, your parent or legal " +
                            "guardian must agree to these Terms on your behalf. Zixo reserves " +
                            "the right to update these Terms at any time, and continued use of " +
                            "the Service constitutes acceptance of any modifications."
                )
            }

            // ── 2. Account Registration ────────────────────────────────
            item {
                TermsSection(
                    title = "2. Account Registration & Zixo Number",
                    content = "Upon registration, you will be assigned a unique 8-digit Zixo " +
                            "Number at no cost. This number serves as your primary identifier " +
                            "within the Zixo ecosystem. You are responsible for maintaining the " +
                            "confidentiality of your account credentials and for all activities " +
                            "that occur under your account. You agree to notify Zixo immediately " +
                            "of any unauthorized use of your account. The free tier includes " +
                            "unlimited 1-on-1 and group audio/video calls, text messaging, and " +
                            "status updates at no charge."
                )
            }

            // ── 3. Premium Services ────────────────────────────────────
            item {
                TermsSection(
                    title = "3. Premium Services & Billing",
                    content = "Certain premium features, including the ability to receive regular " +
                            "PSTN calls via your Zixo Number, require a paid subscription. Premium " +
                            "subscriptions are billed through Google Play Billing. Subscriptions " +
                            "auto-renew at the end of each billing period unless canceled at " +
                            "least 24 hours before the renewal date. Refunds are subject to " +
                            "Google Play's refund policy. Zixo reserves the right to modify " +
                            "pricing with 30 days advance notice. Premium features are activated " +
                            "only after successful payment verification."
                )
            }

            // ── 4. Acceptable Use ──────────────────────────────────────
            item {
                TermsSection(
                    title = "4. Acceptable Use Policy",
                    content = "You agree not to use the Service for any unlawful purpose or in " +
                            "any way that could damage, disable, or impair the Service. Prohibited " +
                            "activities include, but are not limited to: transmitting malicious " +
                            "code, impersonating other users, engaging in harassment or bullying, " +
                            "distributing spam, attempting to gain unauthorized access to other " +
                            "users' accounts, and violating any applicable local, state, national, " +
                            "or international law. Zixo reserves the right to suspend or terminate " +
                            "accounts that violate these policies without prior notice."
                )
            }

            // ── 5. Privacy & Encryption ────────────────────────────────
            item {
                TermsSection(
                    title = "5. Privacy & End-to-End Encryption",
                    content = "Zixo employs Signal Protocol encryption (X3DH key agreement + " +
                            "Double Ratchet) for all message and call content. This means that " +
                            "messages are encrypted on your device before transmission and can " +
                            "only be decrypted by the intended recipient. Zixo cannot read your " +
                            "messages or listen to your calls. Metadata necessary for service " +
                            "operation (such as delivery receipts and online status) is processed " +
                            "in accordance with our Privacy Policy. You retain full ownership of " +
                            "all content you transmit through the Service."
                )
            }

            // ── 6. Intellectual Property ───────────────────────────────
            item {
                TermsSection(
                    title = "6. Intellectual Property",
                    content = "The Zixo application, including all software, design, text, " +
                            "graphics, and other content, is owned by Zixo Inc. and protected " +
                            "by intellectual property laws. You are granted a limited, " +
                            "non-exclusive, non-transferable license to use the Service for " +
                            "personal, non-commercial purposes. You may not modify, reverse " +
                            "engineer, decompile, or create derivative works from the Service. " +
                            "The Zixo name, logo, and brand elements are trademarks of Zixo Inc."
                )
            }

            // ── 7. Termination ─────────────────────────────────────────
            item {
                TermsSection(
                    title = "7. Account Termination",
                    content = "You may delete your account at any time through the Settings screen. " +
                            "Upon deletion, your profile, messages, and associated data will be " +
                            "permanently removed from our servers within 30 days. Zixo reserves " +
                            "the right to suspend or terminate accounts that violate these Terms, " +
                            "engage in fraudulent activity, or pose a security risk. Upon " +
                            "termination, your right to use the Service ceases immediately. " +
                            "Provisions that by their nature should survive termination shall " +
                            "remain in effect."
                )
            }

            // ── 8. Disclaimers & Liability ─────────────────────────────
            item {
                TermsSection(
                    title = "8. Disclaimers & Limitation of Liability",
                    content = "The Service is provided \"as is\" and \"as available\" without " +
                            "warranties of any kind, either express or implied. Zixo does not " +
                            "guarantee that the Service will be uninterrupted, timely, secure, " +
                            "or error-free. In no event shall Zixo Inc. be liable for any " +
                            "indirect, incidental, special, consequential, or punitive damages, " +
                            "including but not limited to loss of data, profits, or goodwill, " +
                            "arising out of or in connection with your use of the Service. " +
                            "Zixo's total liability shall not exceed the amount you paid for " +
                            "the Service in the twelve months preceding the claim."
                )
            }

            // ── 9. Governing Law ───────────────────────────────────────
            item {
                TermsSection(
                    title = "9. Governing Law & Dispute Resolution",
                    content = "These Terms are governed by the laws of the State of Delaware, " +
                            "United States, without regard to conflict of law principles. Any " +
                            "disputes arising from these Terms or the Service shall be resolved " +
                            "through binding arbitration administered by the American Arbitration " +
                            "Association. You waive any right to participate in class action " +
                            "lawsuits or class-wide arbitration. For disputes qualifying for " +
                            "small claims court, you may bring claims in your local jurisdiction."
                )
            }

            // ── 10. Contact ────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "For questions about these Terms, contact us at legal@zixo.app",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
