package com.zixo.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.CallState
import com.zixo.app.ui.theme.AmoledBlack
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

// ════════════════════════════════════════════════════════════════
// Call Screen Overlay — Fullscreen Frosted Glass
// ════════════════════════════════════════════════════════════════

/**
 * Fullscreen frosted glass overlay that pops over the UI during WebRTC calls.
 *
 * Renders different UI based on the current [CallState]:
 *
 * | State                | UI                                                        |
 * |----------------------|-----------------------------------------------------------|
 * | [CallState.DIALING]  | "Calling…" label with pulsing animation                   |
 * | [CallState.RINGING]  | "Incoming Call" with Accept / Decline buttons             |
 * | [CallState.CONNECTED]| Call duration timer + mute/camera/speaker toggles + end   |
 * | [CallState.ENDED]    | Brief "Call Ended" message, then auto-dismiss            |
 *
 * All surfaces use the Liquid Glass design (frosted blur + semi-transparent
 * borders) to prevent the black-screen issue that occurs when a call UI
 * blocks the main thread.
 *
 * @param callId     The unique call identifier.
 * @param callState  The current call state. Defaults to [CallState.DIALING].
 * @param onEndCall  Callback invoked when the user ends/declines the call.
 * @param onAcceptCall Callback invoked when the user accepts an incoming call.
 * @param onToggleMute   Callback invoked when the mute button is toggled.
 * @param onToggleCamera Callback invoked when the camera button is toggled.
 * @param onToggleSpeaker Callback invoked when the speaker button is toggled.
 */
@Composable
fun CallScreenOverlay(
    callId: String,
    callState: CallState = CallState.DIALING(),
    onEndCall: () -> Unit = {},
    onAcceptCall: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
) {
    // ── Auto-dismiss after ENDED state ──────────────────────
    var hasDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(callState) {
        if (callState is CallState.ENDED && !hasDismissed) {
            delay(2000) // Show "Call Ended" for 2 seconds
            hasDismissed = true
            onEndCall()
        }
    }

    // ── Frosted translucent overlay background ─────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = AmoledBlack.copy(alpha = 0.85f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Contact Info ────────────────────────────────
            ContactInfoSection(callState = callState)

            Spacer(modifier = Modifier.height(40.dp))

            // ── Call State Display ──────────────────────────
            when (callState) {
                is CallState.IDLE -> {
                    // Shouldn't happen when overlay is shown
                }

                is CallState.DIALING -> {
                    DialingIndicator()
                }

                is CallState.RINGING -> {
                    RingingIndicator(
                        onAccept = onAcceptCall,
                        onDecline = onEndCall
                    )
                }

                is CallState.CONNECTED -> {
                    ConnectedControls(
                        callState = callState,
                        onToggleMute = onToggleMute,
                        onToggleCamera = onToggleCamera,
                        onToggleSpeaker = onToggleSpeaker,
                        onEndCall = onEndCall
                    )
                }

                is CallState.ENDED -> {
                    EndedIndicator(callState = callState)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Contact Info Section
// ════════════════════════════════════════════════════════════════

/**
 * Displays the contact avatar, display name, and call type (audio/video).
 */
@Composable
private fun ContactInfoSection(callState: CallState) {
    val displayName = when (callState) {
        is CallState.DIALING -> callState.targetDisplayName
        is CallState.RINGING -> callState.callerDisplayName
        is CallState.CONNECTED -> callState.targetDisplayName
        else -> "Unknown"
    }

    val isVideoCall = when (callState) {
        is CallState.DIALING -> callState.isVideoCall
        is CallState.RINGING -> callState.isVideoCall
        is CallState.CONNECTED -> callState.isVideoCall
        else -> false
    }

    // ── Avatar Circle ──
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(GlassSurfaceColor)
            .border(2.dp, GlassBorderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.take(2).uppercase(),
            color = NeonMint,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ── Display Name ──
    Text(
        text = displayName.ifBlank { "Unknown" },
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    // ── Call Type Badge ──
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isVideoCall) Icons.Outlined.Videocam else Icons.Filled.Phone,
            contentDescription = null,
            tint = NeonMint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isVideoCall) "Video Call" else "Audio Call",
            color = NeonMint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ════════════════════════════════════════════════════════════════
// DIALING State — "Calling…" with Pulsing Animation
// ════════════════════════════════════════════════════════════════

/**
 * Pulsing "Calling…" indicator shown while waiting for the remote peer.
 */
@Composable
private fun DialingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dialing_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dialing_alpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dialing_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing ring
        Box(
            modifier = Modifier
                .size((80.dp * pulseScale))
                .clip(CircleShape)
                .background(NeonMint.copy(alpha = pulseAlpha * 0.2f))
                .border(
                    width = 2.dp,
                    color = NeonMint.copy(alpha = pulseAlpha),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = NeonMint.copy(alpha = pulseAlpha),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calling…",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ════════════════════════════════════════════════════════════════
// RINGING State — Incoming Call with Accept/Decline
// ════════════════════════════════════════════════════════════════

/**
 * Incoming call UI with "Incoming Call" label and Accept/Decline buttons.
 */
@Composable
private fun RingingIndicator(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringing_alpha"
    )

    Text(
        text = "Incoming Call",
        color = NeonMint.copy(alpha = pulseAlpha),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(40.dp))

    // ── Accept / Decline Buttons ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decline Button (Red)
        GlassCallButton(
            icon = Icons.Filled.CallEnd,
            contentDescription = "Decline",
            backgroundColor = Color(0xFFFF3B30),
            iconTint = Color.White,
            onClick = onDecline
        )

        // Accept Button (Green)
        GlassCallButton(
            icon = Icons.Filled.Phone,
            contentDescription = "Accept",
            backgroundColor = NeonMint,
            iconTint = Color(0xFF003A1F),
            onClick = onAccept
        )
    }
}

// ════════════════════════════════════════════════════════════════
// CONNECTED State — Duration Timer + Mute/Camera/Speaker Toggles
// ════════════════════════════════════════════════════════════════

/**
 * Active call controls shown while the call is connected.
 *
 * Includes:
 * - Call duration timer
 * - Mute toggle (mic icon)
 * - Camera toggle (video icon)
 * - Speaker toggle (speaker icon)
 * - End call button (red circle with phone icon)
 */
@Composable
private fun ConnectedControls(
    callState: CallState.CONNECTED,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
) {
    // ── Call Duration Timer ──
    CallDurationTimer(connectedAt = callState.connectedAt)

    Spacer(modifier = Modifier.height(48.dp))

    // ── Toggle Buttons Row ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute Toggle
        GlassToggleCallButton(
            icon = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = if (callState.isMuted) "Unmute" else "Mute",
            isActive = callState.isMuted,
            onClick = onToggleMute
        )

        // Camera Toggle
        GlassToggleCallButton(
            icon = if (callState.isCameraOff) Icons.Outlined.VideocamOff else Icons.Outlined.Videocam,
            contentDescription = if (callState.isCameraOff) "Turn camera on" else "Turn camera off",
            isActive = callState.isCameraOff,
            onClick = onToggleCamera
        )

        // Speaker Toggle
        GlassToggleCallButton(
            icon = if (callState.isSpeakerOn) Icons.Filled.VolumeUp else Icons.Outlined.VolumeOff,
            contentDescription = if (callState.isSpeakerOn) "Speaker off" else "Speaker on",
            isActive = callState.isSpeakerOn,
            onClick = onToggleSpeaker
        )
    }

    Spacer(modifier = Modifier.height(48.dp))

    // ── End Call Button ──
    GlassCallButton(
        icon = Icons.Filled.CallEnd,
        contentDescription = "End call",
        backgroundColor = Color(0xFFFF3B30),
        iconTint = Color.White,
        size = 64.dp,
        onClick = onEndCall
    )
}

// ════════════════════════════════════════════════════════════════
// ENDED State — Brief "Call Ended" Message
// ════════════════════════════════════════════════════════════════

/**
 * Brief "Call Ended" message shown for 2 seconds before auto-dismiss.
 */
@Composable
private fun EndedIndicator(callState: CallState.ENDED) {
    val durationText = formatDuration(callState.durationSeconds)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Call Ended",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (callState.durationSeconds > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = durationText,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Call Duration Timer
// ════════════════════════════════════════════════════════════════

/**
 * Live-updating call duration timer.
 *
 * Ticks every second starting from [connectedAt] timestamp.
 */
@Composable
private fun CallDurationTimer(connectedAt: Long) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(connectedAt) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - connectedAt) / 1000
            delay(1000)
        }
    }

    Text(
        text = formatDuration(elapsedSeconds),
        color = TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 2.sp
    )
}

// ════════════════════════════════════════════════════════════════
// Glass Call Button Components
// ════════════════════════════════════════════════════════════════

/** Semi-transparent dark surface for liquid glass panels. */
private val GlassSurfaceColor = Color(0x1A1A2A32)
/** High-gloss border for liquid glass panels. */
private val GlassBorderColor = Color(0x33FFFFFF)

/**
 * Circular glass-styled call action button.
 *
 * Used for Accept, Decline, and End Call buttons with
 * distinct background colors per action.
 *
 * @param icon             The icon to display.
 * @param contentDescription Accessibility description.
 * @param backgroundColor  Circle background color.
 * @param iconTint         Icon tint color.
 * @param size             Button size in dp (default 56.dp).
 * @param onClick          Click callback.
 */
@Composable
private fun GlassCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconTint: Color,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, GlassBorderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Circular glass-styled toggle button for mute/camera/speaker.
 *
 * When [isActive] is true, the button shows a highlighted state
 * with the [NeonMint] accent. When inactive, it shows a subtle
 * glass surface.
 *
 * @param icon             The icon to display.
 * @param contentDescription Accessibility description.
 * @param isActive         Whether the toggle is currently active.
 * @param onClick          Click callback.
 */
@Composable
private fun GlassToggleCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isActive) NeonMint.copy(alpha = 0.25f) else GlassSurfaceColor
    val borderColor = if (isActive) NeonMint.copy(alpha = 0.5f) else GlassBorderColor
    val iconTint = if (isActive) NeonMint else TextSecondary

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Duration Formatter
// ════════════════════════════════════════════════════════════════

/**
 * Formats a duration in seconds to "MM:SS" or "HH:MM:SS".
 */
private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
