package com.mursis.anycalc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.mursis.anycalc.data.AppSettings
import com.mursis.anycalc.data.Palette
import com.mursis.anycalc.data.paletteById

data class VisualStyle(
    val palette: Palette,
    val dark: Boolean,
    val corner: Int,
    val glass: Boolean,
)

val LocalVisual = staticCompositionLocalOf {
    VisualStyle(paletteById("dark_obsidian", true), true, 24, true)
}
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun AnyCalcTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = settings.themeMode != "LIGHT"
    val palette = paletteById(settings.paletteId, dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.accentSoft,
            onSecondary = palette.background,
            tertiary = palette.success,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            error = palette.danger,
            onError = palette.text,
            outline = palette.border,
            scrim = palette.background,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.accentSoft,
            onSecondary = palette.background,
            tertiary = palette.success,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            error = palette.danger,
            onError = palette.text,
            outline = palette.border,
            scrim = palette.background,
        )
    }
    val shapes = Shapes(
        small = RoundedCornerShape((settings.cornerRadius - 10).coerceAtLeast(8).dp),
        medium = RoundedCornerShape(settings.cornerRadius.dp),
        large = RoundedCornerShape((settings.cornerRadius + 4).dp),
    )
    CompositionLocalProvider(
        LocalVisual provides VisualStyle(palette, dark, settings.cornerRadius, settings.glassEffect),
        LocalReduceMotion provides settings.reduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = shapes, content = content)
    }
}
