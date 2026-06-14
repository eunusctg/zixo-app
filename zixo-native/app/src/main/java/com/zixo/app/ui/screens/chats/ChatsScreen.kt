package com.zixo.app.ui.screens.chats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.TopBarAction
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ──────────────────────────────────────────────
// Chats UI State
// ──────────────────────────────────────────────

data class ChatsUiState(
    val chats: List<ChatItemUiState> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false
)

data class ChatItemUiState(
    val threadId: String,
    val participantDisplayName: String,
    val participantUsername: String,
    val participantPhotoUrl: String?,
    val participantIsOnline: Boolean,
    val lastMessage: String?,
    val lastMessageTimestamp: Instant?,
    val unreadCount: Int = 0
)

// ──────────────────────────────────────────────
// Chats Screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onChatClick: (threadId: String) -> Unit = {},
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatsScreenContent(
        uiState = uiState,
        onChatClick = onChatClick,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleSearch = viewModel::onToggleSearch,
        onRefresh = viewModel::onRefresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsScreenContent(
    uiState: ChatsUiState,
    onChatClick: (threadId: String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ─────────────────────────────────
            ZixoTopBar(
                title = "Chats",
                actionIcons = listOf(
                    TopBarAction(
                        icon = Icons.Filled.Search,
                        contentDescription = "Search chats",
                        onClick = onToggleSearch
                    )
                )
            )

            // ── Animated Search Bar ─────────────────────
            AnimatedVisibility(
                visible = uiState.isSearchExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search chats\u2026",
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMint,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        cursorColor = NeonMint,
                        focusedContainerColor = DarkPetrolCharcoal,
                        unfocusedContainerColor = DarkPetrolCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* handled by query change */ }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ── Pull-to-Refresh + Chat List ─────────────
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.chats.isEmpty() -> {
                        // Full-screen loading
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeonMint,
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    uiState.chats.isEmpty() -> {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No chats yet",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Start a conversation to see it here",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    else -> {
                        // Chat list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = 80.dp // Space for bottom nav
                            )
                        ) {
                            items(
                                items = uiState.chats,
                                key = { it.threadId }
                            ) { chatItem ->
                                ChatThreadItem(
                                    chatItem = chatItem,
                                    onClick = { onChatClick(chatItem.threadId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Chat Thread Item
// ──────────────────────────────────────────────

@Composable
private fun ChatThreadItem(
    chatItem: ChatItemUiState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(DarkPetrolCharcoal.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Avatar ─────────────────────────────────
        AvatarComponent(
            imageUrl = chatItem.participantPhotoUrl,
            name = chatItem.participantDisplayName,
            isOnline = chatItem.participantIsOnline,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // ── Name + Last Message ────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chatItem.participantDisplayName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true)
                )

                // ── Timestamp ───────────────────────
                chatItem.lastMessageTimestamp?.let { timestamp ->
                    Text(
                        text = formatTimestamp(timestamp),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // ── Username below display name ────────
            Text(
                text = "@${chatItem.participantUsername}",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chatItem.lastMessage ?: "",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true)
                )

                // ── Unread Count Badge ──────────────
                if (chatItem.unreadCount > 0) {
                    UnreadBadge(count = chatItem.unreadCount)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Unread Count Badge
// ──────────────────────────────────────────────

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .background(
                color = NeonMint,
                shape = CircleShape
            )
            .then(
                if (count > 99) {
                    Modifier.size(28.dp)
                } else if (count > 9) {
                    Modifier.size(24.dp)
                } else {
                    Modifier.size(20.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color(0xFF0B1519),
            fontSize = if (count > 99) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ──────────────────────────────────────────────
// Timestamp Formatting
// ──────────────────────────────────────────────

private fun formatTimestamp(instant: Instant): String {
    val now = Instant.now()
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val nowDateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault())

    val duration = Duration.between(instant, now)
    val minutes = duration.toMinutes()
    val hours = duration.toHours()

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 && dateTime.dayOfYear == nowDateTime.dayOfYear -> {
            DateTimeFormatter.ofPattern("h:mm a").format(dateTime)
        }
        hours < 48 -> "yesterday"
        dateTime.year == nowDateTime.year -> {
            DateTimeFormatter.ofPattern("MMM d").format(dateTime)
        }
        else -> {
            DateTimeFormatter.ofPattern("MMM d, yyyy").format(dateTime)
        }
    }
}
