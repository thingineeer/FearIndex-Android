package com.thingineer.fearindex.presentation.theme

import androidx.compose.ui.graphics.Color

// iOS FearScoreColor 포팅 — 5단계 공포지수 컬러
val ExtremeFear = Color(0xFFE53935)      // 0-24
val Fear = Color(0xFFFF9800)              // 25-44
val Neutral = Color(0xFFFFC107)           // 45-55
val Greed = Color(0xFF4DB6AC)             // 56-75
val ExtremeGreed = Color(0xFF4CAF50)      // 76-100

// UI Colors
val Primary = Color(0xFF1976D2)
val OnPrimary = Color(0xFFFFFFFF)
val Background = Color(0xFFFAFAFA)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFF5F5F5)
val Error = Color(0xFFE53935)
val Positive = Color(0xFF4CAF50)
val Negative = Color(0xFFE53935)

// Dark
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE0E0E0)

fun fearScoreColor(score: Int): Color = when {
    score <= 24 -> ExtremeFear
    score <= 44 -> Fear
    score <= 55 -> Neutral
    score <= 75 -> Greed
    else -> ExtremeGreed
}
