package com.zixo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

private val AccentGreen = Color(0xFF00E676)
private val CardBackground = Color(0xFF1A2A32)
private val TextPrimary = Color.White

@Composable
fun AvatarComponent(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    isOnline: Boolean = false,
    size: Dp = 48.dp,
) {
    val borderModifier = if (isOnline) {
        Modifier.border(
            width = 2.5.dp,
            color = AccentGreen,
            shape = CircleShape,
        )
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        // Main avatar circle
        Box(
            modifier = Modifier
                .size(size)
                .then(borderModifier)
                .clip(CircleShape)
                .background(CardBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar of $name",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    color = TextPrimary,
                    fontSize = (size.value / 2.5f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Online indicator dot
        if (isOnline) {
            val indicatorSize = size * 0.27f
            val onlineDotOffset = size * 0.02f
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = onlineDotOffset, y = onlineDotOffset)
                    .clip(CircleShape)
                    .background(AccentGreen)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF0B1519),
                        shape = CircleShape,
                    ),
            )
        }
    }
}
