package com.zixo.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zixo.app.domain.model.ThemeMode

// ════════════════════════════════════════════════════════════════
// ZIXO DARK — Custom Slate / Dark Emerald Matrix
// ════════════════════════════════════════════════════════════════

private val ZixoDarkColorScheme = darkColorScheme(
    primary = NeonMint,
    onPrimary = Color(0xFF003A1F),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = Color(0xFF7EFBB2),
    secondary = TextSecondary,
    onSecondary = Color(0xFF1C352A),
    secondaryContainer = Color(0xFF334B40),
    onSecondaryContainer = Color(0xFFB1CCC0),
    tertiary = Color(0xFF6BC4DB),
    onTertiary = Color(0xFF003641),
    tertiaryContainer = Color(0xFF004E5D),
    onTertiaryContainer = Color(0xFFB4E9FC),
    error = ErrorDark,
    onError = onErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = BackgroundGradientStart,
    onBackground = TextPrimary,
    surface = BackgroundGradientEnd,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    scrim = ScrimDark,
)

// ════════════════════════════════════════════════════════════════
// ZIXO AMOLED — Pure Black (#000000) Power-Saving Variant
//
// Designed for OLED/AMOLED displays where pure black pixels
// are turned off, providing maximum power savings.
// All surface tokens collapse to #000000 or near-black values.
// ════════════════════════════════════════════════════════════════

private val ZixoAmoledColorScheme = ZixoDarkColorScheme.copy(
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = AmoledBlack,
    onSurface = TextPrimary,
    surfaceDim = AmoledBlack,
    surfaceBright = Color(0xFF1A1A1A),
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledBlack,
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF111111),
    surfaceContainerHighest = DarkPetrolCharcoal,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF1A1A1A),
    scrim = AmoledBlack,
)

// ════════════════════════════════════════════════════════════════
// ZIXO LIGHT — Full Material 3 Light Palette
// ════════════════════════════════════════════════════════════════

private val ZixoLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = ErrorLight,
    onError = onErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4946),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    scrim = ScrimLight,
)

// ════════════════════════════════════════════════════════════════
// ZIXO THEME — Composable Entry Point
// ════════════════════════════════════════════════════════════════

/**
 * Root theme composable for the entire Zixo application.
 *
 * Supports three theme modes via [ThemeMode] (imported from `domain.model`):
 *
 * | Mode              | Behaviour                                                      |
 * |-------------------|----------------------------------------------------------------|
 * | [ThemeMode.DARK]  | Custom slate/dark emerald matrix — the default Zixo look       |
 * | [ThemeMode.AMOLED]| Pure black (#000000) backgrounds for OLED power saving         |
 * | [ThemeMode.SYSTEM]| Follows the Android system dark/light setting                  |
 *
 * When [dynamicColor] is `true` **and** the device runs API 31+,
 * Material You dynamic colours from the user's wallpaper replace
 * the brand palette.  The AMOLED mode always overrides dynamic colour
 * since pure-black backgrounds are critical for OLED power saving.
 *
 * Typography is provided by [ZixoTypography] (defined in `Typography.kt`).
 *
 * @param themeMode   DARK → custom slate/emerald matrix,
 *                    AMOLED → pure-black power-saving variant,
 *                    SYSTEM → follows Android system setting.
 * @param dynamicColor When true & API 31+, uses Material You
 *                    dynamic colours from the user's wallpaper.
 *                    Defaults to `false` so the Zixo brand
 *                    palette is always shown unless the user
 *                    explicitly enables Material You.
 */
@Composable
fun ZixoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK   -> true
        ThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        // Android 12+ dynamic colour takes priority when enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode != ThemeMode.AMOLED -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // AMOLED overrides everything (even dynamic) — pure black matters
        themeMode == ThemeMode.AMOLED -> ZixoAmoledColorScheme
        // Standard dark palette
        useDarkTheme -> ZixoDarkColorScheme
        // Light palette
        else -> ZixoLightColorScheme
    }

    // ── Edge-to-edge: transparent status & navigation bars ──────
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZixoTypography,
        content = content
    )
}
