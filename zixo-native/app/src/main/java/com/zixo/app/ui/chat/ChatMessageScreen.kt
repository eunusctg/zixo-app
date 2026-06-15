package com.zixo.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.CommunicationGate
import com.zixo.app.domain.model.MessageContentType
import com.zixo.app.domain.model.MessageModel
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ── Design Tokens ─────────────────────────────────────────────────────────────

private val NeonMintAlpha15 = NeonMint.copy(alpha = 0.15f)
private val IncomingBubbleColor = Color(0x1A1A2A32)
private val GlassBorderColor = Color(0x33FFFFFF)
private val InputTrayHeight = 74.dp
private val InputTrayCornerSize = 28.dp
private val BubbleCornerSize = 16.dp
private val OnlineIndicatorColor = NeonMint

/** Common 3D reaction emoji bar. */
private val ReactionEmojis = listOf("❤️", "😂", "😮", "😢", "👍", "🔥", "🎉", "💯")

// ── Screen Entry Point ────────────────────────────────────────────────────────

/**
 * Full chat message screen with keyboard avoidance and Liquid Glass design.
 *
 * Features:
 * - Wrapped in [Scaffold] with `Modifier.imePadding()` for keyboard avoidance
 * - [LazyColumn] with `reverseLayout = true` for messages — newest at bottom
 * - Auto-scroll on new messages via [LaunchedEffect]
 * - 74dp liquid glass pill-shaped input tray
 * - Outgoing messages: glowing neon mint transparency background
 * - Incoming messages: translucent dark petrol grey curves
 * - Long-press action menu with 3D reactions, reply, forward, delete, copy
 * - Reply preview bar above input tray
 * - Call state overlay when active
 * - Top bar with contact avatar, name, online status, and call icons
 * - Communication gate check — blocked overlay if not mutual contact
 * - NavController for navigation
 *
 * @param threadId The ID of the chat thread to display.
 * @param navController Navigation controller for screen transitions.
 * @param onBack Callback for the back navigation button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageScreen(
    threadId: String,
    navController: NavController,
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val thread by viewModel.thread.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val replyToMessage by viewModel.replyToMessage.collectAsState()
    val showActionMenu by viewModel.showActionMenu.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val communicationGate by viewModel.communicationGate.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Display error messages as snackbars
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Determine contact info from thread
    val otherParticipant = thread?.participantProfiles?.values?.firstOrNull {
        it.uid != currentUserId
    }
    val contactName = otherParticipant?.displayName ?: "Chat"
    val contactAvatar = otherParticipant?.avatarUrl ?: ""
    val isOnline = otherParticipant?.isOnline ?: false

    // Check if communication is blocked
    val isBlocked = communicationGate is CommunicationGate.Blocked

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated glass background
            ZixoGlassBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(innerPadding)
            ) {
                // ── Top Bar ────────────────────────────────────────────────
                ChatTopBar(
                    contactName = contactName,
                    contactAvatar = contactAvatar,
                    isOnline = isOnline,
                    onBackClick = onBack,
                    onAudioCallClick = { viewModel.startAudioCall() },
                    onVideoCallClick = { viewModel.startVideoCall() }
                )

                // ── Message List ───────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        count = messages.size,
                        key = { index -> messages[index].id }
                    ) { index ->
                        val message = messages[index]
                        val isOwnMessage = message.senderUid == currentUserId

                        MessageBubble(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onLongClick = { viewModel.showActionMenu(message) }
                        )
                    }
                }

                // ── Reply Preview Bar ──────────────────────────────────────
                AnimatedVisibility(
                    visible = replyToMessage != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    replyToMessage?.let { replied ->
                        ReplyPreviewBar(
                            senderName = replied.replyToSenderName ?: replied.senderDisplayName,
                            previewText = replied.replyToPreview ?: replied.content,
                            onDismiss = { viewModel.clearReplyTo() }
                        )
                    }
                }

                // ── Input Tray (74dp Liquid Glass Pill) ────────────────────
                InputTray(
                    value = inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    onSendClick = { viewModel.sendMessage() },
                    isSending = isSending,
                    enabled = !isBlocked,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Communication Gate Blocked Overlay ────────────────────────
            if (isBlocked) {
                CommunicationGateOverlay(
                    reason = (communicationGate as? CommunicationGate.Blocked)?.reason
                        ?: "You can only message mutual contacts"
                )
            }

            // ── Action Menu Overlay ────────────────────────────────────────
            showActionMenu?.let { actionMessage ->
                ActionMenuOverlay(
                    message = actionMessage,
                    isOwnMessage = actionMessage.senderUid == currentUserId,
                    onReact = { emoji, isThreeD ->
                        viewModel.addReaction(actionMessage.id, emoji, isThreeD)
                    },
                    onReply = {
                        viewModel.setReplyTo(actionMessage.id)
                    },
                    onForward = {
                        viewModel.forwardMessage(actionMessage.id, emptyList())
                    },
                    onDeleteForMe = {
                        viewModel.deleteForMe(actionMessage.id)
                    },
                    onDeleteForEveryone = {
                        viewModel.deleteForEveryone(actionMessage.id)
                    },
                    onCopy = {
                        // Handled inside the overlay
                    },
                    onDismiss = { viewModel.dismissActionMenu() }
                )
            }

            // ── Call State Overlay ─────────────────────────────────────────
            if (callState !is CallState.IDLE) {
                CallStateOverlay(
                    callState = callState,
                    contactName = contactName,
                    contactAvatar = contactAvatar
                )
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    contactName: String,
    contactAvatar: String,
    isOnline: Boolean,
    onBackClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Contact avatar
                if (contactAvatar.isNotBlank()) {
                    AsyncImage(
                        model = contactAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, GlassBorderColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = contactName,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        color = if (isOnline) OnlineIndicatorColor else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onAudioCallClick) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Audio Call",
                    tint = TextSecondary
                )
            }
            IconButton(onClick = onVideoCallClick) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = "Video Call",
                    tint = TextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ── Message Bubble ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageModel,
    isOwnMessage: Boolean,
    onLongClick: () -> Unit
) {
    // Deleted placeholder
    if (message.isDeletedForEveryone) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isOwnMessage) 48.dp else 0.dp,
                    end = if (!isOwnMessage) 48.dp else 0.dp
                ),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BubbleCornerSize))
                    .background(IncomingBubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "This message was deleted",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        return
    }

    // Soft-deleted for me — invisible
    if (message.isDeletedForMe) return

    val bubbleColor = if (isOwnMessage) NeonMintAlpha15 else IncomingBubbleColor
    val alignment = if (isOwnMessage) Arrangement.End else Arrangement.Start
    val shape = RoundedCornerShape(
        topStart = BubbleCornerSize,
        topEnd = BubbleCornerSize,
        bottomStart = if (isOwnMessage) BubbleCornerSize else 4.dp,
        bottomEnd = if (isOwnMessage) 4.dp else BubbleCornerSize
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwnMessage) 48.dp else 0.dp,
                end = if (!isOwnMessage) 48.dp else 0.dp
            ),
        horizontalArrangement = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(bubbleColor)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Reply reference preview
            if (message.replyToPreview != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonMintAlpha15)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = message.replyToSenderName ?: "",
                        color = NeonMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.replyToPreview ?: "",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Forwarded label
            if (message.isForwarded) {
                Text(
                    text = "Forwarded",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Media thumbnail
            if (message.type == MessageContentType.IMAGE || message.type == MessageContentType.VIDEO) {
                val imageUrl = message.mediaThumbnailUrl ?: message.mediaUrl
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Play icon overlay for video
                if (message.type == MessageContentType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkPetrolCharcoal.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Voice message indicator
            if (message.type == MessageContentType.AUDIO_VOICE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Voice",
                        tint = NeonMint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voice message",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Text content
            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }

            // Media caption
            if (!message.caption.isNullOrBlank() && message.type != MessageContentType.TEXT) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.caption,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Timestamp and read receipt row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (isOwnMessage) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val tickColor = if (message.isRead) NeonMint else TextSecondary
                    Text(text = if (message.isRead) "✓✓" else "✓", color = tickColor, fontSize = 11.sp)
                }
            }

            // Reactions row — emoji badges below the bubble
            if (message.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    message.reactions.distinctBy { it.emoji }.forEach { reaction ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassBorderColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${reaction.emoji} ${message.reactions.count { it.emoji == reaction.emoji }}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Reply Preview Bar ─────────────────────────────────────────────────────────

@Composable
private fun ReplyPreviewBar(
    senderName: String,
    previewText: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPetrolCharcoal.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NeonMint)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = senderName,
                color = NeonMint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = previewText,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss reply",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Input Tray (74dp Liquid Glass Pill) ───────────────────────────────────────

@Composable
private fun InputTray(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(InputTrayHeight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(InputTrayCornerSize))
            .background(DarkPetrolCharcoal.copy(alpha = 0.6f))
            .border(1.dp, GlassBorderColor, RoundedCornerShape(InputTrayCornerSize))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attach button (camera/image icon)
        IconButton(
            onClick = { /* TODO: Launch media picker */ },
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = "Attach",
                tint = if (enabled) TextSecondary else TextSecondary.copy(alpha = 0.4f)
            )
        }

        // Text input field (GlassOutlinedTextField style)
        GlassOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "Message…",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            },
            modifier = Modifier
                .weight(1f),
            enabled = enabled,
            singleLine = false,
            maxLength = 4096
        )

        // Send button (NeonMint arrow icon)
        IconButton(
            onClick = onSendClick,
            enabled = value.isNotBlank() && !isSending && enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (value.isNotBlank() && !isSending && enabled) NeonMint else TextSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

// ── Action Menu Overlay (Frosted Glass) ───────────────────────────────────────

@Composable
private fun ActionMenuOverlay(
    message: MessageModel,
    isOwnMessage: Boolean,
    onReact: (emoji: String, isThreeD: Boolean) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        // Scrim to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                    onLongClick = {}
                )
        )

        // Frosted glass action panel — uses liquidGlassCard()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .liquidGlassCard()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 3D Reactions bar (floating emoji picker with premium symbols)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReactionEmojis.forEach { emoji ->
                    IconButton(
                        onClick = { onReact(emoji, true) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Reply action
            ActionMenuItem(
                icon = Icons.Filled.Reply,
                label = "Reply",
                onClick = onReply
            )

            // Forward action
            ActionMenuItem(
                icon = Icons.Filled.Forward,
                label = "Forward",
                onClick = onForward
            )

            // Copy option
            ActionMenuItem(
                icon = Icons.Filled.ContentCopy,
                label = "Copy",
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.content))
                    onDismiss()
                }
            )

            // Delete for Me
            ActionMenuItem(
                icon = Icons.Filled.Delete,
                label = "Delete for Me",
                onClick = onDeleteForMe,
                tint = Color(0xFFFF5252)
            )

            // Delete for Everyone — only for own messages
            if (isOwnMessage) {
                ActionMenuItem(
                    icon = Icons.Filled.DeleteForever,
                    label = "Delete for Everyone",
                    onClick = onDeleteForEveryone,
                    tint = Color(0xFFFF5252)
                )
            }
        }
    }
}

// ── Action Menu Item ──────────────────────────────────────────────────────────

@Composable
private fun ActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {}
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Communication Gate Blocked Overlay ────────────────────────────────────────

/**
 * Full-screen overlay shown when the communication gate blocks messaging.
 * Displays a message explaining that only mutual contacts can exchange messages.
 */
@Composable
private fun CommunicationGateOverlay(
    reason: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .liquidGlassContainer()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Communication Blocked",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Call State Overlay ────────────────────────────────────────────────────────

@Composable
private fun CallStateOverlay(
    callState: CallState,
    contactName: String,
    contactAvatar: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPetrolCharcoal.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusText = when (callState) {
                is CallState.DIALING -> "Calling ${callState.targetDisplayName}…"
                is CallState.RINGING -> "Incoming call from ${callState.callerDisplayName}…"
                is CallState.CONNECTED -> "Connected with ${callState.targetDisplayName}"
                is CallState.ENDED -> "Call ended"
                else -> ""
            }

            AsyncImage(
                model = contactAvatar,
                contentDescription = "Contact",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonMint, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = contactName,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

/** Formats epoch milliseconds to a readable time string. */
private fun formatTimestamp(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(epochMs))
}
