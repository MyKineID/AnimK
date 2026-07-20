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

// 15 Curated Animal-Inspired Sleek Theme Accents
enum class AppThemeAccent(val label: String, val animalName: String, val primaryColor: Color, val onPrimaryColor: Color) {
    NEON_GECKO("Neon Gecko", "🦎 Neon Gecko", Color(0xFFA3E635), Color(0xFF000000)),
    CRIMSON_FLAMINGO("Crimson Flamingo", "🦩 Crimson Flamingo", Color(0xFFE50914), Color(0xFFFFFFFF)),
    CYBER_DOLPHIN("Cyber Dolphin", "🐬 Cyber Dolphin", Color(0xFF06B6D4), Color(0xFF000000)),
    ROYAL_PEACOCK("Royal Peacock", "🦚 Royal Peacock", Color(0xFFA855F7), Color(0xFFFFFFFF)),
    TIGER_AMBER("Tiger Amber", "🐯 Tiger Amber", Color(0xFFF97316), Color(0xFF000000)),
    GOLDEN_LION("Golden Lion", "🦁 Golden Lion", Color(0xFFF59E0B), Color(0xFF000000)),
    FIRE_FOX("Fire Fox", "🦊 Fire Fox", Color(0xFFEF4444), Color(0xFFFFFFFF)),
    EMERALD_FROG("Emerald Frog", "🐸 Emerald Frog", Color(0xFF10B981), Color(0xFF000000)),
    AMETHYST_UNICORN("Amethyst Unicorn", "🦄 Amethyst Unicorn", Color(0xFFEC4899), Color(0xFFFFFFFF)),
    FALCON_GOLD("Falcon Gold", "🦅 Falcon Gold", Color(0xFFEAB308), Color(0xFF000000)),
    OCEAN_WHALE("Ocean Whale", "🐋 Ocean Whale", Color(0xFF3B82F6), Color(0xFFFFFFFF)),
    DRAGON_JADE("Dragon Jade", "🐲 Dragon Jade", Color(0xFF00F5D4), Color(0xFF000000)),
    BUMBLE_BEE("Bumble Bee", "🐝 Bumble Bee", Color(0xFFFFD166), Color(0xFF000000)),
    OBSIDIAN_OWL("Obsidian Owl", "🦉 Obsidian Owl", Color(0xFF8B5CF6), Color(0xFFFFFFFF)),
    SHADOW_WOLF("Shadow Wolf", "🐺 Shadow Wolf", Color(0xFF94A3B8), Color(0xFF000000))
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
