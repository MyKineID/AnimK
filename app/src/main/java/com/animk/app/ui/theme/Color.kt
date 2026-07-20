package com.animk.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCustomColors = compositionLocalOf { AnimKColors() }

data class AnimKColors(
    val background: Color = Color(0xFF0D0D0D),
    val surface: Color = Color(0xFF1A1A1A),
    val primary: Color = Color(0xFFA3E635),
    val onPrimary: Color = Color(0xFF000000),
    val textPrimary: Color = Color(0xFFF3F4F6),
    val textSecondary: Color = Color(0xFF9CA3AF),
)

@Composable
fun ProvideAnimKColors(colors: AnimKColors = AnimKColors(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCustomColors provides colors, content = content)
}
