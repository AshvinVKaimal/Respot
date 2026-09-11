package com.example.respotapp.ui.theme

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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.respotapp.domain.DarkModePreference
import com.example.respotapp.domain.UiSettings
import com.example.respotapp.ui.parseAccentColor

private fun onAccent(accent: Color): Color =
    if (accent.luminance() > 0.45f) RespotBlack else RespotTextPrimary

@Composable
fun RespotAppTheme(
    uiSettings: UiSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (uiSettings.darkMode) {
        DarkModePreference.SYSTEM -> systemDark
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }

    val accent = parseAccentColor(uiSettings.accentColorHex)
    val onAccent = onAccent(accent)

    val colorScheme = when {
        uiSettings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Keep wallpaper-driven surfaces, but still honor the user's accent as primary
            base.copy(
                primary = accent,
                onPrimary = onAccent,
                secondary = accent.copy(alpha = 0.85f),
                onSecondary = onAccent,
                tertiary = accent,
                onTertiary = onAccent,
                background = if (darkTheme) RespotBlack else base.background,
                surface = if (darkTheme) RespotSurface else base.surface
            )
        }
        darkTheme -> darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            tertiary = accent,
            onTertiary = onAccent,
            background = RespotBlack,
            onBackground = RespotTextPrimary,
            surface = RespotSurface,
            onSurface = RespotTextPrimary,
            surfaceVariant = RespotSurfaceSecondary,
            onSurfaceVariant = RespotTextSecondary,
            outline = RespotField,
            error = RespotDanger,
            onError = RespotTextPrimary
        )
        else -> lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            tertiary = accent,
            onTertiary = onAccent,
            background = Color(0xFFFAFAFA),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1A1A1A),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFE8E8E8),
            onSurfaceVariant = Color(0xFF6A6A6A),
            error = RespotDanger,
            onError = RespotTextPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
