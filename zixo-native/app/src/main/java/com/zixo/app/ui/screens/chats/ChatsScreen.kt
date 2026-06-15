package com.zixo.app.ui.screens.chats

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.domain.model.ChatThreadModel
import com.zixo.app.domain.model.LastMessageInfo
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import com.zixo.app.ui.theme.TextTertiary

/**
 * Chats tab screen — displays the user's conversation thread list.
 *
 * Each thread is rendered as a liquid glass card showing:
 * - Participant avatar + online indicator
 * - Thread name (display name for 1:1, group name for groups)
 * - Last message preview + timestamp
 * - Unread count badge
 *
 * Real-time updates are provided via [ChatsViewModel] which attaches
 * continuous Firestore snapshot listeners to the threads collection.
 *
 * @param onChatClick Callback invoked with the threadId when a thread is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onChatClick: (String) -> Unit = {},
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    ZixoGlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Search Bar ─────────────────────────────
            SearchBar(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    viewModel.filterThreads(query)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Thread List ────────────────────────────
            if (isLoading && threads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonMint)
                }
            } else if (threads.isEmpty()) {
                EmptyChatsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    )
                ) {
                    items(
                        items = threads,
                        key = { it.id }
                    ) { thread ->
                        ThreadItem(
                            thread = thread,
                            onClick = { onChatClick(thread.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("Search chats...", color = TextSecondary.copy(alpha = 0.6f))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = TextSecondary
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DarkPetrolCharcoal.copy(alpha = 0.6f),
            unfocusedContainerColor = DarkPetrolCharcoal.copy(alpha = 0.4f),
            focusedIndicatorColor = NeonMint,
            unfocusedIndicatorColor = TextTertiary.copy(alpha = 0.3f),
            cursorColor = NeonMint,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = modifier
    )
}

@Composable
private fun ThreadItem(
    thread: ChatThreadModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .liquidGlassCard()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        val avatarUrl = thread.participantProfiles.values.firstOrNull()?.avatarUrl ?: ""
        val displayName = getThreadDisplayName(thread)

        AvatarComponent(
            avatarUrl = avatarUrl,
            displayName = displayName,
            size = 48.dp,
            isOnline = thread.participantProfiles.values.any { it.isOnline }
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                thread.lastMessage?.let { lastMsg ->
                    Text(
                        text = formatTimestamp(lastMsg.timestamp),
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                thread.lastMessage?.let { lastMsg ->
                    Text(
                        text = formatLastMessage(lastMsg),
                        color = if (thread.unreadCount > 0) TextSecondary else TextTertiary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } ?: Text(
                    text = "No messages yet",
                    color = TextTertiary,
                    fontSize = 13.sp
                )

                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    UnreadBadge(count = thread.unreadCount)
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(if (count > 9) 28.dp else 22.dp)
            .clip(CircleShape)
            .background(NeonMint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyChatsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No conversations yet",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Find a contact by their Zixo Number to start chatting",
                color = TextTertiary,
                fontSize = 13.sp
            )
        }
    }
}

private fun getThreadDisplayName(thread: ChatThreadModel): String {
    return when {
        !thread.groupName.isNullOrBlank() -> thread.groupName
        else -> thread.participantProfiles.values.firstOrNull()?.displayName ?: "Unknown"
    }
}

private fun formatLastMessage(lastMsg: LastMessageInfo): String {
    val prefix = if (lastMsg.senderDisplayName.isNotBlank()) {
        "${lastMsg.senderDisplayName}: "
    } else ""
    return "$prefix${lastMsg.content}"
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        diff < 604_800_000L -> "${diff / 86_400_000L}d"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

private fun Modifier.background(color: androidx.compose.ui.graphics.Color) =
    this.then(androidx.compose.foundation.background(color))

import androidx.compose.foundation.background
