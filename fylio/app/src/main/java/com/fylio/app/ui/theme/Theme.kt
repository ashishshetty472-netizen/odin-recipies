package com.fylio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FylioDarkScheme = darkColorScheme(
    primary = FylioElectricBlue,
    onPrimary = Color.White,
    primaryContainer = FylioSurfaceDarkElevated,
    onPrimaryContainer = FylioTextPrimary,
    secondary = FylioCyan,
    background = FylioBgDark,
    onBackground = FylioTextPrimary,
    surface = FylioSurfaceDark,
    onSurface = FylioTextPrimary,
    surfaceVariant = FylioSurfaceDarkElevated,
    onSurfaceVariant = FylioTextSecondary,
    outline = FylioBorderDark,
    error = FylioRed
)

private val FylioLightScheme = lightColorScheme(
    primary = FylioElectricBlue,
    onPrimary = Color.White,
    primaryContainer = FylioElectricBlueSoft,
    onPrimaryContainer = FylioBgDark,
    secondary = FylioCyan,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0F1420),
    surface = Color.White,
    onSurface = Color(0xFF0F1420),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF5A6679),
    outline = Color(0xFFD7DEE9),
    error = FylioRed
)

@Composable
fun FylioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FylioDarkScheme else FylioLightScheme,
        typography = FylioTypography,
        content = content
    )
}
