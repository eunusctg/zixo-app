package com.zexo.app.ui.screens.status

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.zexo.app.data.model.Status
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.StatusRepository
import com.zexo.app.data.repository.UserRepository
import com.zexo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Status View ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class StatusViewViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : androidx.lifecycle.ViewModel() {

    private val _statuses = mutableStateOf<List<Status>>(emptyList())
    val statuses: State<List<Status>> = _statuses

    private val _currentIndex = mutableStateOf(0)
    val currentIndex: State<Int> = _currentIndex

    private val _progress = mutableStateOf(0f)
    val progress: State<Float> = _progress

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private var timerJob: kotlinx.coroutines.Job? = null
    private var progressJob: kotlinx.coroutines.Job? = null

    companion object {
        const val STATUS_DURATION_MS = 6000L  // 6 seconds per status
        const val PROGRESS_STEP_MS = 30L
    }

    fun loadStatuses(initialStatusId: String) {
        val uid = authRepository.currentUid ?: return
        kotlinx.coroutines.MainScope().launch {
            // Get contacts' UIDs (for demo, get all users)
            val usersResult = userRepository.getAllUsers(uid)
            val uids = usersResult.getOrDefault(emptyList()).map { it.uid }.toMutableList()
            uids.add(uid) // Include own statuses

            statusRepository.observeStatuses(uids).collect { statusList ->
                if (statusList.isNotEmpty()) {
                    _statuses.value = statusList
                    // Find the initial status
                    val initialIndex = statusList.indexOfFirst { it.id == initialStatusId }
                    _currentIndex.value = if (initialIndex >= 0) initialIndex else 0
                    _isLoading.value = false
                    startProgress()
                } else {
                    _isLoading.value = false
                }
            }
        }
    }

    fun loadStatusesByUser(userId: String) {
        val uid = authRepository.currentUid ?: return
        kotlinx.coroutines.MainScope().launch {
            val uids = listOf(userId, uid)
            statusRepository.observeStatuses(uids).collect { statusList ->
                val filtered = statusList.filter { it.userId == userId }
                if (filtered.isNotEmpty()) {
                    _statuses.value = filtered
                    _currentIndex.value = 0
                    _isLoading.value = false
                    startProgress()
                } else {
                    _isLoading.value = false
                }
            }
        }
    }

    fun goToNextStatus() {
        val current = _currentIndex.value
        if (current < _statuses.value.size - 1) {
            markCurrentAsSeen()
            _currentIndex.value = current + 1
            resetAndStartProgress()
        }
    }

    fun goToPrevStatus() {
        val current = _currentIndex.value
        if (current > 0) {
            _currentIndex.value = current - 1
            resetAndStartProgress()
        }
    }

    private fun startProgress() {
        progressJob?.cancel()
        _progress.value = 0f

        progressJob = kotlinx.coroutines.MainScope().launch {
            while (_progress.value < 1f) {
                delay(PROGRESS_STEP_MS)
                _progress.value += PROGRESS_STEP_MS.toFloat() / STATUS_DURATION_MS
                if (_progress.value >= 1f) {
                    _progress.value = 1f
                    // Auto-advance
                    val current = _currentIndex.value
                    if (current < _statuses.value.size - 1) {
                        markCurrentAsSeen()
                        _currentIndex.value = current + 1
                        _progress.value = 0f
                    }
                }
            }
        }
    }

    private fun resetAndStartProgress() {
        markCurrentAsSeen()
        startProgress()
    }

    private fun markCurrentAsSeen() {
        val status = _statuses.value.getOrNull(_currentIndex.value) ?: return
        val uid = authRepository.currentUid ?: return
        if (status.userId != uid && uid !in status.seenBy) {
            kotlinx.coroutines.MainScope().launch {
                statusRepository.markStatusSeen(status.id, uid)
            }
        }
    }

    fun pauseProgress() {
        progressJob?.cancel()
    }

    fun resumeProgress() {
        startProgress()
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        timerJob?.cancel()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status View Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusViewScreen(
    navController: NavController,
    statusId: String,
    viewModel: StatusViewViewModel = hiltViewModel()
) {
    val statuses by viewModel.statuses
    val currentIndex by viewModel.currentIndex
    val progress by viewModel.progress
    val isLoading by viewModel.isLoading

    LaunchedEffect(statusId) {
        viewModel.loadStatuses(statusId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZexoBackground)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = ZexoPrimary
            )
        } else if (statuses.isEmpty()) {
            // No statuses
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = ZexoTextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No status available",
                    color = ZexoTextSecondary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Go Back", color = ZexoPrimary)
                }
            }
        } else {
            val currentStatus = statuses.getOrNull(currentIndex)

            if (currentStatus != null) {
                StatusContent(
                    status = currentStatus,
                    progress = progress,
                    totalStatuses = statuses.size,
                    currentIndex = currentIndex,
                    onNext = { viewModel.goToNextStatus() },
                    onPrev = { viewModel.goToPrevStatus() },
                    onClose = { navController.popBackStack() },
                    onPause = { viewModel.pauseProgress() },
                    onResume = { viewModel.resumeProgress() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusContent(
    status: Status,
    progress: Float,
    totalStatuses: Int,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) {
                            onPrev()
                        } else {
                            onNext()
                        }
                    },
                    onLongPress = { onPause() },
                    onPress = {
                        awaitRelease()
                        onResume()
                    }
                )
            }
    ) {
        // Status Background
        if (status.type == "image" && status.mediaUrl.isNotEmpty()) {
            // Image status
            AsyncImage(
                model = status.mediaUrl,
                contentDescription = "Status image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            )
        } else {
            // Text status with colored background
            val bgColor = parseColor(status.backgroundColor, ZexoPrimary)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                bgColor,
                                bgColor.copy(alpha = 0.8f),
                                bgColor.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // Progress Bars at Top
        StatusProgressBars(
            totalStatuses = totalStatuses,
            currentIndex = currentIndex,
            currentProgress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        )

        // Header: Close + User Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (status.userAvatar.isNotEmpty()) {
                    AsyncImage(
                        model = status.userAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = status.userName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // User name and time
            Column {
                Text(
                    text = status.userName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimeAgo(status.createdAt),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        // Status Text Content (centered)
        if (status.type == "text" && status.content.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                val textColor = parseColor(status.textColor, Color.White)
                Text(
                    text = status.content,
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp,
                    modifier = Modifier.padding(bottom = 60.dp)
                )
            }
        }

        // Tap hint indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left tap area hint
            if (currentIndex > 0) {
                Text(
                    text = "< Prev",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }

            // Status counter
            Text(
                text = "${currentIndex + 1} / $totalStatuses",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )

            // Right tap area hint
            if (currentIndex < totalStatuses - 1) {
                Text(
                    text = "Next >",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Progress Bars
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusProgressBars(
    totalStatuses: Int,
    currentIndex: Int,
    currentProgress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until totalStatuses) {
            val progressValue = when {
                i < currentIndex -> 1f
                i == currentIndex -> currentProgress.coerceIn(0f, 1f)
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fractionalWidth(progressValue)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun Modifier.fractionalWidth(fraction: Float): Modifier {
    return this.then(
        Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────

private fun parseColor(hex: String, default: Color): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = when (cleanHex.length) {
            6 -> "FF$cleanHex".toLong(16)
            8 -> cleanHex.toLong(16)
            else -> return default
        }
        Color(colorLong)
    } catch (_: Exception) {
        default
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
    }
}
