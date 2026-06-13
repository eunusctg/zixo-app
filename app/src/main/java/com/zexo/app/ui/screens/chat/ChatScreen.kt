package com.zexo.app.ui.screens.chat

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zexo.app.data.model.Message
import com.zexo.app.data.model.ZixoUser
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    otherUserId: String,
    onBack: () -> Unit,
    onAudioCall: (String) -> Unit = {},
    onVideoCall: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(chatId, otherUserId) {
        viewModel.initChat(chatId, otherUserId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        DeleteConfirmDialog(
            isOwnMessage = uiState.selectedMessage?.senderId == currentUser?.uid,
            onDeleteForMe = viewModel::deleteForMe,
            onDeleteForEveryone = viewModel::deleteForEveryone,
            onDismiss = viewModel::dismissDeleteConfirm
        )
    }

    // Forward dialog
    if (uiState.showForwardDialog) {
        ForwardSelectionDialog(
            currentUserId = currentUser?.uid ?: "",
            onForward = viewModel::forwardToChat,
            onDismiss = viewModel::dismissForwardDialog
        )
    }

    // Message action menu
    if (uiState.showActionMenu && uiState.selectedMessage != null) {
        MessageActionSheet(
            message = uiState.selectedMessage!!,
            isOwnMessage = uiState.selectedMessage!!.senderId == currentUser?.uid,
            onReact = viewModel::reactToMessage,
            onReply = viewModel::replyToMessage,
            onForward = viewModel::showForwardDialog,
            onDelete = viewModel::showDeleteConfirm,
            onDismiss = viewModel::dismissActionMenu
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(ZixoBg)) {
        // Top bar
        Surface(
            color = ZixoSurface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ZixoText)
                }
                AsyncImage(
                    model = uiState.otherUser?.avatar?.ifEmpty { null },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(ZixoSurfaceLight)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        uiState.otherUser?.displayName ?: "User",
                        color = ZixoText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (uiState.otherUser?.online == true) "Online" else "Offline",
                        color = if (uiState.otherUser?.online == true) ZixoOnline else ZixoTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { onAudioCall(otherUserId) }) {
                    Icon(Icons.Default.Call, null, tint = ZixoPrimary)
                }
                IconButton(onClick = { onVideoCall(otherUserId) }) {
                    Icon(Icons.Default.Videocam, null, tint = ZixoPrimary)
                }
            }
        }

        // Reply preview bar
        uiState.replyingTo?.let { replyMsg ->
            Surface(color = ZixoSurface.copy(alpha = 0.7f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text(
                            replyMsg.senderZixoNumber,
                            color = ZixoPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            replyMsg.text.take(60),
                            color = ZixoTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = viewModel::cancelReply) {
                        Icon(Icons.Default.Close, null, tint = ZixoTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Messages list
        if (uiState.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZixoPrimary)
            }
        } else {
            val currentUserId = currentUser?.uid ?: ""
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    if (!message.isDeletedForUser(currentUserId)) {
                        MessageBubble(
                            message = message,
                            isOwn = message.senderId == currentUserId,
                            onLongPress = { viewModel.onMessageLongPress(message) }
                        )
                    }
                }
            }
        }

        // Input bar
        Surface(color = ZixoSurface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.messageText,
                    onValueChange = viewModel::onMessageTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message\u2026", color = ZixoTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZixoText,
                        unfocusedTextColor = ZixoText,
                        focusedBorderColor = ZixoPrimary,
                        unfocusedBorderColor = ZixoHighlight,
                        cursorColor = ZixoPrimary,
                        focusedContainerColor = ZixoBg,
                        unfocusedContainerColor = ZixoBg
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = uiState.messageText.isNotBlank(),
                    modifier = Modifier.background(ZixoPrimary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = ZixoBg)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    onLongPress: () -> Unit
) {
    val bgColor = if (isOwn) ZixoChatBubbleOwn else ZixoChatBubbleOther
    val alignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOwn) 16.dp else 4.dp,
        bottomEnd = if (isOwn) 4.dp else 16.dp
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(2.dp, shape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Forwarded label
                message.forwardedFromZixoNumber?.let { fwdNum ->
                    if (fwdNum.isNotEmpty()) {
                        Text(
                            "Forwarded",
                            color = ZixoPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Reply reference
                message.replyTo?.let { replyId ->
                    if (replyId.isNotEmpty() && message.replyToTextPreview != null) {
                        Surface(
                            color = ZixoPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    message.replyToSenderName ?: "",
                                    color = ZixoPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    message.replyToTextPreview ?: "",
                                    color = ZixoTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Deleted message placeholder
                if (message.isDeletedForEveryone) {
                    Text(
                        "This message was deleted.",
                        color = ZixoTextSecondary,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Text(
                        message.text,
                        color = ZixoText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // Reactions row
                if (message.reactions.isNotEmpty()) {
                    val reactionGroups = message.reactions.groupBy { it.reactionEmoji }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reactionGroups.forEach { (emoji, reactions) ->
                            Surface(
                                color = ZixoSurface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "$emoji ${reactions.size}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Timestamp & status
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(message.timestamp),
                        color = ZixoTextSecondary,
                        fontSize = 10.sp
                    )
                    if (isOwn) {
                        Text(
                            when (message.status) {
                                "sending" -> " \u25CB"
                                "sent" -> " \u2713"
                                "delivered" -> " \u2713\u2713"
                                "read" -> " \u2713\u2713"
                                else -> ""
                            },
                            color = if (message.status == "read") ZixoPrimary else ZixoTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionSheet(
    message: Message,
    isOwnMessage: Boolean,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZixoSurface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Message Actions", color = ZixoText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Quick reaction bar
                Text("React", color = ZixoTextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val quickReactions = listOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDD25")
                    quickReactions.forEach { emoji ->
                        TextButton(onClick = { onReact(emoji) }) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                Divider(color = ZixoHighlight)

                // Action items
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onReply).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, null, tint = ZixoPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reply", color = ZixoText, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onForward).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Forward, null, tint = ZixoPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Forward", color = ZixoText, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, null, tint = ZixoError)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Delete", color = ZixoError, fontSize = 15.sp)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun DeleteConfirmDialog(
    isOwnMessage: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZixoSurface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Delete Message", color = ZixoText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                TextButton(
                    onClick = onDeleteForMe,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete for Me", color = ZixoText, fontSize = 15.sp)
                }
                if (isOwnMessage) {
                    TextButton(
                        onClick = onDeleteForEveryone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete for Everyone", color = ZixoError, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ZixoTextSecondary)
            }
        }
    )
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return ""
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}
