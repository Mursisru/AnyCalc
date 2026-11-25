package com.mursis.anycalc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val LocalReduceMotion = staticCompositionLocalOf { false }

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun AnyCalcTheme(reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Platinum,
        onPrimary = BusinessBlack,
        secondary = Champagne,
        onSecondary = BusinessBlack,
        tertiary = Success,
        background = BusinessBlack,
        onBackground = PrimaryText,
        surface = Graphite,
        onSurface = PrimaryText,
        surfaceVariant = GraphiteLight,
        onSurfaceVariant = SecondaryText,
        error = Danger,
        onError = PrimaryText,
        outline = HairlineStrong,
        scrim = BusinessBlack,
    )
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
