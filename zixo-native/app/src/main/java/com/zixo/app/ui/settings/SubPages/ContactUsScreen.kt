package com.zixo.app.ui.settings.SubPages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Private Color Constants
// ─────────────────────────────────────────────────────────────────────────────

private val CardSurfaceColor = Color(0xFF1A2A32)
private val EmergencyRed = Color(0xFFFF5252)
private val EmergencyCardBackground = Color(0x1AFF5252)
private val EmergencyCardBorder = Color(0x33FF5252)

// ─────────────────────────────────────────────────────────────────────────────
// Contact Us Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen "Contact Us" page rendered with the Zixo Liquid Glass design language.
 *
 * Sections:
 * 1. Header — Icon, title, subtitle
 * 2. Support Channels — Email, Live Chat, Twitter/X, Instagram
 * 3. Feedback Form — Subject dropdown, message field, submit button
 * 4. FAQ Quick Links — Expandable question/answer pairs
 * 5. Emergency — Urgent security issue reporting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ── Form State ──
    var selectedSubject by remember { mutableStateOf("") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // ── FAQ Expansion States ──
    var faqExpanded1 by remember { mutableStateOf(false) }
    var faqExpanded2 by remember { mutableStateOf(false) }
    var faqExpanded3 by remember { mutableStateOf(false) }
    var faqExpanded4 by remember { mutableStateOf(false) }

    val subjectOptions = listOf(
        "Bug Report",
        "Feature Request",
        "Account Issue",
        "General Feedback",
        "Partnership Inquiry"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ── Top Bar ──
        ZixoTopBar(
            title = "Contact Us",
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ──────────────────────────────────────────────────────────────
            // 1. Header Section
            // ──────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NeonMint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDCDE",
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Get in Touch",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "We'd love to hear from you. Choose your preferred way to reach us.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // ──────────────────────────────────────────────────────────────
            // 2. Contact Methods Section
            // ──────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                Text(
                    text = "SUPPORT CHANNELS",
                    color = NeonMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Email Support
                ContactMethodRow(
                    icon = "\uD83D\uDCE7",
                    title = "Email Support",
                    subtitle = "support@zixo.app",
                    description = "Typically responds within 24 hours",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:support@zixo.app")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No email app found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Live Chat
                ContactMethodRow(
                    icon = "\uD83D\uDCAC",
                    title = "Live Chat",
                    subtitle = "Available in-app",
                    description = "Monday to Friday, 9 AM – 6 PM UTC",
                    onClick = {
                        Toast.makeText(
                            context,
                            "Live chat coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Twitter / X
                ContactMethodRow(
                    icon = "\uD83D\uDC26",
                    title = "Twitter / X",
                    subtitle = "@ZixoApp",
                    description = "Follow us for updates and announcements",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://twitter.com/ZixoApp")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Unable to open browser",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Instagram
                ContactMethodRow(
                    icon = "\uD83D\uDCF8",
                    title = "Instagram",
                    subtitle = "@zixo.messenger",
                    description = "Tips, features, and community highlights",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://instagram.com/zixo.messenger")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Unable to open browser",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            // ──────────────────────────────────────────────────────────────
            // 3. Feedback Form Section
            // ──────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                Text(
                    text = "SEND FEEDBACK",
                    color = NeonMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = "Subject",
                                color = if (selectedSubject.isEmpty()) TextSecondary else NeonMint,
                                fontSize = 14.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Select a subject",
                                color = TextSecondary.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = subjectExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x0DFFFFFF),
                            unfocusedContainerColor = Color(0x08FFFFFF),
                            cursorColor = NeonMint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = if (selectedSubject.isEmpty()) TextSecondary else TextPrimary,
                            disabledTextColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = NeonMint,
                            unfocusedLabelColor = TextSecondary,
                            focusedTrailingIconColor = NeonMint,
                            unfocusedTrailingIconColor = TextSecondary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false },
                        containerColor = CardSurfaceColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        subjectOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = if (option == selectedSubject) NeonMint else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (option == selectedSubject) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedSubject = option
                                    subjectExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Message Text Field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { newText ->
                        if (newText.length <= 500) {
                            messageText = newText
                        }
                    },
                    label = {
                        Text(
                            text = "Your Message",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Tell us what's on your mind...",
                            color = TextSecondary.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMint,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedContainerColor = Color(0x0DFFFFFF),
                        unfocusedContainerColor = Color(0x08FFFFFF),
                        cursorColor = NeonMint,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        disabledTextColor = TextSecondary.copy(alpha = 0.5f),
                        focusedLabelColor = NeonMint,
                        unfocusedLabelColor = TextSecondary,
                        focusedPlaceholderColor = TextSecondary.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )

                // Character counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${messageText.length}/500",
                        color = if (messageText.length >= 450) EmergencyRed else TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (selectedSubject.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please select a subject",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (messageText.isBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter your message",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isSubmitting = true
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@zixo.app")
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "[$selectedSubject] — Zixo App Feedback"
                            )
                            putExtra(Intent.EXTRA_TEXT, messageText)
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No email app found. Feedback noted!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isSubmitting = false
                        selectedSubject = ""
                        messageText = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonMint,
                        contentColor = Color(0xFF0B1519),
                        disabledContainerColor = NeonMint.copy(alpha = 0.4f),
                        disabledContentColor = Color(0xFF0B1519).copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (isSubmitting) "Sending..." else "Send Feedback",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ──────────────────────────────────────────────────────────────
            // 4. FAQ Quick Links Section
            // ──────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassCard()
                    .padding(16.dp)
            ) {
                Text(
                    text = "FREQUENTLY ASKED",
                    color = NeonMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                FaqItem(
                    question = "How do I reset my passkey?",
                    answer = "Go to Settings → Account & Security → Passkeys and follow the re-registration flow. You'll verify your identity via Google Sign-In, then create a new passkey.",
                    isExpanded = faqExpanded1,
                    onToggle = { faqExpanded1 = !faqExpanded1 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FaqItem(
                    question = "Why can't I message someone?",
                    answer = "Zixo uses a zero-trust contact model. You can only communicate with mutual contacts. Both users must add each other using their 8-digit Zixo Number.",
                    isExpanded = faqExpanded2,
                    onToggle = { faqExpanded2 = !faqExpanded2 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FaqItem(
                    question = "Is my data encrypted?",
                    answer = "Yes. All messages use Signal Protocol X3DH + Double Ratchet encryption. Only you and your recipient can read messages — not even Zixo can access them.",
                    isExpanded = faqExpanded3,
                    onToggle = { faqExpanded3 = !faqExpanded3 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FaqItem(
                    question = "How do I report a bug?",
                    answer = "Use the feedback form above with 'Bug Report' selected as the subject. Include steps to reproduce and your device model for faster resolution.",
                    isExpanded = faqExpanded4,
                    onToggle = { faqExpanded4 = !faqExpanded4 }
                )
            }

            // ──────────────────────────────────────────────────────────────
            // 5. Emergency Section
            // ──────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EmergencyCardBackground)
                    .border(
                        width = 1.dp,
                        color = EmergencyCardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "URGENT SECURITY ISSUE",
                    color = EmergencyRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "If you've discovered a security vulnerability, please report it responsibly to security@zixo.app. We offer bug bounties and will respond within 24 hours.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmergencyRed.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = EmergencyRed.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:security@zixo.app")
                                )
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No email app found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Report Security Issue",
                        color = EmergencyRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Report security issue",
                        tint = EmergencyRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contact Method Row
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single contact method row with icon, title, subtitle, and description.
 *
 * Displays as a clickable card with a rounded shape, ripple effect, and
 * subtle background for hover/press feedback.
 *
 * @param icon        The emoji icon string displayed in a circular container.
 * @param title       The primary label for this contact method.
 * @param subtitle    The secondary label (e.g. email address or handle).
 * @param description A brief description of availability or response time.
 * @param onClick     Callback invoked when the row is tapped.
 */
@Composable
private fun ContactMethodRow(
    icon: String,
    title: String,
    subtitle: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeonMint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NeonMint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Chevron indicator
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open $title",
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAQ Item
// ─────────────────────────────────────────────────────────────────────────────

/**
 * An expandable FAQ item with a question header and animated answer body.
 *
 * Tapping the row toggles the answer visibility with a smooth vertical
 * expand/collapse animation. A chevron icon rotates to indicate the
 * expansion state.
 *
 * @param question    The FAQ question text.
 * @param answer      The FAQ answer text revealed on expansion.
 * @param isExpanded  Whether the answer is currently visible.
 * @param onToggle    Callback invoked when the row is tapped.
 */
@Composable
private fun FaqItem(
    question: String,
    answer: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = question,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = NeonMint,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
