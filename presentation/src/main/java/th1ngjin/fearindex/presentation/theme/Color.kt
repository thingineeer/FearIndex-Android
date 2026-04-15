package th1ngjin.fearindex.presentation.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Fear Score Colors — iOS FearScoreColor 1:1 포팅
// iOS: .red, .orange, neutralAmber, .green, .mint
// =============================================================================

val ExtremeFear = Color(0xFFE53935)       // 0-24  (iOS .red)
val Fear = Color(0xFFFF9800)              // 25-44 (iOS .orange)
val Neutral = Color(0xFFB59000)           // 45-55 (iOS neutralAmber — WCAG 4.5:1)
val Greed = Color(0xFF4CAF50)             // 56-75 (iOS .green)
val ExtremeGreed = Color(0xFF26A69A)      // 76-100 (iOS .mint)

// =============================================================================
// Status Colors — iOS 시스템 컬러 매핑
// =============================================================================

val Positive = Color(0xFF34C759)          // iOS .green (system)
val Negative = Color(0xFFFF3B30)          // iOS .red (system)
val Warning = Color(0xFFFF9500)           // iOS .orange (system)
val Active = Color(0xFF007AFF)            // iOS .blue (system)

// =============================================================================
// Light Theme — iOS systemBackground / secondarySystemBackground
// =============================================================================

val LightBackground = Color(0xFFF2F2F7)          // iOS .systemGroupedBackground
val LightSurface = Color(0xFFFFFFFF)              // iOS .systemBackground (cards)
val LightSurfaceVariant = Color(0xFFF2F2F7)       // iOS .secondarySystemBackground
val LightOnBackground = Color(0xFF000000)          // iOS .label
val LightOnSurface = Color(0xFF000000)             // iOS .label
val LightOnSurfaceVariant = Color(0xFF8E8E93)      // iOS .secondaryLabel
val LightOutline = Color(0xFFC6C6C8)               // iOS .separator
val LightPrimary = Color(0xFF007AFF)               // iOS .blue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8E8ED)    // iOS .systemGray5
val LightTertiaryContainer = Color(0xFFF2F2F7)     // iOS .systemGray6

// =============================================================================
// Dark Theme — iOS dark mode 시스템 컬러
// =============================================================================

val DarkBackground = Color(0xFF121212)             // iOS 다크 배경 (scaffold)
val DarkSurface = Color(0xFF1E1E1E)                // iOS 다크 카드
val DarkSurfaceVariant = Color(0xFF2A2A2A)         // iOS 다크 보조 카드
val DarkOnBackground = Color(0xFFFFFFFF)           // iOS .label (dark)
val DarkOnSurface = Color(0xFFE5E5EA)              // iOS .label (dark)
val DarkOnSurfaceVariant = Color(0xFF8E8E93)       // iOS .secondaryLabel (dark)
val DarkOutline = Color(0xFF38383A)                // iOS .separator (dark)
val DarkPrimary = Color(0xFF0A84FF)                // iOS .blue (dark)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkSecondaryContainer = Color(0xFF2C2C2E)     // iOS .systemGray5 (dark)
val DarkTertiaryContainer = Color(0xFF1C1C1E)      // iOS .systemGray6 (dark)

// =============================================================================
// Card & Shadow
// =============================================================================

val CardShadow = Color(0x0D000000)                 // iOS black 5% opacity

// =============================================================================
// Utility
// =============================================================================

fun fearScoreColor(score: Int): Color = when {
    score <= 24 -> ExtremeFear
    score <= 44 -> Fear
    score <= 55 -> Neutral
    score <= 75 -> Greed
    else -> ExtremeGreed
}
