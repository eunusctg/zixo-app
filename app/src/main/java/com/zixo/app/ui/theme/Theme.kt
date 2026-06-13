package com.zixo.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val ZixoDarkColorScheme = darkColorScheme(
    primary = ZixoPrimary,
    onPrimary = ZixoBg,
    primaryContainer = ZixoPrimaryDark,
    onPrimaryContainer = ZixoText,
    secondary = ZixoSecondary,
    onSecondary = ZixoText,
    secondaryContainer = ZixoSurfaceLight,
    onSecondaryContainer = ZixoText,
    tertiary = ZixoAccent,
    onTertiary = ZixoBg,
    background = ZixoBg,
    onBackground = ZixoText,
    surface = ZixoSurface,
    onSurface = ZixoText,
    surfaceVariant = ZixoSurfaceLight,
    onSurfaceVariant = ZixoTextSecondary,
    error = ZixoError,
    onError = ZixoText,
    errorContainer = ZixoErrorBg,
    onErrorContainer = ZixoError,
    outline = ZixoHighlight,
    outlineVariant = ZixoSurfaceLight
)

private val ZixoAmoledColorScheme = darkColorScheme(
    primary = ZixoPrimary,
    onPrimary = ZixoAmoledBlack,
    primaryContainer = ZixoPrimaryDark,
    onPrimaryContainer = ZixoText,
    secondary = ZixoSecondary,
    onSecondary = ZixoText,
    secondaryContainer = ZixoAmoledBlack.copy(alpha = 0.1f).compositeOver(ZixoAmoledBlack),
    onSecondaryContainer = ZixoText,
    tertiary = ZixoAccent,
    onTertiary = ZixoAmoledBlack,
    background = ZixoAmoledBlack,
    onBackground = ZixoText,
    surface = ZixoAmoledBlack.copy(alpha = 0.05f).compositeOver(ZixoAmoledBlack),
    onSurface = ZixoText,
    surfaceVariant = ZixoAmoledBlack.copy(alpha = 0.12f).compositeOver(ZixoAmoledBlack),
    onSurfaceVariant = ZixoTextSecondary,
    error = ZixoError,
    onError = ZixoText,
    errorContainer = ZixoErrorBg,
    onErrorContainer = ZixoError,
    outline = ZixoAmoledBlack.copy(alpha = 0.2f).compositeOver(ZixoAmoledBlack),
    outlineVariant = ZixoAmoledBlack.copy(alpha = 0.12f).compositeOver(ZixoAmoledBlack)
)

@Composable
fun ZixoTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "amoled" -> ZixoAmoledColorScheme
        "system" -> if (isSystemInDarkTheme()) ZixoDarkColorScheme else ZixoDarkColorScheme
        else -> ZixoDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = when (themeMode) {
                "amoled" -> ZixoAmoledBlack
                else -> ZixoBg
            }
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = ZixoSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZixoTypography,
        content = content
    )
}

private fun Color.compositeOver(background: Color): Color {
    val fgAlpha = this.alpha
    if (fgAlpha == 0f) return background
    if (fgAlpha == 1f) return this
    val r = this.red * fgAlpha + background.red * (1f - fgAlpha)
    val g = this.green * fgAlpha + background.green * (1f - fgAlpha)
    val b = this.blue * fgAlpha + background.blue * (1f - fgAlpha)
    return Color(red = r, green = g, blue = b, alpha = 1f)
}
