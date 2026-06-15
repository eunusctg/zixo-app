package com.zixo.app.ui.components

import android.view.ViewGroup
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zixo.app.data.remote.webrtc.WebRtcClient
import com.zixo.app.domain.model.CallState
import com.zixo.app.ui.theme.AmoledBlack
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

// ════════════════════════════════════════════════════════════════
// Call Screen Overlay — Fullscreen Frosted Glass with Video
// ════════════════════════════════════════════════════════════════

/**
 * Fullscreen frosted glass overlay that pops over the UI during WebRTC calls.
 *
 * ## Crash-Proof Video Rendering:
 *
 * The [SurfaceViewRenderer] for video is wrapped inside an [AndroidView].
 * The hardware-accelerated root EglBase context is retained using the DI
 * Singleton [WebRtcClient] — it is **NEVER** re-initialized on Composable
 * recomposition, as that would trigger an uncatchable SIGABRT crash in the
 * native WebRTC layer.
 *
 * The SurfaceViewRenderer is created once per composition using `remember`
 * and initialized with the singleton EglBase context from [WebRtcClient].
 *
 * Renders different UI based on the current [CallState]:
 *
 * | State                | UI                                                        |
 * |----------------------|-----------------------------------------------------------|
 * | [CallState.DIALING]  | "Calling…" label with pulsing animation                   |
 * | [CallState.RINGING]  | "Incoming Call" with Accept / Decline buttons             |
 * | [CallState.CONNECTED]| Video views + duration timer + controls + end             |
 * | [CallState.ENDED]    | Brief "Call Ended" message, then auto-dismiss            |
 *
 * @param callId         The unique call identifier.
 * @param callState      The current call state.
 * @param webRtcClient   The singleton WebRTC client for video rendering.
 * @param isVideoCall    Whether this is a video call.
 * @param onEndCall      Callback invoked when the user ends/declines the call.
 * @param onAcceptCall   Callback invoked when the user accepts an incoming call.
 * @param onToggleMute   Callback invoked when the mute button is toggled.
 * @param onToggleCamera Callback invoked when the camera button is toggled.
 * @param onToggleSpeaker Callback invoked when the speaker button is toggled.
 * @param onSwitchCamera Callback invoked when the camera switch button is pressed.
 */
@Composable
fun CallScreenOverlay(
    callId: String,
    callState: CallState = CallState.DIALING(),
    webRtcClient: WebRtcClient? = null,
    isVideoCall: Boolean = false,
    onEndCall: () -> Unit = {},
    onAcceptCall: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
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
            .background(color = AmoledBlack.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // ── Video Rendering Layer (EglBase Singleton Preservation) ──
        if (isVideoCall && callState is CallState.CONNECTED && webRtcClient != null) {
            VideoRenderLayer(
                webRtcClient = webRtcClient,
                callState = callState
            )
        }

        // ── UI Controls Layer ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .zIndex(if (isVideoCall && callState is CallState.CONNECTED) 1f else 0f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Contact Info ────────────────────────────────
            ContactInfoSection(callState = callState)

            Spacer(modifier = Modifier.height(40.dp))

            // ── Call State Display ──────────────────────────
            when (callState) {
                is CallState.IDLE -> { /* Shouldn't happen when overlay is shown */ }

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
                        isVideoCall = isVideoCall,
                        onToggleMute = onToggleMute,
                        onToggleCamera = onToggleCamera,
                        onToggleSpeaker = onToggleSpeaker,
                        onSwitchCamera = onSwitchCamera,
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
// Video Rendering Layer — EglBase Singleton + SurfaceViewRenderer
// ════════════════════════════════════════════════════════════════

/**
 * Renders the remote and local video tracks using [SurfaceViewRenderer]
 * wrapped inside [AndroidView].
 *
 * ## Critical Rules for Crash Prevention:
 *
 * 1. **EglBase Singleton**: The [WebRtcClient.eglBaseContext] is a DI Singleton
 *    that is NEVER re-created on recomposition. Re-creating EglBase triggers
 *    an uncatchable SIGABRT crash in the native WebRTC layer.
 *
 * 2. **SurfaceViewRenderer lifecycle**: Created once using `remember`, initialized
 *    with the singleton EglBase context. Released on disposal via `DisposableEffect`.
 *
 * 3. **AndroidView wrapper**: The native SurfaceViewRenderer is embedded inside
 *    Compose using `AndroidView`, which properly manages the View lifecycle.
 *
 * @param webRtcClient The singleton WebRTC client.
 * @param callState    The current CONNECTED call state.
 */
@Composable
private fun VideoRenderLayer(
    webRtcClient: WebRtcClient,
    callState: CallState.CONNECTED
) {
    // ── Remote Video (Full Screen) ──
    val remoteSurfaceView = remember { SurfaceViewRenderer(LocalContext.current) }

    DisposableEffect(remoteSurfaceView) {
        webRtcClient.initRemoteVideoRenderer(remoteSurfaceView)
        onDispose {
            webRtcClient.releaseVideoRenderer(remoteSurfaceView, isLocal = false)
        }
    }

    // Remote video fills the entire background
    AndroidView(
        factory = { remoteSurfaceView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            try {
                webRtcClient.initRemoteVideoRenderer(view)
            } catch (_: Exception) {
                // Renderer may already be initialized
            }
        }
    )

    // ── Local Video (Picture-in-Picture) ──
    if (!callState.isCameraOff) {
        val localSurfaceView = remember { SurfaceViewRenderer(LocalContext.current) }

        DisposableEffect(localSurfaceView) {
            webRtcClient.initLocalVideoRenderer(localSurfaceView)
            onDispose {
                webRtcClient.releaseVideoRenderer(localSurfaceView, isLocal = true)
            }
        }

        // Local video as a small PiP in the top-right corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            AndroidView(
                factory = { localSurfaceView },
                modifier = Modifier
                    .size(width = 120.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, NeonMint.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                update = { view ->
                    try {
                        webRtcClient.initLocalVideoRenderer(view)
                    } catch (_: Exception) {
                        // Renderer may already be initialized
                    }
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Contact Info Section
// ════════════════════════════════════════════════════════════════

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

    Text(
        text = displayName.ifBlank { "Unknown" },
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

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
// DIALING State
// ════════════════════════════════════════════════════════════════

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

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size((80.dp * pulseScale))
                .clip(CircleShape)
                .background(NeonMint.copy(alpha = pulseAlpha * 0.2f))
                .border(2.dp, NeonMint.copy(alpha = pulseAlpha), CircleShape),
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
        Text(text = "Calling…", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// ════════════════════════════════════════════════════════════════
// RINGING State
// ════════════════════════════════════════════════════════════════

@Composable
private fun RingingIndicator(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "ringing_alpha"
    )

    Text(
        text = "Incoming Call",
        color = NeonMint.copy(alpha = pulseAlpha),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(40.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCallButton(
            icon = Icons.Filled.CallEnd,
            contentDescription = "Decline",
            backgroundColor = Color(0xFFFF3B30),
            iconTint = Color.White,
            onClick = onDecline
        )
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
// CONNECTED State
// ════════════════════════════════════════════════════════════════

@Composable
private fun ConnectedControls(
    callState: CallState.CONNECTED,
    isVideoCall: Boolean,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit,
) {
    CallDurationTimer(connectedAt = callState.connectedAt)

    Spacer(modifier = Modifier.height(48.dp))

    // ── Toggle Buttons ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassToggleCallButton(
            icon = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = if (callState.isMuted) "Unmute" else "Mute",
            isActive = callState.isMuted,
            onClick = onToggleMute
        )

        if (isVideoCall) {
            GlassToggleCallButton(
                icon = if (callState.isCameraOff) Icons.Outlined.VideocamOff else Icons.Outlined.Videocam,
                contentDescription = if (callState.isCameraOff) "Camera on" else "Camera off",
                isActive = callState.isCameraOff,
                onClick = onToggleCamera
            )

            // Switch camera button
            GlassToggleCallButton(
                icon = Icons.Filled.SwitchCamera,
                contentDescription = "Switch camera",
                isActive = false,
                onClick = onSwitchCamera
            )
        }

        GlassToggleCallButton(
            icon = if (callState.isSpeakerOn) Icons.Filled.VolumeUp else Icons.Outlined.VolumeOff,
            contentDescription = if (callState.isSpeakerOn) "Speaker off" else "Speaker on",
            isActive = callState.isSpeakerOn,
            onClick = onToggleSpeaker
        )
    }

    Spacer(modifier = Modifier.height(48.dp))

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
// ENDED State
// ════════════════════════════════════════════════════════════════

@Composable
private fun EndedIndicator(callState: CallState.ENDED) {
    val durationText = formatDuration(callState.durationSeconds)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Call Ended", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (callState.durationSeconds > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = durationText, color = TextSecondary, fontSize = 14.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Call Duration Timer
// ════════════════════════════════════════════════════════════════

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

private val GlassSurfaceColor = Color(0x1A1A2A32)
private val GlassBorderColor = Color(0x33FFFFFF)

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
