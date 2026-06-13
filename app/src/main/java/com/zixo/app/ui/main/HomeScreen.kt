package com.zixo.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.CallRecord
import com.zixo.app.domain.model.Chat
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.ui.components.LiquidGlassSurface
import com.zixo.app.ui.theme.*

sealed class HomeTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Chats : HomeTab("Chats", Icons.Filled.Chat, Icons.Outlined.Chat)
    data object Calls : HomeTab("Calls", Icons.Filled.Call, Icons.Outlined.Call)
    data object Status : HomeTab("Status", Icons.Filled.Star, Icons.Outlined.Star)
    data object Settings : HomeTab("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserProfile?,
    chats: List<Chat>,
    callHistory: List<CallRecord>,
    onChatClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onCallClick: (String) -> Unit,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var selectedTab: HomeTab by remember { mutableStateOf(HomeTab.Chats) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ZixoBackground)
        ) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        text = "Zixo",
                        fontWeight = FontWeight.Bold,
                        color = ZixoTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    if (selectedTab == HomeTab.Chats) {
                        IconButton(onClick = onNewChat) {
                            Icon(
                                Icons.Filled.AddComment,
                                contentDescription = "New Chat",
                                tint = ZixoAccent
                            )
                        }
                    }
                }
            )

            // Tab content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    HomeTab.Chats -> ChatsTabContent(
                        chats = chats,
                        currentUserId = currentUser?.uid ?: "",
                        onChatClick = onChatClick
                    )
                    HomeTab.Calls -> CallsTabContent(
                        callHistory = callHistory,
                        currentUserId = currentUser?.uid ?: "",
                        onCallClick = onCallClick
                    )
                    HomeTab.Status -> {
                        // Status content handled by navigation
                        onStatusClick()
                    }
                    HomeTab.Settings -> {
                        // Settings content handled by navigation
                        onSettingsClick()
                    }
                }
            }

            // Bottom navigation — 80dp footer bar with Liquid Glass
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                cornerRadius = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(HomeTab.Chats, HomeTab.Calls, HomeTab.Status, HomeTab.Settings)
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) ZixoAccent else ZixoTextSecondary,
                            animationSpec = tween(300),
                            label = "tab_${tab.title}"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = iconTint
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(ZixoAccent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatsTabContent(
    chats: List<Chat>,
    currentUserId: String,
    onChatClick: (String) -> Unit
) {
    if (chats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = ZixoTextTertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No conversations yet",
                    color = ZixoTextSecondary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start a new chat to begin messaging",
                    color = ZixoTextTertiary,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(chats.size) { index ->
                val chat = chats[index]
                val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: ""
                val otherName = chat.participantNames[otherUserId] ?: "Unknown"
                val otherAvatar = chat.participantAvatars[otherUserId] ?: ""
                val unread = chat.unreadCount[currentUserId] ?: 0

                ChatListItem(
                    name = if (chat.isGroup) chat.groupName else otherName,
                    lastMessage = chat.lastMessage,
                    timestamp = chat.lastMessageTimestamp,
                    unreadCount = unread,
                    avatarUrl = if (chat.isGroup) chat.groupAvatar else otherAvatar,
                    onClick = { onChatClick(chat.id) }
                )
            }
        }
    }
}

@Composable
private fun ChatListItem(
    name: String,
    lastMessage: String,
    timestamp: Long,
    unreadCount: Int,
    avatarUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ZixoSurface),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(avatarUrl),
                    contentDescription = name,
                    modifier = Modifier.size(52.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = ZixoAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = ZixoTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = lastMessage,
                color = ZixoTextSecondary,
                fontSize = 14.sp,
                maxLines = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTimestamp(timestamp),
                color = ZixoTextTertiary,
                fontSize = 12.sp
            )
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ZixoAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CallsTabContent(
    callHistory: List<CallRecord>,
    currentUserId: String,
    onCallClick: (String) -> Unit
) {
    if (callHistory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Call,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = ZixoTextTertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No call history",
                    color = ZixoTextSecondary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your calls will appear here",
                    color = ZixoTextTertiary,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(callHistory.size) { index ->
                val call = callHistory[index]
                val isOutgoing = call.callerId == currentUserId
                val otherName = if (isOutgoing) call.receiverName else call.callerName
                val otherAvatar = if (isOutgoing) call.receiverAvatar else call.callerAvatar

                CallListItem(
                    name = otherName.ifBlank { "Unknown" },
                    avatarUrl = otherAvatar,
                    isOutgoing = isOutgoing,
                    type = call.type,
                    duration = call.duration,
                    timestamp = call.timestamp,
                    status = call.status,
                    onClick = { onCallClick(if (isOutgoing) call.receiverId else call.callerId) }
                )
            }
        }
    }
}

@Composable
private fun CallListItem(
    name: String,
    avatarUrl: String,
    isOutgoing: Boolean,
    type: String,
    duration: Long,
    timestamp: Long,
    status: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ZixoSurface),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(avatarUrl),
                    contentDescription = name,
                    modifier = Modifier.size(48.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = ZixoAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = if (status == "missed") ZixoError else ZixoTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOutgoing) Icons.Filled.NorthEast else Icons.Filled.SouthWest,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (status == "missed") ZixoError else ZixoTextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (type == "video") "Video" else "Audio",
                    color = ZixoTextSecondary,
                    fontSize = 13.sp
                )
                if (duration > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "· ${formatDuration(duration)}",
                        color = ZixoTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Text(
            text = formatTimestamp(timestamp),
            color = ZixoTextTertiary,
            fontSize = 12.sp
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
