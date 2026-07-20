package com.animk.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA3E635),
    onPrimary = Color(0xFF000000),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9CA3AF),
    secondary = Color(0xFFA3E635),
    onSecondary = Color(0xFF000000),
)

@Composable
fun AnimKTheme(
    content: @Composable () -> Unit
) {
    val custom = LocalCustomColors.current
    val scheme = DarkColorScheme.copy(
        primary = custom.primary,
        onPrimary = custom.onPrimary,
        background = custom.background,
        onBackground = custom.textPrimary,
        surface = custom.surface,
        onSurface = custom.textPrimary,
    )

    ProvideAnimKColors(custom) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}
