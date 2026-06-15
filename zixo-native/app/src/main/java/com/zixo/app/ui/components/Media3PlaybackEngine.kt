package com.zixo.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zixo.app.ui.theme.NeonMint
import timber.log.Timber
import java.util.Locale

/**
 * Media3 ExoPlayer integration for inline audio voice note playback.
 *
 * Features:
 * - Play/pause toggle with NeonMint accent
 * - Seekbar with real-time position tracking
 * - Duration/position text display
 * - Playback completion callback
 * - Error handling with onPlaybackError callback
 * - Automatic resource release on composition disposal
 * - Wrapped in Liquid Glass styled container
 */
@Composable
fun AudioPlaybackEngine(
    url: String,
    isPlaying: Boolean = false,
    onPlaybackComplete: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    var isCurrentlyPlaying by remember { mutableStateOf(isPlaying) }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    DisposableEffect(url) {
        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer

        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isCurrentlyPlaying = false
                    onPlaybackComplete()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "AudioPlaybackEngine: Playback error")
                isCurrentlyPlaying = false
                onPlaybackError(error.localizedMessage ?: "Playback failed")
            }
        })

        onDispose {
            try {
                exoPlayer.release()
                Timber.d("AudioPlaybackEngine: Player released")
            } catch (e: Exception) {
                Timber.e(e, "AudioPlaybackEngine: Error releasing player")
            }
        }
    }

    LaunchedEffect(isCurrentlyPlaying) {
        player?.let { exoPlayer ->
            if (isCurrentlyPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            kotlinx.coroutines.delay(200)
            player?.let { exoPlayer ->
                if (exoPlayer.duration > 0) {
                    duration = exoPlayer.duration
                    currentPosition = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x3B1A2A32), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { isCurrentlyPlaying = !isCurrentlyPlaying },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isCurrentlyPlaying) NeonMint.copy(alpha = pulseAlpha) else NeonMint.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { currentPosition.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NeonMint,
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration((currentPosition * duration).toLong()),
                        color = Color(0xFFA1B0B3),
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatDuration(duration),
                        color = Color(0xFFA1B0B3),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Media3 ExoPlayer integration for video playback in status and chat.
 *
 * Features:
 * - PlayerView in AndroidView for Compose interop
 * - Adaptive bitrate with DefaultLoadControl
 * - Mute toggle support
 * - Automatic resource release
 * - Error handling callback
 */
@Composable
fun VideoPlaybackEngine(
    url: String,
    isMuted: Boolean = false,
    onPlaybackComplete: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(url) {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(2000, 5000, 1000, 1000)
            .build()

        val exoPlayer = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()

        player = exoPlayer

        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackComplete()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "VideoPlaybackEngine: Playback error")
                onPlaybackError(error.localizedMessage ?: "Video playback failed")
            }
        })

        onDispose {
            try {
                exoPlayer.release()
                Timber.d("VideoPlaybackEngine: Player released")
            } catch (e: Exception) {
                Timber.e(e, "VideoPlaybackEngine: Error releasing player")
            }
        }
    }

    LaunchedEffect(isMuted) {
        player?.volume = if (isMuted) 0f else 1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        player?.let { exoPlayer ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = true
                        controllerShowTimeoutMs = 3000
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
}

private fun Modifier.border(
    width: dp: Int,
    color: Color,
    shape: RoundedCornerShape
): Modifier = this.then(
    Modifier.border(width.dp, color, shape)
)
