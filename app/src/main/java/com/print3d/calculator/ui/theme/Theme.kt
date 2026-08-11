package com.print3d.calculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.print3d.calculator.domain.model.AppThemeMode

/** Semantic colors Material3 has no slot for. Access via [AppColors]. */
data class ExtraColors(
    val success: Color,
    val warning: Color,
    val border: Color,
    val isDark: Boolean
)

private val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(success = Success, warning = Warning, border = LightOutline, isDark = false)
}

/** App-wide semantic color accessors: AppColors.success / warning / border. */
object AppColors {
    val success: Color
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current.success
    val warning: Color
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current.warning
    val border: Color
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current.border
}

/**
 * The single premium card treatment for the whole app — use these two accessors on every
 * card-like Surface so the look stays identical everywhere.
 *
 * Light: no border, an ultra-soft shadow lifts the white surface off the off-white background.
 * Dark: shadows are invisible, so a hairline border defines the edge instead.
 */
object CardStyle {
    val border: androidx.compose.foundation.BorderStroke?
        @Composable get() = if (LocalExtraColors.current.isDark)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    val elevation: androidx.compose.ui.unit.Dp
        @Composable get() = if (LocalExtraColors.current.isDark) Elevation.none else Elevation.raised
}

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = LightSurface,
    primaryContainer = Color_primaryContainerLight,
    onPrimaryContainer = Indigo,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    tertiary = Success
)

private val DarkColors = darkColorScheme(
    primary = IndigoDark,
    onPrimary = DarkBackground,
    primaryContainer = Color_primaryContainerDark,
    onPrimaryContainer = IndigoDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    tertiary = SuccessDark
)

// Aligned with Radius tokens so every MaterialTheme.shapes.* call is consistent app-wide.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp), // chips, small controls
    small = RoundedCornerShape(14.dp),      // inputs
    medium = RoundedCornerShape(18.dp),     // cards, tiles
    large = RoundedCornerShape(24.dp),      // dialogs, sheets
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun Print3DTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val extras = if (dark) {
        ExtraColors(success = SuccessDark, warning = WarningDark, border = DarkOutline, isDark = true)
    } else {
        ExtraColors(success = Success, warning = Warning, border = LightOutline, isDark = false)
    }
    CompositionLocalProvider(LocalExtraColors provides extras) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
