package com.zexo.app.ui.screens.chat

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.zexo.app.data.model.Message
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.screens.calls.CallActivity
import com.zexo.app.ui.theme.*
import com.zexo.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatId: String,
    otherUserId: String = "",
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Initialize chat once
    LaunchedEffect(chatId, otherUserId) {
        viewModel.initChat(chatId, otherUserId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Clear error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                otherUser = uiState.otherUser,
                isOnline = uiState.isOtherUserOnline,
                isTyping = uiState.isOtherUserTyping,
                lastSeen = uiState.otherUserLastSeen,
                onBackClick = { navController.popBackStack() },
                onAvatarClick = {
                    uiState.otherUser?.uid?.let { uid ->
                        navController.navigate(Screen.Profile.createRoute(uid))
                    }
                },
                onAudioCallClick = {
                    val otherUser = uiState.otherUser
                    if (otherUser != null) {
                        val intent = CallActivity.createOutgoingIntent(
                            context = context,
                            receiverId = uiState.otherUserId.ifBlank { otherUser.uid },
                            isVideo = false
                        )
                        context.startActivity(intent)
                    }
                },
                onVideoCallClick = {
                    val otherUser = uiState.otherUser
                    if (otherUser != null) {
                        val intent = CallActivity.createOutgoingIntent(
                            context = context,
                            receiverId = uiState.otherUserId.ifBlank { otherUser.uid },
                            isVideo = true
                        )
                        context.startActivity(intent)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ZexoBackground)
                .padding(paddingValues)
        ) {
            // Messages list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ZexoPrimary,
                        strokeWidth = 2.dp
                    )
                } else if (uiState.messages.isEmpty()) {
                    EmptyChatState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isFromMe = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
                            )
                        }
                    }
                }
            }

            // Message input bar
            MessageInputBar(
                text = messageText,
                onTextChange = { newText ->
                    messageText = newText
                    viewModel.setTyping(chatId, newText.isNotBlank())
                },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(chatId, messageText)
                        messageText = ""
                        viewModel.setTyping(chatId, false)
                    }
                },
                onAttachmentClick = { /* TODO: Handle attachments */ },
                onVoiceClick = { /* TODO: Handle voice recording */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    otherUser: com.zexo.app.data.model.User?,
    isOnline: Boolean,
    isTyping: Boolean,
    lastSeen: Long,
    onBackClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column(modifier = Modifier.clickable { onAvatarClick() }) {
                Text(
                    text = otherUser?.displayName ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ZexoTextPrimary
                )
                AnimatedVisibility(visible = isTyping || isOnline) {
                    Text(
                        text = when {
                            isTyping -> "typing..."
                            isOnline -> "online"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTyping) ZexoTyping else ZexoOnline,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!isOnline && !isTyping && lastSeen > 0) {
                    Text(
                        text = "last seen ${formatLastSeen(lastSeen)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZexoTextSecondary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ZexoTextPrimary
                )
            }
        },
        actions = {
            // Audio call button
            IconButton(onClick = onAudioCallClick) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = ZexoTextPrimary
                )
            }
            // Video call button
            IconButton(onClick = onVideoCallClick) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = ZexoTextPrimary
                )
            }
            // Avatar in top bar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ZexoSurfaceLight)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (otherUser?.avatar?.isNotBlank() == true) {
                    AsyncImage(
                        model = otherUser.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = otherUser?.displayName?.take(1)?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                // Online indicator dot
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ZexoOnline)
                            .padding(1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ZexoSurface
        )
    )
}

@Composable
private fun MessageBubble(
    message: Message,
    isFromMe: Boolean
) {
    val bubbleColor = if (isFromMe) ZexoPrimary else ZexoSurfaceLight
    val textColor = ZexoTextPrimary
    val timeColor = if (isFromMe) Color.White.copy(alpha = 0.7f) else ZexoTextSecondary

    val alignment = if (isFromMe) Alignment.End else Alignment.Start
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        color = timeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: String) {
    val color = when (status) {
        "sent" -> Color.White.copy(alpha = 0.5f)
        "delivered" -> Color.White.copy(alpha = 0.7f)
        "read" -> ZexoOnline
        else -> Color.White.copy(alpha = 0.5f)
    }

    Text(
        text = when (status) {
            "sent" -> "✓"
            "delivered" -> "✓✓"
            "read" -> "✓✓"
            else -> ""
        },
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Surface(
        color = ZexoSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment button
            IconButton(
                onClick = onAttachmentClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = ZexoTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text input field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 120.dp),
                placeholder = {
                    Text(
                        "Type a message...",
                        color = ZexoTextSecondary,
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = ZexoSurfaceLight,
                    unfocusedContainerColor = ZexoSurfaceLight,
                    focusedTextColor = ZexoTextPrimary,
                    unfocusedTextColor = ZexoTextPrimary,
                    cursorColor = ZexoPrimary
                ),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Send or Voice button
            AnimatedVisibility(
                visible = text.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ZexoPrimary)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = text.isBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = ZexoTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "👋",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Start the conversation",
                color = ZexoTextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Say hello and break the ice!",
                color = ZexoTextSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatMessageTime(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return ""
    return try {
        DateUtils.formatSameDayTime(
            timestamp,
            System.currentTimeMillis(),
            DateUtils.FORMAT_SHOW_TIME,
            DateUtils.FORMAT_ABBREV_TIME
        ).toString()
    } catch (e: Exception) {
        ""
    }
}

private fun formatLastSeen(lastSeen: Long): String {
    if (lastSeen == 0L) return "recently"
    val diff = System.currentTimeMillis() - lastSeen
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> DateUtils.getRelativeTimeSpanString(
            lastSeen,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS
        ).toString()
    }
}
