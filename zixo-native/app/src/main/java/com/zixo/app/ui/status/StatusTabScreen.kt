package com.zixo.app.ui.status

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zixo.app.domain.model.StatusContentType
import com.zixo.app.domain.model.StatusGroupModel
import com.zixo.app.domain.model.StatusModel
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.components.liquidGlassContainer
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.EmeraldGreen
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────
// Design Tokens
// ──────────────────────────────────────────────

private val GlassBackground = Color(0x1A1A2A32)
private val GlassBorder = Color(0x33FFFFFF)
private val ViewedRingColor = Color(0xFF546E7A)
private val StatusProgressBackground = Color(0x33FFFFFF)

/** Preset background colors for text statuses. */
private val TextStatusBackgroundColors = listOf(
    Color(0xFF00E676), Color(0xFF05C46B), Color(0xFF00838F),
    Color(0xFF5C6BC0), Color(0xFFE91E63), Color(0xFFFF5722),
    Color(0xFFFFB300), Color(0xFF7B1FA2), Color(0xFF1A2A32),
    Color(0xFF0B1519)
)

/** Available font families for text statuses. */
private val TextStatusFonts = listOf("Default", "Serif", "Monospace", "Cursive")

/** Auto-advance duration for text/image statuses in milliseconds. */
private const val STATUS_AUTO_ADVANCE_MS = 5_000L

// ──────────────────────────────────────────────
// Main Status Tab Screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusTabScreen(
    viewModel: StatusViewModel = hiltViewModel()
) {
    val statusFeed by viewModel.statusFeed.collectAsStateWithLifecycle()
    val myStatuses by viewModel.myStatuses.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val viewingGroup by viewModel.viewingStatusGroup.collectAsStateWithLifecycle()
    val viewingIndex by viewModel.currentViewingIndex.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var showTextComposer by remember { mutableStateOf(false) }
    var showMediaComposer by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Clear error on consumption
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3_000L)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ─────────────────────────────
            Text(
                text = "Status",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp)
            )

            // ── My Status Card ─────────────────────
            MyStatusCard(
                myStatuses = myStatuses.myStatuses,
                isUploading = isUploading,
                uploadProgress = uploadProgress,
                onAddClick = { showAddSheet = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Recent Updates Header ──────────────
            Text(
                text = "Recent Updates",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // ── Status Feed ────────────────────────
            if (statusFeed.isEmpty()) {
                EmptyStatusFeed()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = statusFeed,
                        key = { it.senderUid }
                    ) { group ->
                        StatusGroupItem(
                            group = group,
                            onClick = { viewModel.startViewing(group) }
                        )
                    }
                }
            }
        }

        // ── Error Snack ──────────────────────────
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(12.dp)
                ) {
                    Text(text = msg, color = DestructiveText, fontSize = 14.sp)
                }
            }
        }
    }

    // ── Add Status Bottom Sheet ─────────────────
    if (showAddSheet) {
        AddStatusBottomSheet(
            sheetState = sheetState,
            onDismiss = { showAddSheet = false },
            onTextClick = {
                showAddSheet = false
                showTextComposer = true
            },
            onPhotoClick = {
                showAddSheet = false
                isVideoMode = false
                showMediaComposer = true
            },
            onVideoClick = {
                showAddSheet = false
                isVideoMode = true
                showMediaComposer = true
            }
        )
    }

    // ── Text Status Composer ────────────────────
    if (showTextComposer) {
        TextStatusComposer(
            isUploading = isUploading,
            onDismiss = { showTextComposer = false },
            onPost = { text, bgColor ->
                viewModel.postTextStatus(text, bgColor)
                showTextComposer = false
            }
        )
    }

    // ── Media Status Composer ───────────────────
    if (showMediaComposer) {
        MediaStatusComposer(
            isVideoMode = isVideoMode,
            isUploading = isUploading,
            uploadProgress = uploadProgress,
            onDismiss = { showMediaComposer = false },
            onPost = { filePath, caption ->
                if (isVideoMode) {
                    viewModel.postVideoStatus(filePath, caption)
                } else {
                    viewModel.postImageStatus(filePath, caption)
                }
                showMediaComposer = false
            }
        )
    }

    // ── Full-Screen Status Viewer ───────────────
    if (viewingGroup != null) {
        StatusViewerOverlay(
            group = viewingGroup!!,
            currentIndex = viewingIndex,
            replyText = replyText,
            onReplyTextChange = { replyText = it },
            onPrevious = viewModel::previousStatus,
            onNext = viewModel::nextStatus,
            onClose = viewModel::stopViewing,
            onReaction = { statusId, emoji -> viewModel.addReaction(statusId, emoji) }
        )
    }
}

// ──────────────────────────────────────────────
// My Status Card
// ──────────────────────────────────────────────

@Composable
private fun MyStatusCard(
    myStatuses: List<StatusModel>,
    isUploading: Boolean,
    uploadProgress: Float,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAddClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            if (myStatuses.isNotEmpty()) {
                StatusRingAvatar(
                    avatarUrl = "",
                    name = "You",
                    size = 64.dp,
                    segmentCount = myStatuses.size,
                    ringColor = NeonMint
                )
            } else {
                AvatarComponent(
                    imageUrl = "",
                    name = "You",
                    size = 64.dp
                )
            }

            // Upload progress overlay
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        color = NeonMint,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Add button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(NeonMint)
                    .border(2.dp, DarkPetrolCharcoal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add status",
                    tint = DarkPetrolCharcoal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "My Status",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (myStatuses.isNotEmpty()) {
                Text(
                    text = "${myStatuses.size} update${if (myStatuses.size != 1) "s" else ""}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "Tap to add status update",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Status Group Item in Feed
// ──────────────────────────────────────────────

@Composable
private fun StatusGroupItem(
    group: StatusGroupModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusRingAvatar(
            avatarUrl = group.senderAvatarUrl,
            name = group.senderDisplayName,
            size = 56.dp,
            segmentCount = group.statusCount,
            ringColor = if (group.hasUnviewedStatuses) NeonMint else ViewedRingColor
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.senderDisplayName,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatRelativeTime(group.latestTimestamp),
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

// ──────────────────────────────────────────────
// Status Ring Avatar
// ──────────────────────────────────────────────

@Composable
private fun StatusRingAvatar(
    avatarUrl: String,
    name: String,
    size: Dp,
    segmentCount: Int,
    ringColor: Color
) {
    val strokeWidth = 3.dp
    val gapAngle = 6f
    val totalGap = gapAngle * segmentCount
    val segmentSweep = if (segmentCount > 0) (360f - totalGap) / segmentCount else 0f

    Box(contentAlignment = Alignment.Center) {
        // Colored ring segments
        if (segmentCount > 0) {
            Canvas(modifier = Modifier.size(size + strokeWidth * 2)) {
                var startAngle = -90f
                for (i in 0 until segmentCount) {
                    drawArc(
                        color = ringColor,
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidth.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += segmentSweep + gapAngle
                }
            }
        }

        AvatarComponent(
            imageUrl = avatarUrl,
            name = name,
            size = size
        )
    }
}

// ──────────────────────────────────────────────
// Empty Status Feed
// ──────────────────────────────────────────────

@Composable
private fun EmptyStatusFeed() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No recent updates",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Statuses from your contacts will appear here",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}

// ──────────────────────────────────────────────
// Add Status Bottom Sheet
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStatusBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onTextClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkPetrolCharcoal,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Create Status",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AddOptionRow(
                icon = Icons.Filled.TextFields,
                label = "Text",
                description = "Share what's on your mind",
                onClick = onTextClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            AddOptionRow(
                icon = Icons.Filled.Image,
                label = "Photo",
                description = "Share a moment with a photo",
                onClick = onPhotoClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            AddOptionRow(
                icon = Icons.Filled.Videocam,
                label = "Video",
                description = "Share a video clip",
                onClick = onVideoClick
            )
        }
    }
}

@Composable
private fun AddOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeonMint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonMint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

// ──────────────────────────────────────────────
// Text Status Composer
// ──────────────────────────────────────────────

@Composable
private fun TextStatusComposer(
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (text: String, backgroundColor: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedFontIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TextStatusBackgroundColors[selectedColorIndex])
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        // Text input area
        GlassOutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {
                Text(
                    text = "Type a status...",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            singleLine = false,
            enabled = !isUploading
        )

        // Color picker
        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(TextStatusBackgroundColors) { index, color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (index == selectedColorIndex) {
                                Modifier.border(3.dp, Color.White, CircleShape)
                            } else {
                                Modifier.border(1.dp, GlassBorder, CircleShape)
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedColorIndex = index }
                        )
                )
            }
        }

        // Font selector
        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(TextStatusFonts) { index, fontName ->
                val fontFamily = when (fontName) {
                    "Serif" -> FontFamily.Serif
                    "Monospace" -> FontFamily.Monospace
                    "Cursive" -> FontFamily.Cursive
                    else -> FontFamily.Default
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (index == selectedFontIndex) NeonMint.copy(alpha = 0.3f)
                            else GlassBackground
                        )
                        .border(
                            1.dp,
                            if (index == selectedFontIndex) NeonMint else GlassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedFontIndex = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = fontName,
                        color = if (index == selectedFontIndex) NeonMint else TextSecondary,
                        fontFamily = fontFamily,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Post button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(NeonMint)
                    .clickable(enabled = text.isNotBlank() && !isUploading) {
                        val bgColor = colorToHex(TextStatusBackgroundColors[selectedColorIndex])
                        onPost(text.trim(), bgColor)
                    }
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = DarkPetrolCharcoal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "Post",
                        color = DarkPetrolCharcoal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Media Status Composer
// ──────────────────────────────────────────────

@Composable
private fun MediaStatusComposer(
    isVideoMode: Boolean,
    isUploading: Boolean,
    uploadProgress: Float,
    onDismiss: () -> Unit,
    onPost: (filePath: String, caption: String?) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }

    val mediaType = if (isVideoMode) {
        ActivityResultContracts.PickVisualMedia.VideoOnly
    } else {
        ActivityResultContracts.PickVisualMedia.ImageOnly
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    LaunchedEffect(Unit) {
        pickerLauncher.launch(
            PickVisualMediaRequest(mediaType)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = TextPrimary
            )
        }

        if (selectedUri != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkPetrolCharcoal)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    progress = { uploadProgress },
                                    color = NeonMint,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${(uploadProgress * 100).toInt()}%",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Caption input
                GlassOutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add a caption...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Post button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(NeonMint)
                        .clickable(enabled = !isUploading) {
                            selectedUri?.let { uri ->
                                onPost(uri.toString(), caption.ifBlank { null })
                            }
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = DarkPetrolCharcoal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Uploading...",
                            color = DarkPetrolCharcoal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            tint = DarkPetrolCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Post",
                            color = DarkPetrolCharcoal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // No media selected
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No media selected",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to select again",
                        color = NeonMint,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(mediaType)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Full-Screen Status Viewer Overlay
// ──────────────────────────────────────────────

@Composable
private fun StatusViewerOverlay(
    group: StatusGroupModel,
    currentIndex: Int,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onReaction: (statusId: String, emoji: String) -> Unit
) {
    val currentStatus = group.statuses.getOrNull(currentIndex) ?: return

    // Auto-advance timer
    LaunchedEffect(currentIndex, currentStatus.id) {
        val duration = if (currentStatus.type == StatusContentType.VIDEO) {
            30_000L // Longer for video; real duration would come from media metadata
        } else {
            STATUS_AUTO_ADVANCE_MS
        }
        delay(duration)
        onNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Progress Bars at Top ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
                .zIndex(2f),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            group.statuses.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(StatusProgressBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .then(
                                when {
                                    index < currentIndex -> Modifier.fillMaxWidth()
                                    index == currentIndex -> Modifier.fillMaxWidth() // Animated fill handled by timer
                                    else -> Modifier.fillMaxWidth(0f)
                                }
                            )
                            .background(NeonMint)
                    )
                }
            }
        }

        // ── Close Button ─────────────────────
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 32.dp, start = 4.dp)
                .zIndex(2f)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Close viewer",
                tint = Color.White
            )
        }

        // ── Sender Info ──────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp, start = 52.dp, end = 16.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarComponent(
                imageUrl = group.senderAvatarUrl,
                name = group.senderDisplayName,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = group.senderDisplayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatRelativeTime(currentStatus.createdAt),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        // ── Status Content ───────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 3) {
                                onPrevious()
                            } else {
                                onNext()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            StatusContentRenderer(status = currentStatus)
        }

        // ── Reply / Reaction Bar ─────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassOutlinedTextField(
                value = replyText,
                onValueChange = onReplyTextChange,
                placeholder = { Text("Reply...", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Quick emoji reactions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("❤️", "😂", "😍", "😮").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReaction(currentStatus.id, emoji) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Status Content Renderer
// ──────────────────────────────────────────────

@Composable
private fun StatusContentRenderer(status: StatusModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (status.type) {
            StatusContentType.TEXT -> {
                val bgColor = status.backgroundColor?.let { parseHexColor(it) } ?: EmeraldGreen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.textContent ?: "",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            StatusContentType.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(status.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Image status",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                if (!status.caption.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = status.caption,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            StatusContentType.VIDEO -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶ Video",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                }
                if (!status.caption.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = status.caption,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            StatusContentType.SHAPE -> {
                val bgColor = status.backgroundColor?.let { parseHexColor(it) } ?: EmeraldGreen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.textContent ?: "",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            StatusContentType.EMOJI_3D -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkPetrolCharcoal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.emoji3dCode ?: "✨",
                        fontSize = 80.sp
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Utility Functions
// ──────────────────────────────────────────────

private fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${hours / 24}d ago"
    }
}

private fun colorToHex(color: Color): String {
    val alpha = (color.alpha * 255).toInt()
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return "#${String.format("%02X", alpha)}${String.format("%02X", red)}${String.format("%02X", green)}${String.format("%02X", blue)}"
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val argb = cleanHex.toLong(16)
        Color(argb)
    } catch (_: Exception) {
        EmeraldGreen
    }
}

private val DestructiveText = Color(0xFFFF5252)
private fun Modifier.zIndex(z: Float): Modifier = this.graphicsLayer { translationZ = z }
