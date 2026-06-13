package com.zixo.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zixo.app.domain.model.ThemeMode

private val ZixoDarkColorScheme = darkColorScheme(
    primary = ZixoAccent,
    onPrimary = Color.Black,
    primaryContainer = ZixoAccentDark,
    onPrimaryContainer = Color.White,
    secondary = ZixoSurfaceVariant,
    onSecondary = Color.White,
    secondaryContainer = ZixoSurface,
    onSecondaryContainer = ZixoTextPrimary,
    tertiary = ZixoAccent,
    onTertiary = Color.Black,
    background = ZixoBackground,
    onBackground = ZixoTextPrimary,
    surface = ZixoSurface,
    onSurface = ZixoTextPrimary,
    surfaceVariant = ZixoSurfaceVariant,
    onSurfaceVariant = ZixoTextSecondary,
    error = ZixoError,
    onError = Color.White,
    errorContainer = ZixoErrorBackground,
    onErrorContainer = ZixoError,
    outline = GlassBorder,
    outlineVariant = ZixoTextTertiary,
)

private val ZixoAmoledColorScheme = darkColorScheme(
    primary = ZixoAccent,
    onPrimary = Color.Black,
    primaryContainer = ZixoAccentDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF0A0A0A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF111111),
    onSecondaryContainer = ZixoTextPrimary,
    tertiary = ZixoAccent,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = ZixoTextPrimary,
    surface = Color(0xFF0A0A0A),
    onSurface = ZixoTextPrimary,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = ZixoTextSecondary,
    error = ZixoError,
    onError = Color.White,
    errorContainer = ZixoErrorBackground,
    onErrorContainer = ZixoError,
    outline = Color(0x22FFFFFF),
    outlineVariant = ZixoTextTertiary,
)

@Composable
fun ZixoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> ZixoAmoledColorScheme
        else -> ZixoDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ZixoBackground.toArgb()
            window.navigationBarColor = ZixoBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
