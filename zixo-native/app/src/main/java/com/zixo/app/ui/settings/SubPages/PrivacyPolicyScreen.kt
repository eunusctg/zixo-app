package com.zixo.app.ui.settings.SubPages

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Privacy Policy Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "Privacy Policy" page rendered with the Zixo Liquid Glass
 * design language. Contains a comprehensive privacy policy document for
 * the Zixo secure messenger application.
 *
 * Content is presented inside a scrollable [LazyColumn] with each section
 * wrapped in a glass-card container. Section titles use the NeonMint accent
 * colour; body text uses [TextSecondary].
 */
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            item {
                ZixoTopBar(
                    title = "Privacy Policy",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            }

            // ── Policy Header ────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassContainer()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Privacy Policy",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Last updated: June 15, 2026",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Effective date: January 1, 2026",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── Introduction ─────────────────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(title = "Introduction")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Zixo Inc. (\"Zixo,\" \"we,\" \"us,\" or \"our\") is committed to protecting " +
                            "your privacy and ensuring the security of your personal information. This " +
                            "Privacy Policy describes how we collect, use, disclose, and safeguard your " +
                            "information when you use our mobile application Zixo Messenger (the \"App\"). " +
                            "By using Zixo, you agree to the collection and use of information in accordance " +
                            "with this policy. We encourage you to read this policy carefully and to check " +
                            "this page periodically for changes."
                    )
                }
            }

            // ── 1. Information We Collect ────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "1", title = "Information We Collect")
                    Spacer(modifier = Modifier.height(10.dp))

                    SubsectionTitle(text = "1.1 Information You Provide")
                    Spacer(modifier = Modifier.height(6.dp))
                    BodyText(
                        text = "When you create a Zixo account, you provide certain personal information " +
                            "that is essential for the operation of the service. This includes account " +
                            "registration data such as your display name and email address, which is " +
                            "obtained securely through Google Sign-In. You may also provide optional " +
                            "profile information including your avatar, bio, and your unique 8-digit " +
                            "Zixo Number. Additionally, your contact list data is processed solely for " +
                            "the purpose of mutual contact verification — we only use this data to confirm " +
                            "whether both parties have added each other, and it is never stored or shared " +
                            "beyond this verification step. All messages and media you send through Zixo " +
                            "are encrypted end-to-end and are not readable by Zixo at any point in " +
                            "transit or at rest on our servers."
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    SubsectionTitle(text = "1.2 Automatically Collected Information")
                    Spacer(modifier = Modifier.height(6.dp))
                    BodyText(
                        text = "When you use Zixo, certain technical information is collected automatically " +
                            "to ensure the reliability and performance of the service. This includes device " +
                            "information such as your device model, operating system version, and the " +
                            "version of the Zixo app you are running. We also collect anonymized crash " +
                            "reports and analytics data that help us identify and resolve bugs, improve " +
                            "app stability, and enhance the overall user experience. Network information, " +
                            "including connection type and quality metrics, is collected temporarily during " +
                            "active calls to optimize call quality and reduce latency. This data is never " +
                            "linked to your identity and is aggregated for statistical analysis only."
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    SubsectionTitle(text = "1.3 Information We Do NOT Collect")
                    Spacer(modifier = Modifier.height(6.dp))
                    BodyText(
                        text = "Zixo is built on a privacy-first philosophy, and there are strict categories " +
                            "of data we deliberately choose not to collect. We do not read, scan, or analyze " +
                            "your messages — ever. Your conversations remain entirely private, protected by " +
                            "end-to-end encryption that even we cannot bypass. We do not sell your personal " +
                            "data to third parties under any circumstances. We do not track your location " +
                            "or store GPS coordinates at any time. Furthermore, we do not build advertising " +
                            "profiles based on your conversations, contacts, or usage patterns. Zixo will " +
                            "never monetize your personal information, and our business model does not rely " +
                            "on data exploitation of any kind."
                    )
                }
            }

            // ── 2. End-to-End Encryption ─────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "2", title = "End-to-End Encryption")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "All messages, calls, and status updates in Zixo are protected with " +
                            "end-to-end encryption using the Signal Protocol, which combines the X3DH " +
                            "(Extended Triple Diffie-Hellman) key agreement protocol with the Double " +
                            "Ratchet algorithm for forward secrecy and break-in recovery. This means " +
                            "that only you and your intended recipient can read your messages — not " +
                            "Zixo, not any third party, and not any intermediary server. Zixo servers " +
                            "cannot decrypt, read, or access the content of your communications at any " +
                            "stage. Encryption keys are generated and stored exclusively on your device, " +
                            "protected by the Android Keystore system and secured with biometric " +
                            "authentication when enabled. We have no ability to provide decrypted message " +
                            "content to any third party, including law enforcement agencies. Even if " +
                            "compelled by court order, we can only provide encrypted ciphertext that is " +
                            "mathematically infeasible to decrypt without the private keys held only on " +
                            "the devices of the communicating parties."
                    )
                }
            }

            // ── 3. Zero-Trust Contact Model ──────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "3", title = "Zero-Trust Contact Model")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Zixo employs a Zero-Trust Contact Model that fundamentally restricts " +
                            "communication to verified mutual contacts only. Unlike conventional messaging " +
                            "platforms where any user can initiate contact with any other user, Zixo " +
                            "requires that both parties must explicitly add each other using their unique " +
                            "8-digit Zixo Numbers before any communication can occur. This mutual " +
                            "verification ensures that you will never receive unsolicited messages, spam, " +
                            "or contact requests from strangers. When User A adds User B's Zixo Number, " +
                            "User B only becomes reachable once they have also added User A's Zixo Number " +
                            "to their contacts. Any attempt at non-contact communication is blocked " +
                            "at the repository boundary — our server infrastructure rejects messages, " +
                            "calls, and status updates directed at users who have not established a " +
                            "verified mutual contact relationship. This architectural decision eliminates " +
                            "entire categories of harassment, spam, and social engineering attacks that " +
                            "plague traditional messaging platforms."
                    )
                }
            }

            // ── 4. How We Use Your Information ───────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "4", title = "How We Use Your Information")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "We use the information we collect for specific, limited purposes that are " +
                            "essential to providing and improving the Zixo service. We use your account " +
                            "data to provide and maintain the Service, ensuring that your messaging and " +
                            "calling features operate reliably. Your identity is verified via Google " +
                            "Sign-In to prevent account impersonation and ensure platform integrity. We " +
                            "facilitate mutual contact discovery by matching Zixo Numbers between users " +
                            "who have added each other, enabling you to find and connect with people you " +
                            "know. Encrypted messages are routed between contacts through our servers " +
                            "without ever being decrypted or inspected. We use collected information to " +
                            "provide customer support when you reach out with questions or issues. Our " +
                            "systems detect and prevent fraud, abuse, and violations of our Terms of " +
                            "Service to maintain a safe environment for all users. Finally, we use " +
                            "anonymized performance metrics to improve app reliability, reduce bugs, " +
                            "and enhance call quality over time."
                    )
                }
            }

            // ── 5. Data Storage and Security ─────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "5", title = "Data Storage and Security")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Zixo takes a multi-layered approach to data security, employing " +
                            "industry-leading technologies at every level of the infrastructure stack. " +
                            "All data is stored on Google Cloud Platform with enterprise-grade physical " +
                            "security, redundant backups, and continuous monitoring. Identity management " +
                            "is handled through Firebase Authentication, which provides secure token-based " +
                            "authentication and protects against credential stuffing and brute-force " +
                            "attacks. Your data at rest is stored in Firestore with encryption at rest " +
                            "(AES-256) and encryption in transit (TLS 1.3), ensuring that data is " +
                            "protected both while stored and while being transmitted. Cloudflare Edge " +
                            "Workers are utilized for registration flows and passkey verification, " +
                            "providing distributed processing with DDoS protection and edge-level " +
                            "security. On your device, local data is encrypted via the Android Keystore " +
                            "system, which leverages hardware-backed security modules to protect " +
                            "cryptographic keys. Biometric authentication (fingerprint and face " +
                            "recognition) can be enabled to add an additional layer of protection " +
                            "before the app can be accessed, ensuring that even if your device is " +
                            "physically compromised, your Zixo data remains secure."
                    )
                }
            }

            // ── 6. Data Retention ────────────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "6", title = "Data Retention")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "We follow a strict data minimization and retention policy designed to " +
                            "limit the amount of personal information we hold at any given time. Your " +
                            "account data is retained only while your account is active; once you delete " +
                            "your account, all associated data is scheduled for removal. Encrypted " +
                            "messages are stored on our servers only until they have been successfully " +
                            "delivered to the recipient and subsequently deleted by either the sender " +
                            "or the recipient. If a message is deleted by either party, it is purged " +
                            "from our servers within 24 hours. Call metadata — which includes only the " +
                            "timestamps and participant identifiers of calls, never the content — is " +
                            "retained for 90 days for debugging and quality assurance purposes, after " +
                            "which it is automatically and permanently deleted. Crash reports and " +
                            "anonymized analytics data are retained for 30 days, providing our " +
                            "engineering team sufficient time to investigate and resolve issues while " +
                            "minimizing data exposure. When you request account deletion, all associated " +
                            "data — including profile information, contact lists, and encrypted message " +
                            "artifacts — is permanently removed from our systems within 30 days of the " +
                            "deletion request."
                    )
                }
            }

            // ── 7. Third-Party Services ──────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "7", title = "Third-Party Services")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Zixo integrates with a limited number of carefully vetted third-party " +
                            "services, each of which is governed by their own privacy policies and " +
                            "terms of service. Google Sign-In is used for secure, passwordless " +
                            "authentication; by using Google Sign-In, you also agree to Google's " +
                            "Privacy Policy, which governs how Google handles your authentication " +
                            "credentials and associated data. Firebase, also operated by Google, is " +
                            "used for real-time data synchronization, push notifications, and cloud " +
                            "messaging; Firebase's data processing is subject to Google's Terms of " +
                            "Service and adheres to strict data residency and security standards. " +
                            "Cloudflare Edge Workers are used for processing registration requests and " +
                            "passkey verification at the network edge; Cloudflare's Privacy Policy " +
                            "governs how they process the limited data that passes through their " +
                            "infrastructure. We carefully evaluate each third-party provider to ensure " +
                            "they meet our high standards for data protection, and we do not share " +
                            "any more data with these providers than is strictly necessary for the " +
                            "functionality they provide."
                    )
                }
            }

            // ── 8. Your Rights ───────────────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "8", title = "Your Rights")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Depending on your jurisdiction, you may have certain rights regarding " +
                            "your personal data. We are committed to facilitating the exercise of these " +
                            "rights in a transparent and timely manner. You have the right to access " +
                            "your personal data and receive a copy of the information we hold about you. " +
                            "You have the right to request the rectification of any inaccurate or " +
                            "incomplete personal data we hold. You have the right to erasure, which you " +
                            "can exercise by deleting your Zixo account, triggering the complete removal " +
                            "of your data from our systems. You have the right to data portability, " +
                            "allowing you to receive your personal data in a structured, commonly used, " +
                            "and machine-readable format. You have the right to withdraw consent for any " +
                            "data processing activities that are based on your consent, without " +
                            "affecting the lawfulness of processing carried out prior to withdrawal. " +
                            "Finally, you have the right to lodge a complaint with a supervisory " +
                            "authority — such as the Information Commissioner's Office in the UK or a " +
                            "Data Protection Authority in the EU — if you believe that our processing " +
                            "of your personal data violates applicable data protection laws."
                    )
                }
            }

            // ── 9. Children's Privacy ────────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "9", title = "Children's Privacy")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Zixo is not intended for use by children under the age of 13. We do " +
                            "not knowingly collect, store, or process personal data from children under " +
                            "13 years of age. If we discover that a child under 13 has provided us with " +
                            "personal information, we will take immediate steps to delete such " +
                            "information from our servers and terminate the associated account. Parents " +
                            "or guardians who become aware that their child has created a Zixo account " +
                            "in violation of this policy are encouraged to contact us at " +
                            "privacy@zixo.app so that we can promptly remove the account and all " +
                            "associated data. We encourage parents and guardians to monitor their " +
                            "children's online activities and to discuss safe internet practices with " +
                            "them. For users between the ages of 13 and 18, we recommend parental " +
                            "guidance when using the service, and we limit certain features — such as " +
                            "location sharing — to ensure a safer experience for younger users."
                    )
                }
            }

            // ── 10. International Data Transfers ─────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "10", title = "International Data Transfers")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "Zixo operates on global cloud infrastructure, and your data may be " +
                            "transferred to and processed in countries other than your country of " +
                            "residence. These countries may have different data protection laws than " +
                            "your jurisdiction. When we transfer your data internationally, we ensure " +
                            "that appropriate safeguards are in place to protect your information in " +
                            "accordance with applicable data protection regulations. For users in the " +
                            "European Economic Area (EEA), the United Kingdom, and Switzerland, we rely " +
                            "on Standard Contractual Clauses (SCCs) approved by the European Commission " +
                            "to ensure that data transfers to countries outside the EEA provide an " +
                            "adequate level of protection. We also implement supplementary measures — " +
                            "such as encryption, pseudonymization, and access controls — to further " +
                            "protect your data during international transfers. Google Cloud Platform and " +
                            "Cloudflare, our primary infrastructure providers, maintain comprehensive " +
                            "compliance programs including SOC 2 Type II, ISO 27001, and GDPR " +
                            "certifications, providing additional assurance that your data is handled " +
                            "securely regardless of where it is processed."
                    )
                }
            }

            // ── 11. Changes to This Policy ───────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "11", title = "Changes to This Policy")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "We may update this Privacy Policy from time to time to reflect changes " +
                            "in our practices, technology, legal requirements, or other factors. When we " +
                            "make changes, we will update the \"Last updated\" date at the top of this " +
                            "page and, for material changes, we will provide additional notice through " +
                            "the App — such as an in-app notification or a prominent banner — before the " +
                            "changes take effect. Changes to this Privacy Policy become effective upon " +
                            "posting the revised policy on this page. Your continued use of Zixo after " +
                            "any changes become effective constitutes your acceptance of the revised " +
                            "Privacy Policy. We encourage you to review this page periodically to stay " +
                            "informed about how we protect your information. If we make significant " +
                            "changes that affect your rights, we will provide at least 30 days' advance " +
                            "notice before the new policy takes effect, giving you adequate time to " +
                            "review the changes and exercise your rights if you disagree with them."
                    )
                }
            }

            // ── 12. Contact Us ───────────────────────────────────────────
            item {
                PolicyGlassCard {
                    SectionHeader(number = "12", title = "Contact Us")
                    Spacer(modifier = Modifier.height(10.dp))
                    BodyText(
                        text = "If you have any questions, concerns, or requests regarding this Privacy " +
                            "Policy or our data practices, we encourage you to reach out to us through " +
                            "the appropriate channel listed below. Our dedicated privacy team strives to " +
                            "respond to all inquiries within 14 business days."
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ContactRow(
                        icon = Icons.Default.Email,
                        label = "Privacy Inquiries",
                        value = "privacy@zixo.app"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ContactRow(
                        icon = Icons.Default.Email,
                        label = "Data Deletion Requests",
                        value = "delete@zixo.app"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ContactRow(
                        icon = Icons.Default.Security,
                        label = "Security Vulnerabilities",
                        value = "security@zixo.app"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = NeonMint,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mailing Address",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Zixo Inc.\n123 Privacy Lane\nSan Francisco, CA 94105",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // ── Footer spacing ───────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Composable Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Glass card wrapper that applies the [liquidGlassContainer] modifier and
 * standard padding used for each policy section.
 */
@Composable
private fun PolicyGlassCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassContainer()
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Section header displaying the section number in [NeonMint] followed by
 * the section title in bold [TextPrimary].
 */
@Composable
private fun SectionHeader(
    number: String? = null,
    title: String
) {
    Text(
        text = buildAnnotatedString {
            if (number != null) {
                withStyle(SpanStyle(color = NeonMint, fontWeight = FontWeight.Bold)) {
                    append("$number. ")
                }
            }
            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                append(title)
            }
        },
        fontSize = 20.sp,
        lineHeight = 26.sp
    )
}

/**
 * Subsection title rendered in semi-bold [TextPrimary] at 16.sp.
 */
@Composable
private fun SubsectionTitle(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    )
}

/**
 * Body text rendered in [TextSecondary] at 14.sp with 20.sp line height,
 * matching the Zixo design system's reading-optimized typography.
 */
@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}

/**
 * Contact row displaying an icon, a label, and a value (typically an
 * email address) with consistent styling.
 */
@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonMint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = NeonMint,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
