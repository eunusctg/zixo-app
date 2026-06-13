package com.zixo.app.ui.screens.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zixo.app.data.model.StatusUpdate
import com.zixo.app.ui.theme.*

@Composable
fun StatusTabScreen(onCreateStatus: () -> Unit, viewModel: StatusViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(ZixoBg)) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ZixoPrimary) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Surface(color = ZixoSurface.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth().clickable { if (uiState.myStatuses.isNotEmpty()) viewModel.openStatusViewer(uiState.myStatuses.first()) else onCreateStatus() }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                AsyncImage(model = currentUser?.avatar?.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape).background(ZixoSurfaceLight))
                                if (uiState.myStatuses.isEmpty()) { Surface(color = ZixoPrimary, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd)) { Icon(Icons.Default.Add, contentDescription = "Add Status", tint = ZixoBg, modifier = Modifier.size(18.dp).padding(2.dp)) } }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column { Text("My Status", color = ZixoText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text(if (uiState.myStatuses.isEmpty()) "Tap to add status update" else "${uiState.myStatuses.size} update(s)", color = ZixoTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
                if (uiState.contactStatuses.isNotEmpty()) {
                    item { Text("Recent updates", color = ZixoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
                    val groupedByCreator = uiState.contactStatuses.groupBy { it.creatorZixoNumber }
                    items(groupedByCreator.entries.toList()) { entry ->
                        val statuses = entry.value
                        val first = statuses.first()
                        val hasUnviewed = statuses.any { !it.viewedBy.contains(currentUser?.zixoNumber ?: "") }
                        StatusContactItem(status = first, count = statuses.size, hasUnviewed = hasUnviewed, onClick = { viewModel.openStatusViewer(first) })
                    }
                } else {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No status updates yet", color = ZixoTextSecondary, fontSize = 16.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Status updates from contacts will appear here", color = ZixoTextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) } } }
                }
            }
        }
    }

    if (uiState.isViewerOpen && uiState.selectedStatus != null) {
        StatusViewer(status = uiState.selectedStatus!!, onClose = viewModel::closeStatusViewer, onReact = viewModel::reactToStatus, currentZixoNumber = currentUser?.zixoNumber ?: "")
    }
}

@Composable
private fun StatusContactItem(status: StatusUpdate, count: Int, hasUnviewed: Boolean, onClick: () -> Unit) {
    Surface(color = ZixoSurface.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box { val borderColor = if (hasUnviewed) ZixoPrimary else ZixoHighlight; Surface(shape = CircleShape, border = BorderStroke(2.5.dp, borderColor), color = Color.Transparent) { AsyncImage(model = status.creatorAvatarUrl.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(52.dp).padding(3.dp).clip(CircleShape).background(ZixoSurfaceLight)) } }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) { Text(status.creatorDisplayName, color = ZixoText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (count > 1) "$count updates" else "1 update", color = ZixoTextSecondary, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun StatusViewer(status: StatusUpdate, onClose: () -> Unit, onReact: (String) -> Unit, currentZixoNumber: String) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 5000, easing = LinearEasing), label = "statusProgress")
    LaunchedEffect(Unit) { progress = 1f }

    Box(modifier = Modifier.fillMaxSize().background(parseHexColor(status.backgroundColorHex))) {
        Column(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = ZixoPrimary, trackColor = ZixoHighlight)
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = ZixoText) }
                AsyncImage(model = status.creatorAvatarUrl.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).background(ZixoSurfaceLight))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) { Text(status.creatorDisplayName, color = ZixoText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(formatStatusTime(status.timestamp), color = ZixoTextSecondary, fontSize = 11.sp) }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (status.mediaUrl != null) { AsyncImage(model = status.mediaUrl, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                if (status.textContent != null) { Text(status.textContent, color = ZixoText, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(32.dp)) }
                if (status.overlaySymbols.isNotEmpty()) { LazyRow(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(status.overlaySymbols) { symbol -> Text(symbol, fontSize = 36.sp, modifier = Modifier.padding(horizontal = 4.dp)) } } }
            }
            Surface(color = ZixoSurface.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDD25", "\uD83D\uDE4F").forEach { emoji -> TextButton(onClick = { onReact(emoji) }) { Text(emoji, fontSize = 24.sp) } }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color = try { Color(0xFF000000 or hex.removePrefix("#").toLong(16)) } catch (e: Exception) { ZixoSurface }
private fun formatStatusTime(timestamp: Long): String { if (timestamp <= 0) return ""; val diff = System.currentTimeMillis() - timestamp; val m = diff / 60000; return when { m < 1 -> "Just now"; m < 60 -> "${m}m ago"; m < 1440 -> "${m / 60}h ago"; else -> "${m / 1440}d ago" } }
