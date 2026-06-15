package com.zixo.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.theme.NeonMint
import java.util.Locale

/**
 * Section header component with Zixo brand accent styling.
 *
 * Renders a bold, uppercase label in the brand Neon Emerald Green
 * (#00E676) with letter spacing for visual hierarchy. Used as
 * category dividers throughout the settings and list screens.
 *
 * The component intentionally uses a fixed accent color rather than
 * a theme parameter because section headers are a signature design
 * element that should maintain consistent brand recognition across
 * light/dark/AMOLED theme variations.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.toUpperCase(Locale.getDefault()),
        color = NeonMint,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp,
            bottom = 8.dp,
        ),
    )
}
