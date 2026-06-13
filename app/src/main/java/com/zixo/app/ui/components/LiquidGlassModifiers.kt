package com.zixo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zixo.app.ui.theme.*

/**
 * Liquid Glass Modifier — frosted glass container effect
 * Applies frosted background, blur, and thin glossy border
 */
fun Modifier.liquidGlass(
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 24.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(GlassBackground)
    .blur(blurRadius)

/**
 * Liquid Glass Surface — composable wrapper with frosted glass effect
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = GlassBackground,
        border = BorderStroke(1.dp, GlassBorder),
        content = { Box(content = content) }
    )
}

/**
 * Liquid Glass Card — elevated glass card for settings/profile sections
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = GlassBackground,
        border = BorderStroke(1.dp, GlassBorder),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    )
}

/**
 * Profile Gradient Brush — diagonal mesh gradient for profile headers
 */
val ProfileGradientBrush: Brush
    @Composable get() = Brush.linearGradient(
        colors = listOf(ProfileGradientStart, ProfileGradientEnd),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
    )

/**
 * Background blob gradient for ambient animated backgrounds
 */
val AmbientBlobBrush: Brush
    @Composable get() = Brush.radialGradient(
        colors = listOf(
            Color(0x1A00E676),
            Color(0x0D05C46B),
            Color.Transparent
        )
    )
