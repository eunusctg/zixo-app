package com.zixo.app.ui.screens.calls

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallMissed
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zixo.app.domain.model.CallDirection
import com.zixo.app.domain.model.CallFilter
import com.zixo.app.domain.model.CallLogEntry
import com.zixo.app.domain.model.CallTechnology
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.SegmentedPicker
import com.zixo.app.ui.components.ZixoTopBar
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ──────────────────────────────────────────────
// Color constants
// ──────────────────────────────────────────────
private val BackgroundStart = Color(0xFF0B1519)
private val BackgroundEnd = Color(0xFF111E24)
private val CardSurface = Color(0xFF1A2A32)
private val AccentGreen = Color(0xFF00E676)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF90A4AE)
private val MissedRed = Color(0xFFFF5252)
private val DialPadKeySurface = Color(0xFF1E3239)

// ──────────────────────────────────────────────
// Calls Screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val calls by viewModel.filteredCalls.collectAsState()
    val isRefreshing by remember { derivedStateOf { uiState.isRefreshing } }

    var showDialPad by remember { mutableStateOf(false) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(BackgroundStart, BackgroundEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                ZixoTopBar(title = "Calls")
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialPad = true },
                    containerColor = AccentGreen,
                    contentColor = Color(0xFF003A1F),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Dialpad,
                        contentDescription = "Dial pad",
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── Segmented Filter ─────────────────────
                val filterOptions = remember {
                    listOf("All", "Missed", "Outgoing", "Incoming")
                }
                val filterIndices = remember {
                    listOf(CallFilter.ALL, CallFilter.MISSED, CallFilter.OUTGOING, CallFilter.INCOMING)
                }
                val selectedFilterIndex = filterIndices.indexOf(uiState.selectedFilter).coerceAtLeast(0)

                SegmentedPicker(
                    options = filterOptions,
                    selectedIndex = selectedFilterIndex,
                    onOptionSelected = { index ->
                        viewModel.onFilterSelected(filterIndices[index])
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Call Log List with Pull-to-Refresh ───
                val pullToRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.onRefresh() },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (calls.isEmpty()) {
                        EmptyCallsState(
                            filter = uiState.selectedFilter,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = 80.dp, // Space for FAB
                            ),
                        ) {
                            items(
                                items = calls,
                                key = { it.id },
                            ) { entry ->
                                CallLogItem(
                                    entry = entry,
                                    currentUserId = uiState.currentUserId,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dial Pad Bottom Sheet ─────────────────────
        if (showDialPad) {
            DialPadSheet(
                onDismiss = { showDialPad = false },
                onCall = { _ ->
                    // TODO: Initiate call with the dialed number
                    showDialPad = false
                },
            )
        }
    }
}

// ──────────────────────────────────────────────
// Call Log Item
// ──────────────────────────────────────────────

@Composable
private fun CallLogItem(
    entry: CallLogEntry,
    currentUserId: String?,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = entry.type == CallDirection.OUTGOING
    val displayName = if (isOutgoing) entry.calleeName else entry.callerName
    val displayAvatar = if (isOutgoing) entry.calleeAvatar else entry.callerAvatar

    val (icon, iconTint, typeLabel) = when (entry.type) {
        CallDirection.INCOMING -> Triple(
            Icons.Outlined.CallReceived,
            TextSecondary,
            "Incoming",
        )
        CallDirection.OUTGOING -> Triple(
            Icons.Outlined.CallMade,
            AccentGreen,
            "Outgoing",
        )
        CallDirection.MISSED -> Triple(
            Icons.Outlined.CallMissed,
            MissedRed,
            "Missed",
        )
    }

    val techLabel = when (entry.callType) {
        CallTechnology.SIP -> "SIP"
        CallTechnology.WEBRTC_AUDIO -> "WebRTC Audio"
        CallTechnology.WEBRTC_VIDEO -> "WebRTC Video"
    }

    val durationText = if (entry.duration > 0L) formatDuration(entry.duration) else null
    val relativeTime = formatRelativeTime(entry.timestamp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Avatar ──
            AvatarComponent(
                imageUrl = displayAvatar,
                name = displayName,
                size = 48.dp,
            )

            Spacer(modifier = Modifier.width(14.dp))

            // ── Content ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Name
                Text(
                    text = displayName,
                    color = if (entry.type == CallDirection.MISSED) MissedRed else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )

                // Call type + technology + duration row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = typeLabel,
                        modifier = Modifier.size(14.dp),
                        tint = iconTint,
                    )

                    Text(
                        text = typeLabel,
                        color = iconTint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Text(
                        text = "·",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )

                    Text(
                        text = techLabel,
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )

                    if (durationText != null) {
                        Text(
                            text = "·",
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )

                        Text(
                            text = durationText,
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // ── Timestamp ──
            Text(
                text = relativeTime,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Empty State
// ──────────────────────────────────────────────

@Composable
private fun EmptyCallsState(
    filter: CallFilter,
    modifier: Modifier = Modifier,
) {
    val message = when (filter) {
        CallFilter.ALL -> "No calls yet"
        CallFilter.MISSED -> "No missed calls"
        CallFilter.OUTGOING -> "No outgoing calls"
        CallFilter.INCOMING -> "No incoming calls"
    }

    val subtitle = when (filter) {
        CallFilter.ALL -> "Your call history will appear here"
        CallFilter.MISSED -> "Missed calls will appear here"
        CallFilter.OUTGOING -> "Outgoing calls will appear here"
        CallFilter.INCOMING -> "Incoming calls will appear here"
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = when (filter) {
                    CallFilter.ALL -> Icons.Outlined.Phone
                    CallFilter.MISSED -> Icons.Outlined.CallMissed
                    CallFilter.OUTGOING -> Icons.Outlined.CallMade
                    CallFilter.INCOMING -> Icons.Outlined.CallReceived
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Dial Pad Bottom Sheet
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialPadSheet(
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    var dialedNumber by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Title ──
            Text(
                text = "Dial",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // ── Number Display ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dialedNumber.ifBlank { "Enter number" },
                    color = if (dialedNumber.isBlank()) TextSecondary else TextPrimary,
                    fontSize = if (dialedNumber.length > 12) 22.sp else 28.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Dial Pad Grid ──
            val dialKeys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#"),
            )

            val keySubtexts = mapOf(
                "2" to "ABC", "3" to "DEF",
                "4" to "GHI", "5" to "JKL", "6" to "MNO",
                "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
                "0" to "+",
            )

            dialKeys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { key ->
                        DialPadKey(
                            key = key,
                            subtext = keySubtexts[key],
                            onClick = {
                                if (dialedNumber.length < 20) {
                                    dialedNumber += key
                                }
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Call / Backspace Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Invisible spacer to balance layout
                Box(modifier = Modifier.size(64.dp))

                // Call button
                IconButton(
                    onClick = { if (dialedNumber.isNotBlank()) onCall(dialedNumber) },
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentGreen, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = "Call",
                        tint = Color(0xFF003A1F),
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Backspace
                IconButton(
                    onClick = {
                        if (dialedNumber.isNotEmpty()) {
                            dialedNumber = dialedNumber.dropLast(1)
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    enabled = dialedNumber.isNotEmpty(),
                ) {
                    Text(
                        text = "⌫",
                        color = if (dialedNumber.isNotEmpty()) TextSecondary else TextSecondary.copy(alpha = 0.3f),
                        fontSize = 24.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialPadKey(
    key: String,
    subtext: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(72.dp)
            .background(DialPadKeySurface, CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = key,
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        if (subtext != null) {
            Text(
                text = subtext,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Utility Functions
// ──────────────────────────────────────────────

private fun formatDuration(seconds: Long): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "%02d:%02d".format(mins, secs)
}

private fun formatRelativeTime(timestamp: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(timestamp, now)

    return when {
        duration.isNegative -> "Just now"
        duration.toMinutes() < 1L -> "Just now"
        duration.toMinutes() < 60L -> "${duration.toMinutes()} min ago"
        duration.toHours() < 24L -> {
            val hours = duration.toHours()
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        duration.toDays() == 1L -> "Yesterday"
        duration.toDays() < 7L -> "${duration.toDays()} days ago"
        duration.toDays() < 30L -> {
            val weeks = duration.toDays() / 7
            if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        }
        else -> {
            val dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            dateTime.format(formatter)
        }
    }
}
