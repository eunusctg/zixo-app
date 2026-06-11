package com.zexo.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ZexoPrimary,
    onPrimary = ZexoTextPrimary,
    primaryContainer = ZexoPrimaryDark,
    onPrimaryContainer = ZexoTextPrimary,
    secondary = ZexoSecondary,
    onSecondary = ZexoTextPrimary,
    secondaryContainer = ZexoSecondaryDark,
    tertiary = ZexoAccent,
    background = ZexoBackground,
    onBackground = ZexoTextPrimary,
    surface = ZexoSurface,
    onSurface = ZexoTextPrimary,
    surfaceVariant = ZexoSurfaceVariant,
    onSurfaceVariant = ZexoTextSecondary,
    outline = ZexoSurfaceLight,
    outlineVariant = ZexoSurfaceVariant,
    error = ZexoRed,
    onError = ZexoTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ZexoPrimary,
    onPrimary = ZexoTextPrimary,
    primaryContainer = ZexoPrimaryLight,
    onPrimaryContainer = ZexoTextPrimaryDark,
    secondary = ZexoSecondary,
    onSecondary = ZexoTextPrimary,
    secondaryContainer = ZexoSecondaryDark,
    tertiary = ZexoAccent,
    background = ZexoBackgroundLight,
    onBackground = ZexoTextPrimaryDark,
    surface = ZexoSurfaceLightTheme,
    onSurface = ZexoTextPrimaryDark,
    surfaceVariant = ZexoSurfaceLightLight,
    onSurfaceVariant = ZexoTextSecondaryDark,
    outline = ZexoSurfaceLightLight,
    outlineVariant = ZexoSurfaceLightLight,
    error = ZexoRed,
    onError = ZexoTextPrimary
)

@Composable
fun ZexoTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZexoTypography,
        content = content
    )
}
