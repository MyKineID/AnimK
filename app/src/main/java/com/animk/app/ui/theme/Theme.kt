package com.animk.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private fun createAnimKDarkColorScheme(accentColor: Color) = darkColorScheme(
    background = OLEDBlack,
    surface = DarkSurface,
    primary = accentColor,
    onPrimary = Color.Black,
    onBackground = TextOffWhite,
    onSurface = TextOffWhite,
    surfaceContainer = CardSurfaceDark
)

@Composable
fun AnimKTheme(
    accent: AppThemeAccent = AppThemeAccent.NEON_GECKO,
    content: @Composable () -> Unit
) {
    val customColors = CustomColors(
        background = OLEDBlack,
        surface = DarkSurface,
        cardSurface = CardSurfaceDark,
        primary = accent.color,
        onPrimary = Color.Black,
        textPrimary = TextOffWhite,
        textSecondary = TextMutedGray
    )

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = createAnimKDarkColorScheme(accent.color),
            typography = Typography(),
            content = content
        )
    }
}
