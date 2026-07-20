package com.animk.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AnimKTheme(
    accent: AppThemeAccent = AppThemeAccent.NEON_LIME,
    content: @Composable () -> Unit
) {
    val customColors = AnimKColors(
        primary = accent.primaryColor,
        onPrimary = accent.onPrimaryColor
    )

    val darkScheme = darkColorScheme(
        primary = accent.primaryColor,
        onPrimary = accent.onPrimaryColor,
        background = ObsidianBackground,
        onBackground = TextPrimary,
        surface = DarkSurface,
        onSurface = TextPrimary,
        surfaceVariant = DarkCardSurface,
        onSurfaceVariant = TextSecondary,
        secondary = accent.primaryColor,
        onSecondary = accent.onPrimaryColor
    )

    ProvideAnimKColors(customColors) {
        MaterialTheme(
            colorScheme = darkScheme,
            typography = Typography,
            content = content
        )
    }
}
