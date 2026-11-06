package com.fylio.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Plus Jakarta Sans / Inter are not bundled (no font binaries in the repo), so the
 * system sans-serif is used — same metrics family, zero APK weight. Swap in
 * res/font/*.ttf and point FontFamily here to change it.
 */
private val FylioFont = FontFamily.SansSerif

val FylioTypography = Typography(
    displaySmall = TextStyle(FylioFont, FontWeight.Bold, 30.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(FylioFont, FontWeight.Bold, 26.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(FylioFont, FontWeight.SemiBold, 22.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(FylioFont, FontWeight.SemiBold, 18.sp),
    titleMedium = TextStyle(FylioFont, FontWeight.SemiBold, 15.sp),
    titleSmall = TextStyle(FylioFont, FontWeight.Medium, 13.sp),
    bodyLarge = TextStyle(FylioFont, FontWeight.Normal, 15.sp),
    bodyMedium = TextStyle(FylioFont, FontWeight.Normal, 13.sp),
    bodySmall = TextStyle(FylioFont, FontWeight.Normal, 12.sp),
    labelLarge = TextStyle(FylioFont, FontWeight.SemiBold, 13.sp),
    labelMedium = TextStyle(FylioFont, FontWeight.Medium, 11.sp),
    labelSmall = TextStyle(FylioFont, FontWeight.Medium, 10.sp, letterSpacing = 0.2.sp)
)

val FylioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
