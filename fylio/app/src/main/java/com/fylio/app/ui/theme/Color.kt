package com.fylio.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Fylio design system — electric blue on warm neutral. */

// --- brand ---
val ElectricBlue = Color(0xFF2563EB)
val ElectricIndigo = Color(0xFF3B82F6)
val DeepElectricBlue = Color(0xFF1D4ED8)
val ElectricBlueDark = Color(0xFF60A5FA)

// --- light surfaces ---
val LightBackground = Color(0xFFF8FAFC)
val LightHeaderWarm = Color(0xFFFAF6E9)
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// --- dark surfaces ---
val DarkBackground = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF111827)
val DarkElevated = Color(0xFF1F2937)
val DarkBorder = Color(0xFF283548)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)

// --- file type accents + pastel containers ---
val PdfRed = Color(0xFFEF4444)
val PdfContainer = Color(0xFFFEE2E2)
val PptOrange = Color(0xFFF97316)
val PptContainer = Color(0xFFFFEDD5)
val DocBlue = Color(0xFF2563EB)
val DocContainer = Color(0xFFDBEAFE)
val XlsGreen = Color(0xFF10B981)
val XlsContainer = Color(0xFFD1FAE5)
val ImagePurple = Color(0xFF8B5CF6)
val ImageContainer = Color(0xFFEDE9FE)
val AudioCyan = Color(0xFF06B6D4)
val AudioContainer = Color(0xFFCFFAFE)
val VideoPink = Color(0xFFEC4899)
val VideoContainer = Color(0xFFFCE7F3)
val ZipSlate = Color(0xFF64748B)
val ZipContainer = Color(0xFFF1F5F9)
val UtilityIndigo = Color(0xFF4F46E5)
val UtilityContainer = Color(0xFFE0E7FF)

// pastel containers, dark-theme equivalents (alpha-tinted so they read on #111827)
fun pastelOnDark(base: Color) = base.copy(alpha = 0.16f)
