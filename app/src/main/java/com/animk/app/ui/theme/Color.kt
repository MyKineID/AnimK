package com.animk.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1B1B1B)
val DarkCardSurface = Color(0xFF242424)
val NeonLimePrimary = Color(0xFFA3E635)
val NeonLimeVariant = Color(0xFF84CC16)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val ScrimDark = Color(0xCC0D0D0D)

val LocalCustomColors = compositionLocalOf { AnimKColors() }

data class AnimKColors(
    val background: Color = ObsidianBackground,
    val surface: Color = DarkSurface,
    val cardSurface: Color = DarkCardSurface,
    val primary: Color = NeonLimePrimary,
    val primaryVariant: Color = NeonLimeVariant,
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
