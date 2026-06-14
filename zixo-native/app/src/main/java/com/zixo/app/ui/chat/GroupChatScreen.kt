package com.zixo.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zixo.app.domain.model.CallState
import com.zixo.app.domain.model.MessageContentType
import com.zixo.app.domain.model.MessageModel
import com.zixo.app.domain.model.ParticipantRole
import com.zixo.app.domain.model.ThreadParticipant
import com.zixo.app.ui.components.ZixoGlassBackground
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
private val ReactionEmojis = listOf("❤️", "😂", "😮", "😢", "👍", "🔥", "🎉", "💯")
private val AdminBadgeColor = NeonMint

// ── Screen Entry Point ────────────────────────────────────────────────────────

/**
 * Group chat screen — extends ChatMessageScreen with group-specific features.
 *
 * Features:
 * - Group-specific header: group name, avatar, participant count
 * - Tap header to show group info panel (slide-up Liquid Glass sheet)
 * - Group info panel with avatar, name, description, participant list
 * - Admin badges on participant rows
 * - "Add participant" button (admin only, mutual contacts)
 * - "Leave group" option
 * - Participant avatars in the top bar
 * - Group calling: audio/video call buttons start LiveKit Room sessions
 * - Ephemeral timer indicator
 * - Same keyboard avoidance (74dp input tray, reverseLayout, imePadding)
 *
 * @param threadId The ID of the group chat thread to display.
 * @param onBack Callback for the back navigation button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupChatScreen(
    threadId: String,
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

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showGroupInfoPanel by remember { mutableStateOf(false) }

    // Load thread data on first composition
    LaunchedEffect(threadId) {
        viewModel.loadThread(threadId)
    }

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

    // Group-specific data
    val groupName = thread?.groupName ?: "Group"
    val groupAvatar = thread?.groupAvatarUrl ?: ""
    val participants = thread?.participantProfiles?.values?.toList() ?: emptyList()
    val participantCount = participants.size
    val isAdmin = thread?.groupAdminUids?.contains(currentUserId) == true
    val ephemeralSeconds = thread?.ephemeralTimerSeconds ?: 0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ZixoGlassBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(innerPadding)
            ) {
                // ── Group Top Bar ──────────────────────────────────────────
                GroupChatTopBar(
                    groupName = groupName,
                    groupAvatar = groupAvatar,
                    participantCount = participantCount,
                    participants = participants,
                    onBackClick = onBack,
                    onHeaderClick = { showGroupInfoPanel = true },
                    onAudioCallClick = { viewModel.startGroupAudioCall() },
                    onVideoCallClick = { viewModel.startGroupVideoCall() }
                )

                // ── Ephemeral Timer Indicator ─────────────────────────────
                if (ephemeralSeconds > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonMintAlpha15)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⏱ Messages disappear in ${formatEphemeralTimer(ephemeralSeconds)}",
                            color = NeonMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

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

                        GroupMessageBubble(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            participants = participants,
                            onLongClick = { viewModel.showActionMenu.value = message }
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
                            onDismiss = { viewModel.clearReply() }
                        )
                    }
                }

                // ── Input Tray (74dp Liquid Glass Pill) ────────────────────
                GroupInputTray(
                    value = inputText,
                    onValueChange = { viewModel.inputText.value = it },
                    onSendClick = { viewModel.sendMessage() },
                    isSending = isSending,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Group Info Panel (Slide-up Sheet) ─────────────────────────
            if (showGroupInfoPanel) {
                GroupInfoPanel(
                    groupName = groupName,
                    groupAvatar = groupAvatar,
                    groupDescription = thread?.groupDescription ?: "",
                    participants = participants,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    onClose = { showGroupInfoPanel = false }
                )
            }

            // ── Action Menu Overlay ────────────────────────────────────────
            showActionMenu?.let { actionMessage ->
                GroupActionMenuOverlay(
                    message = actionMessage,
                    isOwnMessage = actionMessage.senderUid == currentUserId,
                    onReact = { emoji, isThreeD ->
                        viewModel.addReaction(actionMessage.id, emoji, isThreeD)
                    },
                    onReply = {
                        viewModel.replyToMessage.value = actionMessage
                        viewModel.dismissActionMenu()
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
                    onDismiss = { viewModel.dismissActionMenu() }
                )
            }

            // ── Call State Overlay ─────────────────────────────────────────
            if (callState !is CallState.IDLE) {
                GroupCallOverlay(
                    callState = callState,
                    groupName = groupName,
                    groupAvatar = groupAvatar
                )
            }
        }
    }
}

// ── Group Top Bar ─────────────────────────────────────────────────────────────

@Composable
private fun GroupChatTopBar(
    groupName: String,
    groupAvatar: String,
    participantCount: Int,
    participants: List<ThreadParticipant>,
    onBackClick: () -> Unit,
    onHeaderClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHeaderClick,
                        onLongClick = {}
                    )
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = groupName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$participantCount participants",
                        color = TextSecondary,
                        fontSize = 12.sp
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
            // Participant avatars in top bar (show up to 3)
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(participants.take(3)) { participant ->
                    AsyncImage(
                        model = participant.avatarUrl,
                        contentDescription = participant.displayName,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, GlassBorderColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onAudioCallClick) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Group Audio Call",
                    tint = TextSecondary
                )
            }
            IconButton(onClick = onVideoCallClick) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = "Group Video Call",
                    tint = TextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ── Group Message Bubble ──────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupMessageBubble(
    message: MessageModel,
    isOwnMessage: Boolean,
    participants: List<ThreadParticipant>,
    onLongClick: () -> Unit
) {
    if (message.isDeletedForEveryone) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp),
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
            .padding(start = if (isOwnMessage) 48.dp else 0.dp, end = if (!isOwnMessage) 48.dp else 0.dp),
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
            // Sender name label (group-specific: show for incoming messages)
            if (!isOwnMessage) {
                val sender = participants.firstOrNull { it.uid == message.senderUid }
                val senderColor = senderNameColor(message.senderUid)
                Text(
                    text = sender?.displayName ?: message.senderDisplayName,
                    color = senderColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

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
                    Text(text = "Voice message", color = TextSecondary, fontSize = 14.sp)
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
                Text(text = message.caption, color = TextPrimary, fontSize = 14.sp)
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

            // Reactions row
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

// ── Group Info Panel ──────────────────────────────────────────────────────────

@Composable
private fun GroupInfoPanel(
    groupName: String,
    groupAvatar: String,
    groupDescription: String,
    participants: List<ThreadParticipant>,
    currentUserId: String,
    isAdmin: Boolean,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        // Scrim dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                    onLongClick = {}
                )
        )

        // Slide-up glass sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DarkPetrolCharcoal.copy(alpha = 0.92f))
                .border(1.dp, GlassBorderColor, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(24.dp)
        ) {
            // Close handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextSecondary.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Group avatar
            AsyncImage(
                model = groupAvatar,
                contentDescription = "Group Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, GlassBorderColor, RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Group name
            Text(
                text = groupName,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Group description
            if (groupDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = groupDescription,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Participant count
            Text(
                text = "${participants.size} participants",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Participant list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(participants) { participant ->
                    ParticipantRow(
                        participant = participant,
                        isCurrentUser = participant.uid == currentUserId,
                        isAdmin = participant.role == ParticipantRole.ADMIN
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add participant (admin only)
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonMintAlpha15)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { },
                            onLongClick = {}
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Add Participant",
                        tint = NeonMint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add participant",
                        color = NeonMint,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Leave group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FF5252))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { },
                        onLongClick = {}
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = "Leave Group",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Leave group",
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Participant Row ───────────────────────────────────────────────────────────

@Composable
private fun ParticipantRow(
    participant: ThreadParticipant,
    isCurrentUser: Boolean,
    isAdmin: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IncomingBubbleColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = participant.avatarUrl,
            contentDescription = participant.displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, GlassBorderColor, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isCurrentUser) "You" else participant.displayName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Admin",
                        color = AdminBadgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = participant.zixoNumber,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        if (participant.isOnline) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(OnlineIndicatorColor)
            )
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

// ── Group Input Tray ──────────────────────────────────────────────────────────

@Composable
private fun GroupInputTray(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(InputTrayHeight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(InputTrayCornerSize))
            .background(DarkPetrolCharcoal.copy(alpha = 0.6f))
            .border(1.dp, GlassBorderColor, RoundedCornerShape(InputTrayCornerSize))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(NeonMint),
            maxLines = 4,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Message…",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        IconButton(
            onClick = onSendClick,
            enabled = value.isNotBlank() && !isSending,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (value.isNotBlank() && !isSending) NeonMint else TextSecondary
            )
        }
    }
}

// ── Group Action Menu Overlay ─────────────────────────────────────────────────

@Composable
private fun GroupActionMenuOverlay(
    message: MessageModel,
    isOwnMessage: Boolean,
    onReact: (emoji: String, isThreeD: Boolean) -> Unit,
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
    ) {
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkPetrolCharcoal.copy(alpha = 0.85f))
                .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            ActionMenuItem(icon = Icons.Filled.Reply, label = "Reply", onClick = onReply)
            ActionMenuItem(icon = Icons.Filled.Forward, label = "Forward", onClick = onForward)
            ActionMenuItem(
                icon = Icons.Filled.Delete,
                label = "Delete for Me",
                onClick = onDeleteForMe,
                tint = Color(0xFFFF5252)
            )
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

// ── Group Call Overlay ────────────────────────────────────────────────────────

@Composable
private fun GroupCallOverlay(
    callState: CallState,
    groupName: String,
    groupAvatar: String
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
                is CallState.DIALING -> "Starting group call…"
                is CallState.RINGING -> "Incoming group call…"
                is CallState.CONNECTED -> "Group call connected • ${callState.participantCount} participants"
                is CallState.ENDED -> "Group call ended"
                else -> ""
            }

            AsyncImage(
                model = groupAvatar,
                contentDescription = "Group",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, NeonMint, RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = groupName,
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

/** Formats ephemeral timer seconds into a human-readable string. */
private fun formatEphemeralTimer(seconds: Int): String {
    return when {
        seconds >= 86400 -> "${seconds / 86400} day${if (seconds / 86400 > 1) "s" else ""}"
        seconds >= 3600 -> "${seconds / 3600} hour${if (seconds / 3600 > 1) "s" else ""}"
        seconds >= 60 -> "${seconds / 60} minute${if (seconds / 60 > 1) "s" else ""}"
        else -> "$seconds second${if (seconds > 1) "s" else ""}"
    }
}

/** Generates a deterministic color for a sender's name in group chats. */
private fun senderNameColor(uid: String): Color {
    val colors = listOf(
        NeonMint,
        Color(0xFF42A5F5),
        Color(0xFFAB47BC),
        Color(0xFFFF7043),
        Color(0xFFFFCA28),
        Color(0xFF26C6DA),
        Color(0xFFEC407A),
        Color(0xFF66BB6A)
    )
    val index = uid.hashCode().mod(colors.size).let { if (it < 0) -it else it }
    return colors[index]
}
