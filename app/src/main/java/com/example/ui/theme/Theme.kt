package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.prefs.FantasyAccentTheme

val GhostMalliShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GhostMalliTheme(
    accentTheme: FantasyAccentTheme = FantasyAccentTheme.NEON_VIOLET,
    content: @Composable () -> Unit
) {
    val primary = when (accentTheme) {
        FantasyAccentTheme.NEON_VIOLET -> NeonViolet
        FantasyAccentTheme.ARCANE_CRIMSON -> AbyssalCrimson
        FantasyAccentTheme.ASTRAL_BLUE -> AstralBlue
    }
    val secondary = when (accentTheme) {
        FantasyAccentTheme.NEON_VIOLET -> ElectricCyan
        FantasyAccentTheme.ARCANE_CRIMSON -> NeonViolet
        FantasyAccentTheme.ASTRAL_BLUE -> NeonViolet
    }
    val primaryContainer = when (accentTheme) {
        FantasyAccentTheme.NEON_VIOLET -> DeepVioletContainer
        FantasyAccentTheme.ARCANE_CRIMSON -> CrimsonContainer
        FantasyAccentTheme.ASTRAL_BLUE -> DeepCyanContainer
    }

    val scheme = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF0A0414),
        primaryContainer = primaryContainer,
        onPrimaryContainer = StarlightWhite,
        secondary = secondary,
        onSecondary = Color(0xFF03131A),
        secondaryContainer = DeepCyanContainer,
        onSecondaryContainer = StarlightWhite,
        tertiary = ArcanePurple,
        onTertiary = StarlightWhite,
        background = VoidBlack,
        onBackground = StarlightWhite,
        surface = AbyssalPurple,
        onSurface = StarlightWhite,
        surfaceVariant = ShadowSurface,
        onSurfaceVariant = LavenderText,
        surfaceContainerLowest = VoidBlack,
        surfaceContainerLow = AbyssalPurple,
        surfaceContainer = ShadowSurface,
        surfaceContainerHigh = ElevatedObsidian,
        outline = SubtleBorder,
        outlineVariant = Color(0x28A496C7),
        error = AbyssalCrimson,
        onError = StarlightWhite,
        errorContainer = CrimsonContainer,
        onErrorContainer = StarlightWhite
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = GhostMalliTypography,
        shapes = GhostMalliShapes,
        content = content
    )
}
