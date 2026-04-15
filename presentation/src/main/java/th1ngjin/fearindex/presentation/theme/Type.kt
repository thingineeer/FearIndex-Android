package th1ngjin.fearindex.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// Typography — iOS .rounded design 매핑
// Android에서 SF Rounded 대체: 시스템 기본 sans-serif 사용
// 커스텀 Rounded 폰트 추가 시 FontFamily만 교체하면 됨
// =============================================================================

// TODO: Rounded 폰트 에셋 추가 시 아래 주석 해제
// private val RoundedFontFamily = FontFamily(
//     Font(R.font.sf_rounded_regular, FontWeight.Normal),
//     Font(R.font.sf_rounded_medium, FontWeight.Medium),
//     Font(R.font.sf_rounded_semibold, FontWeight.SemiBold),
//     Font(R.font.sf_rounded_bold, FontWeight.Bold),
// )

private val DefaultFontFamily = FontFamily.Default

// =============================================================================
// Material3 Typography — iOS 텍스트 스타일 매핑
// =============================================================================

val FearIndexTypography = Typography(
    // iOS .largeTitle.bold() → 34sp bold
    displayLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.25.sp,
    ),

    // iOS .title → 28sp bold
    displayMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),

    // iOS .title2 → 22sp bold
    displaySmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    // Score number — iOS .largeTitle.bold().rounded → 36sp bold
    headlineLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),

    // iOS .title3 → 20sp semibold
    headlineMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
    ),

    // iOS .headline → 17sp semibold
    headlineSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),

    // iOS .headline → 17sp semibold (alias)
    titleLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),

    // iOS .subheadline → 15sp medium
    titleMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),

    // iOS .footnote → 13sp regular
    titleSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),

    // iOS .body → 17sp regular
    bodyLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),

    // iOS .callout → 16sp regular
    bodyMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.25.sp,
    ),

    // iOS .subheadline → 15sp regular
    bodySmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),

    // iOS .caption → 12sp regular
    labelLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // iOS .caption → 12sp regular
    labelMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),

    // iOS .caption2 → 11sp regular
    labelSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp,
    ),
)
