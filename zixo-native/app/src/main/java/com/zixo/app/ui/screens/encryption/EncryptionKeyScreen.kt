package com.zixo.app.ui.screens.encryption

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ──────────────────────────────────────────────
// Encryption Key Screen
// ──────────────────────────────────────────────

@Composable
fun EncryptionKeyScreen(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Top Bar ────────────────────────────────
            ZixoTopBar(
                title = "Encryption Key",
                showBackButton = true,
                onBackClick = onBackClick,
            )

            // ── Section Header ─────────────────────────
            SectionHeader(title = "End-to-End Encryption")

            // ── Verification Status Card ───────────────
            VerificationStatusCard()

            Spacer(modifier = Modifier.height(12.dp))

            // ── E2EE Explanation ───────────────────────
            E2EEExplanationCard()

            Spacer(modifier = Modifier.height(12.dp))

            // ── Encryption Key Fingerprint ─────────────
            EncryptionKeyFingerprintCard()

            Spacer(modifier = Modifier.height(8.dp))

            // ── Verify Key with Contact ────────────────
            NavigationItem(
                title = "Verify Key with Contact",
                subtitle = "Compare keys for a face-to-face verification",
                icon = Icons.Filled.VerifiedUser,
                onClick = { /* Placeholder — will navigate to contact picker */ },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────
// Verification Status Card
// ──────────────────────────────────────────────

@Composable
private fun VerificationStatusCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkPetrolCharcoal,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EnhancedEncryption,
                contentDescription = null,
                tint = NeonMint,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Encryption Verified",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Your messages are end-to-end encrypted",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// E2EE Explanation Card
// ──────────────────────────────────────────────

@Composable
private fun E2EEExplanationCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkPetrolCharcoal,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) {
                        append("How it works")
                    }
                },
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zixo uses end-to-end encryption to protect your messages and calls. " +
                        "Only you and the person you're communicating with can read or listen " +
                        "to your content — not even Zixo has access.",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 19.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Each conversation uses a unique encryption key pair. " +
                        "Your encryption key fingerprint below is derived from your public key " +
                        "and can be shared openly — it cannot be used to decrypt your messages.",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 19.sp,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Encryption Key Fingerprint Card
// ──────────────────────────────────────────────

@Composable
private fun EncryptionKeyFingerprintCard() {
    // Placeholder fingerprint — in production this would come from the crypto module
    val fingerprintHex = remember {
        generatePlaceholderFingerprint()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkPetrolCharcoal,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = "YOUR KEY FINGERPRINT",
                color = NeonMint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Display hex fingerprint in groups of 8 for readability
            val formattedFingerprint = formatFingerprintHex(fingerprintHex)

            Text(
                text = formattedFingerprint,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "64 characters · SHA-256",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────

/**
 * Generates a deterministic-looking 64-character hex string as a placeholder
 * for the encryption key fingerprint. In production, this would be replaced
 * by the actual fingerprint from the signal/crypto module.
 */
private fun generatePlaceholderFingerprint(): String {
    // Fixed placeholder value so previews remain stable
    return "A4F2E81B3C07D9564E2A1F8BD3C7E09A" +
            "6B1D4E8F2A3C7B5D9E1F4A8C2B6D0E3"
}

/**
 * Formats a 64-character hex string into groups of 8 for readability,
 * splitting across two lines.
 *
 * Example:
 *   A4F2E81B 3C07D956 4E2A1F8B D3C7E09A
 *   6B1D4E8F 2A3C7B5D 9E1F4A8C 2B6D0E3
 */
private fun formatFingerprintHex(hex: String): String {
    val chunkSize = 8
    val chunks = hex.chunked(chunkSize)

    // First line: chunks 0–3, second line: chunks 4–7
    val line1 = chunks.take(4).joinToString("  ")
    val line2 = chunks.drop(4).take(4).joinToString("  ")

    return if (line2.isNotBlank()) "$line1\n$line2" else line1
}
