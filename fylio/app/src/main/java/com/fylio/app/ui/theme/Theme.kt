package com.fylio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Extra surface tokens that Material 3 doesn't model: pastel card backings,
 * the warm header zone, and the segmented storage bar colours.
 */
data class FylioSurfaces(
    val card: Color,
    val cardBorder: Color,
    val elevated: Color,
    val headerWarm: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
    val pastelScale: Float
)

val LocalFylioSurfaces = staticCompositionLocalOf {
    FylioSurfaces(
        card = LightSurface,
        cardBorder = LightBorder,
        elevated = Color(0xFFF1F5F9),
        headerWarm = LightHeaderWarm,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        isDark = false,
        pastelScale = 1f
    )
}

private val LightScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = DocContainer,
    onPrimaryContainer = DeepElectricBlue,
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = PdfRed
)

private val DarkScheme = darkColorScheme(
    primary = ElectricBlueDark,
    onPrimary = Color(0xFF06122A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = PdfRed
)

@Composable
fun FylioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val surfaces = if (darkTheme) {
        FylioSurfaces(
            card = DarkSurface,
            cardBorder = DarkBorder,
            elevated = DarkElevated,
            headerWarm = DarkBackground,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            isDark = true,
            pastelScale = 0.18f
        )
    } else {
        FylioSurfaces(
            card = LightSurface,
            cardBorder = LightBorder,
            elevated = Color(0xFFF1F5F9),
            headerWarm = LightHeaderWarm,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            isDark = false,
            pastelScale = 1f
        )
    }

    CompositionLocalProvider(LocalFylioSurfaces provides surfaces) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = FylioTypography,
            shapes = FylioShapes,
            content = content
        )
    }
}

object FylioDimens {
    val cardRadius = 22.dp
    val tileRadius = 18.dp
    val screenPadding = 20.dp
    val iconTile = 56.dp
    val iconSize = 28.dp
}
