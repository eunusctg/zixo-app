package com.zexo.app.ui.screens.calls

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.database.FirebaseDatabase
import com.zexo.app.data.model.CallRecord
import com.zexo.app.data.model.CallSignal
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.CallRepository
import com.zexo.app.data.repository.UserRepository
import com.zexo.app.ui.theme.*
import com.zexo.app.services.CallService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Call State
// ─────────────────────────────────────────────────────────────────────────────

enum class CallStatus {
    RINGING, CONNECTING, CONNECTED, ENDED, DECLINED, MISSED
}

data class CallUiState(
    val callId: String = "",
    val isCaller: Boolean = false,
    val isVideoCall: Boolean = false,
    val status: CallStatus = CallStatus.RINGING,
    val otherUser: User? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val durationSeconds: Long = 0L
)

// ─────────────────────────────────────────────────────────────────────────────
// Call ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@dagger.hilt.android.lifecycle.HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val rtdb: FirebaseDatabase
) : androidx.lifecycle.ViewModel() {

    private val _uiState = mutableStateOf(CallUiState())
    val uiState: State<CallUiState> = _uiState

    private var callId: String = ""
    private var isCaller: Boolean = false
    private var otherUserId: String = ""
    private var callStartTime: Long = 0L
    private var timerJob: kotlinx.coroutines.Job? = null
    private var signalListener: com.google.firebase.database.ValueEventListener? = null

    fun initOutgoingCall(receiverId: String, isVideo: Boolean) {
        val uid = authRepository.currentUid ?: return
        isCaller = true
        otherUserId = receiverId
        _uiState.value = _uiState.value.copy(
            isCaller = true,
            isVideoCall = isVideo,
            isVideoEnabled = isVideo,
            status = CallStatus.RINGING
        )

        // Fetch other user
        kotlinx.coroutines.MainScope().launch {
            userRepository.getUserByUid(receiverId).onSuccess { user ->
                _uiState.value = _uiState.value.copy(otherUser = user)
            }
        }

        // Create signal in RTDB
        val userName = authRepository.currentUser?.displayName ?: "Unknown"
        val signal = CallSignal(
            callerId = uid,
            callerName = userName,
            receiverId = receiverId,
            type = if (isVideo) "video" else "audio",
            status = "ringing"
        )

        kotlinx.coroutines.MainScope().launch {
            callRepository.createCallSignal(signal).onSuccess { id ->
                callId = id
                _uiState.value = _uiState.value.copy(callId = id)
                listenForCallResponse(id)
            }
        }
    }

    fun initIncomingCall(existingCallId: String, callerId: String, isVideo: Boolean) {
        val uid = authRepository.currentUid ?: return
        isCaller = false
        otherUserId = callerId
        callId = existingCallId
        _uiState.value = _uiState.value.copy(
            callId = existingCallId,
            isCaller = false,
            isVideoCall = isVideo,
            isVideoEnabled = isVideo,
            status = CallStatus.RINGING
        )

        kotlinx.coroutines.MainScope().launch {
            userRepository.getUserByUid(callerId).onSuccess { user ->
                _uiState.value = _uiState.value.copy(otherUser = user)
            }
        }

        listenForCallCancel(existingCallId)
    }

    private fun listenForCallResponse(cid: String) {
        val ref = rtdb.getReference("calls").child(cid).child("status")
        signalListener = ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                when (status) {
                    "connected" -> onCallConnected()
                    "declined" -> onCallDeclined()
                    "ended" -> onCallEnded()
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun listenForCallCancel(cid: String) {
        val ref = rtdb.getReference("calls").child(cid).child("status")
        signalListener = ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                when (status) {
                    "ended" -> onCallEnded()
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun answerCall() {
        _uiState.value = _uiState.value.copy(status = CallStatus.CONNECTING)
        rtdb.getReference("calls").child(callId).child("status").setValue("connected")
        onCallConnected()
    }

    fun declineCall() {
        rtdb.getReference("calls").child(callId).child("status").setValue("declined")
        _uiState.value = _uiState.value.copy(status = CallStatus.DECLINED)
        cleanup()
    }

    fun endCall() {
        if (_uiState.value.status == CallStatus.CONNECTED) {
            rtdb.getReference("calls").child(callId).child("status").setValue("ended")
        } else {
            kotlinx.coroutines.MainScope().launch { callRepository.endCallSignal(callId) }
        }
        onCallEnded()
    }

    fun toggleMute() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun toggleSpeaker() {
        _uiState.value = _uiState.value.copy(isSpeakerOn = !_uiState.value.isSpeakerOn)
    }

    fun toggleVideo() {
        _uiState.value = _uiState.value.copy(isVideoEnabled = !_uiState.value.isVideoEnabled)
    }

    private fun onCallConnected() {
        callStartTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(status = CallStatus.CONNECTED)
        startTimer()
    }

    private fun onCallDeclined() {
        _uiState.value = _uiState.value.copy(status = CallStatus.DECLINED)
        cleanup()
    }

    private fun onCallEnded() {
        _uiState.value = _uiState.value.copy(status = CallStatus.ENDED)
        saveCallRecord()
        cleanup()
    }

    private fun startTimer() {
        timerJob = kotlinx.coroutines.MainScope().launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                _uiState.value = _uiState.value.copy(durationSeconds = elapsed)
                delay(1000L)
            }
        }
    }

    private fun saveCallRecord() {
        val state = _uiState.value
        val uid = authRepository.currentUid ?: return
        val other = state.otherUser ?: return

        val record = CallRecord(
            callerId = if (isCaller) uid else otherUserId,
            callerName = if (isCaller) (authRepository.currentUser?.displayName ?: "") else other.displayName,
            callerAvatar = if (isCaller) "" else other.avatar,
            receiverId = if (isCaller) otherUserId else uid,
            receiverName = if (isCaller) other.displayName else (authRepository.currentUser?.displayName ?: ""),
            receiverAvatar = if (isCaller) other.avatar else "",
            type = if (state.isVideoCall) "video" else "audio",
            direction = if (isCaller) "outgoing" else "incoming",
            duration = state.durationSeconds
        )

        kotlinx.coroutines.MainScope().launch {
            callRepository.saveCallRecord(record)
        }
    }

    private fun cleanup() {
        timerJob?.cancel()
        signalListener?.let { listener ->
            rtdb.getReference("calls").child(callId).child("status").removeEventListener(listener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Call Activity
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RECEIVER_ID = "receiver_id"
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_CALLER_ID = "caller_id"
        const val EXTRA_IS_VIDEO = "is_video"
        const val EXTRA_IS_INCOMING = "is_incoming"

        fun createOutgoingIntent(
            context: Context,
            receiverId: String,
            isVideo: Boolean
        ): Intent = Intent(context, CallActivity::class.java).apply {
            putExtra(EXTRA_RECEIVER_ID, receiverId)
            putExtra(EXTRA_IS_VIDEO, isVideo)
            putExtra(EXTRA_IS_INCOMING, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        fun createIncomingIntent(
            context: Context,
            callId: String,
            callerId: String,
            isVideo: Boolean
        ): Intent = Intent(context, CallActivity::class.java).apply {
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_IS_VIDEO, isVideo)
            putExtra(EXTRA_IS_INCOMING, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
        val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

        setContent {
            ZexoTheme(darkTheme = true) {
                val viewModel: CallViewModel = hiltViewModel()
                val uiState by viewModel.uiState

                LaunchedEffect(Unit) {
                    if (isIncoming) {
                        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
                        val callerId = intent.getStringExtra(EXTRA_CALLER_ID) ?: ""
                        viewModel.initIncomingCall(callId, callerId, isVideo)
                    } else {
                        val receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID) ?: ""
                        viewModel.initOutgoingCall(receiverId, isVideo)
                    }

                    // Start foreground service
                    val serviceIntent = Intent(this@CallActivity, CallService::class.java)
                    startForegroundService(serviceIntent)
                }

                // Auto-finish when call ends
                val currentStatus = uiState.status
                LaunchedEffect(currentStatus) {
                    if (currentStatus == CallStatus.ENDED ||
                        currentStatus == CallStatus.DECLINED ||
                        currentStatus == CallStatus.MISSED
                    ) {
                        delay(1500L)
                        stopService(Intent(this@CallActivity, CallService::class.java))
                        finish()
                    }
                }

                CallScreenContent(
                    uiState = uiState,
                    onAnswer = { viewModel.answerCall() },
                    onDecline = { viewModel.declineCall() },
                    onEndCall = { viewModel.endCall() },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleVideo = { viewModel.toggleVideo() },
                    onBack = { viewModel.endCall(); finish() }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun installSplashScreen() {
        // Compatibility splash screen
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Call Screen Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CallScreenContent(
    uiState: CallUiState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ZexoBackground,
                        ZexoSurface,
                        ZexoBackground
                    )
                )
            )
    ) {
        if (uiState.isVideoCall && uiState.status == CallStatus.CONNECTED && uiState.isVideoEnabled) {
            VideoCallLayout(
                uiState = uiState,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onToggleVideo = onToggleVideo,
                onEndCall = onEndCall
            )
        } else {
            AudioCallLayout(
                uiState = uiState,
                onAnswer = onAnswer,
                onDecline = onDecline,
                onEndCall = onEndCall,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onToggleVideo = if (uiState.isVideoCall) onToggleVideo else null
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Audio Call Layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AudioCallLayout(
    uiState: CallUiState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: (() -> Unit)?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background pulse ring for ringing state
        if (uiState.status == CallStatus.RINGING) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .alpha(pulseAlpha * 0.15f)
                    .background(ZexoPrimary, CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // User Avatar
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ZexoPrimary.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.otherUser?.avatar?.isNotEmpty() == true) {
                    AsyncImage(
                        model = uiState.otherUser.avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(ZexoPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.otherUser?.displayName?.take(1)?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Online indicator
                if (uiState.status == CallStatus.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(20.dp)
                            .background(ZexoGreen, CircleShape)
                            .padding(3.dp)
                            .background(ZexoBackground, CircleShape)
                            .padding(2.dp)
                            .background(ZexoGreen, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Name
            Text(
                text = uiState.otherUser?.displayName ?: "Unknown",
                color = ZexoTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Zixo Number
            if (uiState.otherUser?.zixoNumber?.isNotEmpty() == true) {
                Text(
                    text = uiState.otherUser.zixoNumber,
                    color = ZexoPrimaryLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Call Status
            CallStatusLabel(status = uiState.status, durationSeconds = uiState.durationSeconds)

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            if (uiState.status == CallStatus.RINGING && !uiState.isCaller) {
                // Incoming call controls
                IncomingCallControls(onAnswer = onAnswer, onDecline = onDecline)
            } else if (uiState.status == CallStatus.RINGING && uiState.isCaller) {
                // Outgoing ringing - just end call
                OutgoingRingingControls(onEndCall = onEndCall)
            } else if (uiState.status == CallStatus.CONNECTED) {
                // Connected call controls
                ConnectedCallControls(
                    isMuted = uiState.isMuted,
                    isSpeakerOn = uiState.isSpeakerOn,
                    isVideoEnabled = uiState.isVideoEnabled,
                    isVideoCall = uiState.isVideoCall,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onToggleVideo = onToggleVideo,
                    onEndCall = onEndCall
                )
            } else {
                // Ended/Declined/Missed
                EndedCallControls(status = uiState.status)
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video Call Layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VideoCallLayout(
    uiState: CallUiState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote Video (Full Screen)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                org.webrtc.SurfaceViewRenderer(ctx).apply {
                    setZOrderMediaOverlay(false)
                }
            },
            update = { view ->
                // In production, attach to WebRTC peer connection remote video track
            }
        )

        // Dark overlay gradient at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // User info at top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.otherUser?.avatar?.isNotEmpty() == true) {
                AsyncImage(
                    model = uiState.otherUser.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ZexoPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.otherUser?.displayName?.take(1)?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = uiState.otherUser?.displayName ?: "Unknown",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                CallStatusLabel(
                    status = uiState.status,
                    durationSeconds = uiState.durationSeconds,
                    textColor = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        // Local Video (PiP)
        AndroidView(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 16.dp)
                .size(width = 120.dp, height = 170.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ZexoSurface),
            factory = { ctx ->
                org.webrtc.SurfaceViewRenderer(ctx).apply {
                    setZOrderMediaOverlay(true)
                }
            },
            update = { view ->
                // In production, attach to WebRTC peer connection local video track
            }
        )

        // Controls at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 48.dp)
        ) {
            VideoCallControls(
                isMuted = uiState.isMuted,
                isSpeakerOn = uiState.isSpeakerOn,
                isVideoEnabled = uiState.isVideoEnabled,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onToggleVideo = onToggleVideo,
                onEndCall = onEndCall
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CallStatusLabel(
    status: CallStatus,
    durationSeconds: Long,
    textColor: Color = ZexoTextSecondary,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val statusText = when (status) {
        CallStatus.RINGING -> "Ringing..."
        CallStatus.CONNECTING -> "Connecting..."
        CallStatus.CONNECTED -> formatDuration(durationSeconds)
        CallStatus.ENDED -> "Call ended"
        CallStatus.DECLINED -> "Declined"
        CallStatus.MISSED -> "Missed call"
    }

    val animatedColor = when (status) {
        CallStatus.RINGING -> ZexoOrange
        CallStatus.CONNECTING -> ZexoBlue
        CallStatus.CONNECTED -> ZexoGreen
        CallStatus.ENDED -> ZexoTextSecondary
        CallStatus.DECLINED -> ZexoRed
        CallStatus.MISSED -> ZexoRed
    }

    Text(
        text = statusText,
        color = if (status == CallStatus.CONNECTED) animatedColor else textColor,
        fontSize = fontSize,
        fontWeight = if (status == CallStatus.CONNECTED) FontWeight.Medium else FontWeight.Normal
    )
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

// ─────────────────────────────────────────────────────────────────────────────
// Incoming Call Controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IncomingCallControls(onAnswer: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decline Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledIconButton(
                onClick = onDecline,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ZexoRed
                )
            ) {
                Icon(
                    Icons.Filled.CallEnd,
                    contentDescription = "Decline",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Decline", color = ZexoTextSecondary, fontSize = 12.sp)
        }

        // Answer Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledIconButton(
                onClick = onAnswer,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ZexoGreen
                )
            ) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Answer",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Answer", color = ZexoTextSecondary, fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Outgoing Ringing Controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OutgoingRingingControls(onEndCall: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onEndCall,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ZexoRed
            )
        ) {
            Icon(
                Icons.Filled.CallEnd,
                contentDescription = "Cancel",
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Cancel", color = ZexoTextSecondary, fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Connected Call Controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConnectedCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    isVideoCall: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: (() -> Unit)?,
    onEndCall: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute
            ControlButton(
                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = isMuted,
                onClick = onToggleMute
            )

            // Speaker
            ControlButton(
                icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                label = "Speaker",
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker
            )

            // Video Toggle (video calls only)
            if (isVideoCall && onToggleVideo != null) {
                ControlButton(
                    icon = if (isVideoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    label = "Video",
                    isActive = !isVideoEnabled,
                    onClick = onToggleVideo
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // End Call
        FilledIconButton(
            onClick = onEndCall,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ZexoRed
            )
        ) {
            Icon(
                Icons.Filled.CallEnd,
                contentDescription = "End Call",
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video Call Controls (bottom bar style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VideoCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            isActive = isMuted,
            onClick = onToggleMute,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.7f)
        )

        ControlButton(
            icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            label = "Speaker",
            isActive = isSpeakerOn,
            onClick = onToggleSpeaker,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.7f)
        )

        ControlButton(
            icon = if (isVideoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            label = "Video",
            isActive = !isVideoEnabled,
            onClick = onToggleVideo,
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.7f)
        )

        // End Call
        FilledIconButton(
            onClick = onEndCall,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ZexoRed
            )
        ) {
            Icon(
                Icons.Filled.CallEnd,
                contentDescription = "End Call",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ended Call Controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EndedCallControls(status: CallStatus) {
    val message = when (status) {
        CallStatus.ENDED -> "Call ended"
        CallStatus.DECLINED -> "Call declined"
        CallStatus.MISSED -> "Call missed"
        else -> "Call ended"
    }

    val iconColor = when (status) {
        CallStatus.DECLINED, CallStatus.MISSED -> ZexoRed
        else -> ZexoTextSecondary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.CallEnd,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = ZexoTextSecondary,
            fontSize = 16.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable Control Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    activeColor: Color = ZexoPrimary,
    inactiveColor: Color = ZexoTextSecondary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) activeColor else ZexoSurfaceLight
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isActive) Color.White else inactiveColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else inactiveColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
