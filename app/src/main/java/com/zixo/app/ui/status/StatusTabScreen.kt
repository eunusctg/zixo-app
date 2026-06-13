package com.zixo.app.ui.status

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.StatusUpdate
import com.zixo.app.domain.model.UserStatus
import com.zixo.app.domain.model.SymbolCatalog
import com.zixo.app.domain.model.ReactionPanel
import com.zixo.app.ui.components.LiquidGlassSurface
import com.zixo.app.ui.theme.*

@Composable
fun StatusTabScreen(
    statusViewModel: StatusViewModel
) {
    val userStatuses by statusViewModel.userStatuses.collectAsState()
    val myStatuses by statusViewModel.myStatuses.collectAsState()
    val showCreator by statusViewModel.showCreator.collectAsState()
    val showViewer by statusViewModel.showViewer.collectAsState()
    val selectedStatus by statusViewModel.selectedStatus.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // My Status
            Text(
                text = "My Status",
                color = ZixoTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZixoSurface)
                    .clickable { statusViewModel.showCreator() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ZixoAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        "Add Status",
                        tint = ZixoAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (myStatuses.isEmpty()) "Add to my status" else "My status (${myStatuses.size})",
                        color = ZixoTextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Tap to add status update",
                        color = ZixoTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Updates
            Text(
                text = "Recent Updates",
                color = ZixoTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (userStatuses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = ZixoTextTertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recent updates",
                            color = ZixoTextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(userStatuses) { userStatus ->
                        UserStatusItem(
                            userStatus = userStatus,
                            onClick = {
                                userStatus.statuses.firstOrNull()?.let {
                                    statusViewModel.viewStatus(it)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Status viewer overlay
        if (showViewer && selectedStatus != null) {
            StatusViewerOverlay(
                status = selectedStatus!!,
                onReact = { statusViewModel.addReaction(it) },
                onDismiss = { statusViewModel.dismissViewer() }
            )
        }

        // Status creator overlay
        if (showCreator) {
            StatusCreatorOverlay(
                onCreateText = { text, color -> statusViewModel.createTextStatus(text, color) },
                onDismiss = { statusViewModel.dismissCreator() }
            )
        }
    }
}

@Composable
private fun UserStatusItem(
    userStatus: UserStatus,
    onClick: () -> Unit
) {
    // Animated border for unviewed statuses
    val borderBrush = if (userStatus.hasUnviewed) {
        Brush.sweepGradient(
            colors = listOf(ZixoAccent, ZixoAccentDark, ZixoAccent, ZixoAccentDark)
        )
    } else {
        Brush.sweepGradient(
            colors = listOf(ZixoTextTertiary, ZixoTextTertiary)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with animated border
        Box(
            modifier = Modifier
                .size(56.dp)
                .drawBehind {
                    drawCircle(
                        brush = borderBrush,
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ZixoSurface),
                contentAlignment = Alignment.Center
            ) {
                if (userStatus.userAvatar.isNotBlank()) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(userStatus.userAvatar),
                        contentDescription = userStatus.userName,
                        modifier = Modifier.size(48.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = userStatus.userName.take(1).uppercase(),
                        color = ZixoAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = userStatus.userName,
                color = ZixoTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = "${userStatus.statuses.size} update${if (userStatus.statuses.size != 1) "s" else ""}",
                color = ZixoTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StatusViewerOverlay(
    status: StatusUpdate,
    onReact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (status.type) {
                com.zixo.app.domain.model.StatusType.TEXT -> {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(android.graphics.Color.parseColor(status.backgroundColor))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = status.text,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                com.zixo.app.domain.model.StatusType.IMAGE -> {
                    if (status.imageUrl.isNotBlank()) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(status.imageUrl),
                            contentDescription = "Status image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            // Quick reactions
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReactionPanel.quickReactions.take(6).forEach { reaction ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassBackground)
                            .clickable { onReact(reaction.emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = reaction.emoji, fontSize = 20.sp)
                    }
                }
            }
        }

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Filled.Close, "Close", tint = Color.White)
        }
    }
}

@Composable
private fun StatusCreatorOverlay(
    onCreateText: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#1A2A32") }
    var showSymbolPanel by remember { mutableStateOf(false) }

    val colors = listOf("#1A2A32", "#0B1519", "#1B5E20", "#0D47A1", "#4A148C", "#B71C1C", "#E65100", "#006064")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = ZixoTextPrimary)
                }
                Text(
                    text = "Create Status",
                    color = ZixoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text input with colored background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(android.graphics.Color.parseColor(selectedColor)))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { if (it.length <= 200) statusText = it },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = {
                        Text(
                            "Type a status...",
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = ZixoAccent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color picker
            Text("Background", color = ZixoTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colors.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colors[index])))
                            .border(
                                width = if (colors[index] == selectedColor) 3.dp else 0.dp,
                                color = if (colors[index] == selectedColor) ZixoAccent else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = colors[index] }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Symbol button
            OutlinedButton(
                onClick = { showSymbolPanel = !showSymbolPanel },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ZixoSurface,
                    contentColor = ZixoTextPrimary
                )
            ) {
                Icon(Icons.Filled.EmojiEmotions, null, tint = ZixoAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("3D Symbols & Stamps")
            }

            // Symbol panel
            AnimatedVisibility(visible = showSymbolPanel) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    cornerRadius = 16.dp
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SymbolCatalog.symbols.forEach { symbol ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        statusText = statusText + symbol.symbol
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = symbol.symbol, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Post button
            Button(
                onClick = {
                    if (statusText.isNotBlank()) {
                        onCreateText(statusText, selectedColor)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZixoAccent),
                enabled = statusText.isNotBlank()
            ) {
                Text(
                    "Post Status",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
