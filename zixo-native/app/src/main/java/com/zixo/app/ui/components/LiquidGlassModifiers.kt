package com.zixo.app.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.EmeraldGreen
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ──────────────────────────────────────────────
// Private Glass Color Palette
// ──────────────────────────────────────────────

/** Semi-transparent dark surface used as the glass container background. */
private val GlassSurfaceColor = Color(0x1A1A2A32)

/** High-gloss semi-transparent white used for frosted glass borders. */
private val GlassBorderColor = Color(0x33FFFFFF)

/** Bright accent green used for active/selected states. */
private val AccentGreen = Color(0xFF00E676)

/** Semi-transparent dark for unchecked/inactive track backgrounds. */
private val GlassTrackInactive = Color(0x33FFFFFF)

/** Deep teal background for segmented picker panels. */
private val SegmentPanelBackground = Color(0x1A1A2A32)

/** Green focus indicator for text fields. */
private val GlassFocusGreen = Color(0xFF00E676)

/** Unfocused border color for glass text fields. */
private val GlassUnfocusedBorder = Color(0x33FFFFFF)

// ──────────────────────────────────────────────
// Blob Colors for ZixoGlassBackground
// ──────────────────────────────────────────────

/** Mint/Emerald green tint for the first animated blob. */
private val BlobMintTint = Color(0xFF05C46B)

/** Deep teal/cyan tint for the second animated blob. */
private val BlobTealTint = Color(0xFF00838F)

/** Subtle purple/indigo tint for the third animated blob. */
private val BlobPurpleTint = Color(0xFF5C6BC0)

// ──────────────────────────────────────────────
// Glass Modifier Extensions
// ──────────────────────────────────────────────

/**
 * Applies a premium "Liquid Glass" container effect to any composable.
 *
 * This modifier creates a frosted-glass appearance by combining:
 * - A semi-transparent dark surface background
 * - A 24.dp blur effect (on API 31+; gracefully degrades to background-only on older devices)
 * - A thin 1.dp frosted white border
 * - 20.dp rounded corners
 * - Clipping to ensure the blur stays within bounds
 *
 * Use this for large content containers such as settings panels or main content sections.
 *
 * @return A [Modifier] with the liquid glass container styling applied.
 */
fun Modifier.liquidGlassContainer(): Modifier = liquidGlass(
    cornerRadius = 20.dp,
    blurRadius = 24.dp
)

/**
 * Applies a "Liquid Glass" card effect optimized for smaller card items.
 *
 * Compared to [liquidGlassContainer], this uses tighter corners (16.dp) and a
 * lighter blur (16.dp) to feel more compact while maintaining the glass aesthetic.
 *
 * Use this for list items, setting rows, or compact content cards.
 *
 * @return A [Modifier] with the liquid glass card styling applied.
 */
fun Modifier.liquidGlassCard(): Modifier = liquidGlass(
    cornerRadius = 16.dp,
    blurRadius = 16.dp
)

/**
 * Applies a "Liquid Glass" navigation item effect for bottom bars and tab strips.
 *
 * This is the most compact variant with 12.dp corners and 12.dp blur,
 * designed to look clean at small sizes such as navigation bars and tab indicators.
 *
 * @return A [Modifier] with the liquid glass navigation item styling applied.
 */
fun Modifier.liquidGlassNavItem(): Modifier = liquidGlass(
    cornerRadius = 12.dp,
    blurRadius = 12.dp
)

/**
 * Internal core implementation that all public liquid-glass modifiers delegate to.
 *
 * @param cornerRadius The corner radius for both clipping and border shape.
 * @param blurRadius   The blur radius applied when running on API 31+.
 */
private fun Modifier.liquidGlass(
    cornerRadius: Dp,
    blurRadius: Dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(radius = blurRadius)
        } else {
            Modifier
        }
    )
    .background(GlassSurfaceColor)
    .border(
        width = 1.dp,
        color = GlassBorderColor,
        shape = RoundedCornerShape(cornerRadius)
    )

// ──────────────────────────────────────────────
// ZixoGlassBackground – Animated Full-Screen Background
// ──────────────────────────────────────────────

/**
 * Full-screen animated background composable that renders the signature
 * Zixo "Liquid Glass" atmosphere.
 *
 * The background consists of:
 * - A deep dark slate/teal linear gradient base (#0B1519 → #111E24)
 * - Three slowly-moving, soft-blurred radial gradient "blobs" underneath:
 *     1. **Mint/Emerald** (300dp) – drifts across the top-left region
 *     2. **Teal/Cyan** (250dp) – drifts across the bottom-right region
 *     3. **Purple/Indigo** (200dp) – drifts through the center region
 *
 * Each blob follows a gentle Lissajous oscillation pattern using
 * [infiniteRepeatable] animations with 15–25 second durations,
 * creating a living, breathing dark-mode atmosphere.
 *
 * Example usage:
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize()) {
 *     ZixoGlassBackground()
 *     // Your content here…
 * }
 * ```
 */
@Composable
fun ZixoGlassBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_bg_transition")

    // ── Blob 1: Mint / Emerald – slow top-left drift ──
    val blob1X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_x"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_y"
    )

    // ── Blob 2: Teal / Cyan – slow bottom-right drift ──
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_x"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_y"
    )

    // ── Blob 3: Purple / Indigo – slow center drift ──
    val blob3X by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3_x"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3_y"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = maxWidth
        val heightPx = maxHeight

        // Base gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd),
                        start = Offset.Zero,
                        end = Offset(
                            x = widthPx.value * 2f,
                            y = heightPx.value * 2f
                        )
                    )
                )
        )

        // ── Blob 1: Mint / Emerald ──
        val blob1OffsetX = (blob1X * 0.35f + 0.02f) * widthPx
        val blob1OffsetY = (blob1Y * 0.30f + 0.02f) * heightPx
        Box(
            modifier = Modifier
                .offset(x = blob1OffsetX, y = blob1OffsetY)
                .size(300.dp)
                .blur(80.dp)
        ) {
            BlobCanvas(color = BlobMintTint, radiusFraction = 1f)
        }

        // ── Blob 2: Teal / Cyan ──
        val blob2OffsetX = (blob2X * 0.35f + 0.55f) * widthPx
        val blob2OffsetY = (blob2Y * 0.30f + 0.50f) * heightPx
        Box(
            modifier = Modifier
                .offset(x = blob2OffsetX, y = blob2OffsetY)
                .size(250.dp)
                .blur(80.dp)
        ) {
            BlobCanvas(color = BlobTealTint, radiusFraction = 1f)
        }

        // ── Blob 3: Purple / Indigo ──
        val blob3OffsetX = (blob3X * 0.40f + 0.25f) * widthPx
        val blob3OffsetY = (blob3Y * 0.35f + 0.25f) * heightPx
        Box(
            modifier = Modifier
                .offset(x = blob3OffsetX, y = blob3OffsetY)
                .size(200.dp)
                .blur(80.dp)
        ) {
            BlobCanvas(color = BlobPurpleTint, radiusFraction = 1f)
        }
    }
}

/**
 * Draws a soft radial gradient blob with the given [color] at full opacity
 * in the center, fading to transparent at the edges.
 *
 * @param color       The tint color for the center of the blob.
 * @param radiusFraction Fraction of the canvas size to use as the gradient radius (0..1).
 */
@Composable
private fun BlobCanvas(color: Color, radiusFraction: Float = 1f) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasSize = size.minDimension
        val radius = canvasSize * radiusFraction * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.45f),
                    color.copy(alpha = 0.20f),
                    color.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

// ──────────────────────────────────────────────
// diagonalMeshGradient – Profile Header Gradient
// ──────────────────────────────────────────────

/**
 * Applies a diagonal linear mesh gradient running from
 * [EmeraldGreen] (#05C46B) to [BackgroundGradientStart] (#0B1519)
 * as a background brush.
 *
 * This is designed for user profile header cards where a bold
 * directional gradient creates depth and visual hierarchy.
 *
 * Example usage:
 * ```kotlin
 * Box(modifier = Modifier.diagonalMeshGradient().padding(16.dp)) {
 *     // Profile header content
 * }
 * ```
 *
 * @return A [Modifier] with the diagonal gradient background applied.
 */
fun Modifier.diagonalMeshGradient(): Modifier = this.background(
    brush = Brush.linearGradient(
        colors = listOf(
            EmeraldGreen,
            EmeraldGreen.copy(alpha = 0.7f),
            DarkPetrolCharcoal,
            BackgroundGradientStart
        ),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
)

// ──────────────────────────────────────────────
// GlassSwitch – Custom Switch with Glass Aesthetic
// ──────────────────────────────────────────────

/**
 * A Material 3 [Switch] wrapped with the Zixo glass aesthetic.
 *
 * - **Checked track:** NeonMint (#00E676)
 * - **Unchecked track:** Semi-transparent dark surface
 * - **Thumb:** White
 *
 * @param checked          Whether the switch is currently on.
 * @param onCheckedChange  Callback invoked when the user toggles the switch.
 * @param modifier         Optional modifier for the switch.
 * @param enabled          Whether the switch is interactive.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = NeonMint,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = DarkPetrolCharcoal.copy(alpha = 0.6f),
            uncheckedThumbColor = TextSecondary,
            uncheckedBorderColor = GlassBorderColor,
            disabledCheckedTrackColor = NeonMint.copy(alpha = 0.4f),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = DarkPetrolCharcoal.copy(alpha = 0.3f),
            disabledUncheckedThumbColor = TextSecondary.copy(alpha = 0.4f)
        )
    )
}

// ──────────────────────────────────────────────
// GlassOutlinedTextField – Text Field with Glass Aesthetic
// ──────────────────────────────────────────────

/**
 * A styled [OutlinedTextField] with the Zixo glass aesthetic.
 *
 * Features:
 * - Transparent background with a frosted glass border
 * - White primary text, secondary hint text
 * - Green accent border on focus
 * - Optional character count via [maxLength]
 *
 * @param value           The current text value.
 * @param onValueChange   Callback invoked when the text changes.
 * @param label           Optional composable label displayed above the field.
 * @param placeholder     Optional composable placeholder shown when the field is empty.
 * @param modifier        Optional modifier for the text field.
 * @param readOnly        Whether the field is read-only.
 * @param maxLength       Optional maximum character count. Characters beyond this are truncated.
 * @param keyboardOptions Keyboard configuration options (e.g. input type).
 * @param enabled         Whether the field is interactive.
 * @param singleLine      Whether the field is constrained to a single line.
 * @param visualTransformation Visual transformation applied to the input (e.g. password mask).
 * @param trailingIcon    Optional composable trailing icon.
 */
@Composable
fun GlassOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    maxLength: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)

    OutlinedTextField(
        value = value,
        onValueChange = { newText ->
            if (maxLength != null) {
                if (newText.length <= maxLength) onValueChange(newText)
            } else {
                onValueChange(newText)
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            // Text
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextSecondary.copy(alpha = 0.5f),

            // Container / Background
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,

            // Cursor
            cursorColor = AccentGreen,

            // Border
            focusedBorderColor = GlassFocusGreen,
            unfocusedBorderColor = GlassUnfocusedBorder,
            disabledBorderColor = GlassBorderColor.copy(alpha = 0.3f),

            // Label
            focusedLabelColor = AccentGreen,
            unfocusedLabelColor = TextSecondary,
            disabledLabelColor = TextSecondary.copy(alpha = 0.4f),

            // Placeholder
            focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.4f),
            disabledPlaceholderColor = TextSecondary.copy(alpha = 0.3f)
        )
    )
}

// ──────────────────────────────────────────────
// GlassSegmentedPicker – Segmented Control with Glass Aesthetic
// ──────────────────────────────────────────────

/**
 * A segmented control picker with the Zixo glass aesthetic.
 *
 * Displays a list of [options] inside a horizontal glass panel.
 * The selected option is highlighted with a bright accent green (#00E676)
 * background and white text, while unselected options appear transparent
 * with secondary text color. The selection indicator animates smoothly
 * using [animateColorAsState] for smooth color transitions.
 *
 * @param options           The list of string labels for each segment.
 * @param selectedIndex     The index of the currently selected option.
 * @param onOptionSelected  Callback invoked with the new index when the user selects an option.
 * @param modifier          Optional modifier for the picker.
 */
@Composable
fun GlassSegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(SegmentPanelBackground)
            .border(
                width = 1.dp,
                color = GlassBorderColor,
                shape = outerShape
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val segmentShape = RoundedCornerShape(10.dp)

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) AccentGreen else Color.Transparent,
                animationSpec = tween(durationMillis = 250),
                label = "segment_bg_$index"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                animationSpec = tween(durationMillis = 250),
                label = "segment_text_$index"
            )
            val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(segmentShape)
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onOptionSelected(index) }
                    )
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = fontWeight
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// GlassSlider – Slider with Glass Track
// ──────────────────────────────────────────────

/**
 * A Material 3 [Slider] styled with the Zixo glass aesthetic.
 *
 * The active track uses the NeonMint accent green, while the inactive
 * track appears as a semi-transparent white. The thumb is white.
 *
 * This is designed for continuous value inputs such as the font size scale
 * modifier in the accessibility settings.
 *
 * @param value         The current slider value.
 * @param onValueChange Callback invoked as the user drags the slider.
 * @param modifier      Optional modifier for the slider.
 * @param enabled       Whether the slider is interactive.
 * @param valueRange    The range of valid values.
 * @param steps         Number of discrete steps (0 for continuous).
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = NeonMint,
            inactiveTrackColor = GlassTrackInactive,
            activeTickColor = NeonMint.copy(alpha = 0.6f),
            inactiveTickColor = GlassTrackInactive.copy(alpha = 0.4f),
            disabledThumbColor = Color.White.copy(alpha = 0.4f),
            disabledActiveTrackColor = NeonMint.copy(alpha = 0.4f),
            disabledInactiveTrackColor = GlassTrackInactive.copy(alpha = 0.2f)
        )
    )
}

// ──────────────────────────────────────────────
// GlassCheckBox – Checkbox with Glass Aesthetic
// ──────────────────────────────────────────────

/**
 * A Material 3 [Checkbox] styled with the Zixo glass aesthetic.
 *
 * - **Checked:** NeonMint green accent with white checkmark
 * - **Unchecked:** Glass-style semi-transparent container
 *
 * @param checked          Whether the checkbox is currently checked.
 * @param onCheckedChange  Callback invoked when the user toggles the checkbox.
 * @param modifier         Optional modifier for the checkbox.
 * @param enabled          Whether the checkbox is interactive.
 */
@Composable
fun GlassCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = NeonMint,
            uncheckedColor = GlassBorderColor,
            checkmarkColor = Color.White,
            disabledCheckedColor = NeonMint.copy(alpha = 0.4f),
            disabledUncheckedColor = GlassBorderColor.copy(alpha = 0.3f),
            disabledIndeterminateColor = NeonMint.copy(alpha = 0.4f)
        )
    )
}
