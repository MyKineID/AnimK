package com.animk.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val OLEDBlack = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A1A)
val LimeAccent = Color(0xFFA3E635)
val TextOffWhite = Color(0xFFF3F4F6)
val TextMutedGray = Color(0xFF9CA3AF)
val CardSurfaceDark = Color(0xFF262626)

enum class AppThemeAccent(val displayName: String, val color: Color) {
    NEON_GECKO("Neon Gecko", Color(0xFFA3E635)),
    CRIMSON_FLAMINGO("Crimson Flamingo", Color(0xFFFF2A5F)),
    CYBER_DOLPHIN("Cyber Dolphin", Color(0xFF00E5FF)),
    ROYAL_PEACOCK("Royal Peacock", Color(0xFF7C4DFF)),
    TIGER_AMBER("Tiger Amber", Color(0xFFFFAB00)),
    GOLDEN_LION("Golden Lion", Color(0xFFFFD600)),
    FIRE_FOX("Fire Fox", Color(0xFFFF6D00)),
    EMERALD_FROG("Emerald Frog", Color(0xFF00E676)),
    AMETHYST_UNICORN("Amethyst Unicorn", Color(0xFFE040FB)),
    FALCON_GOLD("Falcon Gold", Color(0xFFFFC400)),
    OCEAN_WHALE("Ocean Whale", Color(0xFF2979FF)),
    DRAGON_JADE("Dragon Jade", Color(0xFF1DE9B6)),
    BUMBLE_BEE("Bumble Bee", Color(0xFFFFEA00)),
    OBSIDIAN_OWL("Obsidian Owl", Color(0xFFB0BEC5)),
    SHADOW_WOLF("Shadow Wolf", Color(0xFF78909C))
}

data class CustomColors(
    val background: Color = OLEDBlack,
    val surface: Color = DarkSurface,
    val cardSurface: Color = CardSurfaceDark,
    val primary: Color = LimeAccent,
    val onPrimary: Color = Color.Black,
    val textPrimary: Color = TextOffWhite,
    val textSecondary: Color = TextMutedGray,
    val textMuted: Color = TextMutedGray.copy(alpha = 0.6f)
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }
