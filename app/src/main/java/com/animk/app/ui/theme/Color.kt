package com.animk.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1B1B1B)
val DarkCardSurface = Color(0xFF242424)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val ScrimDark = Color(0xCC0D0D0D)

// Preset Theme Accent Colors
enum class AppThemeAccent(val label: String, val primaryColor: Color, val onPrimaryColor: Color) {
    NEON_LIME("Neon Lime", Color(0xFFA3E635), Color(0xFF000000)),
    NETFLIX_RED("Netflix Red", Color(0xFFE50914), Color(0xFFFFFFFF)),
    CYBER_CYAN("Cyber Cyan", Color(0xFF06B6D4), Color(0xFF000000)),
    VIBRANT_PURPLE("Vibrant Purple", Color(0xFFA855F7), Color(0xFFFFFFFF)),
    SUNSET_AMBER("Sunset Amber", Color(0xFFF97316), Color(0xFF000000))
}

val LocalCustomColors = compositionLocalOf { AnimKColors() }

data class AnimKColors(
    val background: Color = ObsidianBackground,
    val surface: Color = DarkSurface,
    val cardSurface: Color = DarkCardSurface,
    val primary: Color = Color(0xFFA3E635),
    val primaryVariant: Color = Color(0xFF84CC16),
    val onPrimary: Color = Color(0xFF000000),
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val scrim: Color = ScrimDark
)

@Composable
fun ProvideAnimKColors(colors: AnimKColors = AnimKColors(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCustomColors provides colors, content = content)
}
