package com.zixo.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.Message
import com.zixo.app.domain.model.ReactionPanel
import com.zixo.app.ui.components.LiquidGlassSurface
import com.zixo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageScreen(
    chatViewModel: ChatViewModel,
    currentUserId: String,
    chatName: String,
    chatAvatar: String,
    onBack: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    val inputState by chatViewModel.chatInputState.collectAsState()
    val showActionSheet by chatViewModel.showActionSheet.collectAsState()
    val showReactionPanel by chatViewModel.showReactionPanel.collectAsState()
    val selectedMessage by chatViewModel.selectedMessage.collectAsState()
    val error by chatViewModel.error.collectAsState()

    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ZixoSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (chatAvatar.isNotBlank()) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(chatAvatar),
                                    contentDescription = chatName,
                                    modifier = Modifier.size(36.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = chatName.take(1).uppercase(),
                                    color = ZixoAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = chatName,
                            color = ZixoTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = ZixoTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            // Messages list — reverseLayout for newest at bottom
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(
                        message = message,
                        isSender = chatViewModel.isSender(message, currentUserId),
                        currentUserId = currentUserId,
                        onLongPress = { chatViewModel.onMessageLongPress(message) }
                    )
                }
            }

            // Reply bar
            AnimatedVisibility(visible = inputState.isReplying) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ZixoAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = inputState.replyToMessage?.senderId?.take(8) ?: "",
                                color = ZixoAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = inputState.replyToMessage?.text ?: "",
                                color = ZixoTextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { chatViewModel.onCancelReply() }) {
                            Icon(Icons.Filled.Close, "Cancel", tint = ZixoTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Input bar — 72dp height
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                cornerRadius = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Image picker */ }) {
                        Icon(Icons.Filled.Image, "Attach", tint = ZixoTextSecondary)
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Message...", color = ZixoTextTertiary, fontSize = 15.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = ZixoAccent,
                            focusedTextColor = ZixoTextPrimary,
                            unfocusedTextColor = ZixoTextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            "Send",
                            tint = if (messageText.isNotBlank()) ZixoAccent else ZixoTextTertiary
                        )
                    }
                }
            }
        }

        // Action sheet overlay
        if (showActionSheet && selectedMessage != null) {
            ActionSheetOverlay(
                message = selectedMessage!!,
                isSender = chatViewModel.isSender(selectedMessage!!, currentUserId),
                canDeleteForEveryone = chatViewModel.canDeleteForEveryone(selectedMessage!!, currentUserId),
                onReact = { chatViewModel.showReactionPanel() },
                onReply = { chatViewModel.onReply() },
                onForward = { chatViewModel.onForward() },
                onDeleteForMe = { chatViewModel.onDeleteForMe() },
                onDeleteForEveryone = { chatViewModel.onDeleteForEveryone() },
                onDismiss = { chatViewModel.dismissActionSheet() }
            )
        }

        // Reaction panel overlay
        if (showReactionPanel) {
            ReactionPanelOverlay(
                onReact = { chatViewModel.onReactionClick(it) },
                onDismiss = { chatViewModel.dismissActionSheet() }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isSender: Boolean,
    currentUserId: String,
    onLongPress: () -> Unit
) {
    val displayText = message.displayText(currentUserId)
    val isDeleted = message.isDeletedForEveryone || message.deletedForUsers.contains(currentUserId)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSender) 18.dp else 4.dp,
                        bottomEnd = if (isSender) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isSender) OutgoingBubble else IncomingBubble,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSender) 18.dp else 4.dp,
                        bottomEnd = if (isSender) 4.dp else 18.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSender) 18.dp else 4.dp,
                        bottomEnd = if (isSender) 4.dp else 18.dp
                    )
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                // Reply reference
                if (message.replyToText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZixoSurface.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToSender.take(8),
                                color = ZixoAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = message.replyToText,
                                color = ZixoTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message text
                Text(
                    text = displayText,
                    color = if (isDeleted) ZixoTextTertiary else ZixoTextPrimary,
                    fontSize = 15.sp,
                    fontStyle = if (isDeleted) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                )

                // Timestamp and reactions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reactions
                    if (message.reactions.isNotEmpty()) {
                        Row {
                            message.reactions.values.distinct().forEach { emoji ->
                                Text(text = emoji, fontSize = 14.sp)
                            }
                        }
                    }

                    Text(
                        text = formatMessageTime(message.timestamp),
                        color = ZixoTextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionSheetOverlay(
    message: Message,
    isSender: Boolean,
    canDeleteForEveryone: Boolean,
    onReact: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ActionSheetItem("React", Icons.Filled.EmojiEmotions, onReact)
                ActionSheetItem("Reply", Icons.Filled.Reply, onReply)
                ActionSheetItem("Forward", Icons.Filled.Forward, onForward)
                HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                ActionSheetItem("Delete for Me", Icons.Filled.Delete, onDeleteForMe, ZixoError)
                if (canDeleteForEveryone) {
                    ActionSheetItem("Delete for Everyone", Icons.Filled.DeleteForever, onDeleteForEveryone, ZixoError)
                }
            }
        }
    }
}

@Composable
private fun ActionSheetItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color = ZixoTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, title, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReactionPanelOverlay(
    onReact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier.padding(24.dp),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "React",
                    color = ZixoTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Quick reactions grid
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReactionPanel.quickReactions.forEach { reaction ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onReact(reaction.emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = reaction.emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
