package com.fylio.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Plus Jakarta Sans / Inter are not bundled (no font binaries in the repo), so the
 * system sans-serif is used - same metrics family, zero APK weight. Drop TTFs into
 * res/font and point FylioFont at a FontFamily(Font(R.font.x)) to change it.
 */
private val FylioFont = FontFamily.SansSerif

private fun style(
    size: Int,
    weight: FontWeight,
    tracking: Float = 0f,
    leading: Int = 0
): TextStyle = TextStyle(
    fontFamily = FylioFont,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = tracking.sp,
    lineHeight = (if (leading > 0) leading else (size * 1.35f).toInt()).sp
)

val FylioTypography = Typography(
    displaySmall = style(30, FontWeight.Bold, -0.6f, 36),
    headlineMedium = style(26, FontWeight.Bold, -0.5f, 32),
    headlineSmall = style(22, FontWeight.SemiBold, -0.3f, 28),
    titleLarge = style(18, FontWeight.SemiBold, 0f, 24),
    titleMedium = style(15, FontWeight.SemiBold, 0f, 20),
    titleSmall = style(13, FontWeight.Medium, 0f, 18),
    bodyLarge = style(15, FontWeight.Normal, 0f, 21),
    bodyMedium = style(13, FontWeight.Normal, 0f, 18),
    bodySmall = style(12, FontWeight.Normal, 0f, 17),
    labelLarge = style(13, FontWeight.SemiBold, 0f, 17),
    labelMedium = style(11, FontWeight.Medium, 0.1f, 15),
    labelSmall = style(10, FontWeight.Medium, 0.2f, 13)
)

val FylioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
