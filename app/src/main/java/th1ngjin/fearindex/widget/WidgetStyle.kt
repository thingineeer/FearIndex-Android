package th1ngjin.fearindex.widget

import androidx.compose.ui.graphics.Color
import th1ngjin.fearindex.domain.entity.FearIndex

/**
 * 위젯 전용 색상/스타일 상수.
 *
 * presentation 모듈의 [th1ngjin.fearindex.presentation.theme.fearScoreColor] 와 값이 동일하지만,
 * Glance 는 자체 ColorProvider 를 쓰므로 Compose-UI Color 로 로컬 사본을 유지한다
 * (presentation 의존을 위젯 렌더 코드에 끌어들이지 않기 위함).
 */

// Fear score 색상 — presentation/theme/Color.kt 와 1:1 동일
private val WidgetExtremeFear = Color(0xFFE53935) // 0-24
private val WidgetFear = Color(0xFFFF9800)        // 25-44
private val WidgetNeutral = Color(0xFFB59000)     // 45-55
private val WidgetGreed = Color(0xFF4CAF50)       // 56-75
private val WidgetExtremeGreed = Color(0xFF26A69A) // 76-100

/** 데이터 로드 실패 시 중립 placeholder 배경. */
val WidgetPlaceholderColor = Color(0xFF5A5A5A)

/** 컬러 배경 위 텍스트 — 대비를 위해 흰색/근접 흰색. */
val WidgetTextColor = Color(0xFFFFFFFF)

/** iOS 위젯 룩 — 다크 카드 배경 / 전일 변화 색 */
val WidgetCardBackground = Color(0xFF1C1C1E)
val WidgetChangeUpColor = Color(0xFF4CAF50)
val WidgetChangeDownColor = Color(0xFFE53935)
val WidgetChangeFlatColor = Color(0xFF9E9E9E)
val WidgetTextColorDim = Color(0xFFF2F2F2)

/** 대시보드 위젯의 바깥 배경. */
val WidgetDashboardBackground = Color(0xFF1E1E1E)

fun widgetFearScoreColor(score: Int): Color = when {
    score <= 24 -> WidgetExtremeFear
    score <= 44 -> WidgetFear
    score <= 54 -> WidgetNeutral
    score <= 74 -> WidgetGreed
    else -> WidgetExtremeGreed
}

/** 원점수 기준 등급의 색 — 현재 지수 등급 표시는 이 오버로드 사용 (표시 점수는 반올림이어도 등급은 원점수). */
fun widgetFearScoreColor(rating: FearIndex.Rating): Color = when (rating) {
    FearIndex.Rating.EXTREME_FEAR -> WidgetExtremeFear
    FearIndex.Rating.FEAR -> WidgetFear
    FearIndex.Rating.NEUTRAL -> WidgetNeutral
    FearIndex.Rating.GREED -> WidgetGreed
    FearIndex.Rating.EXTREME_GREED -> WidgetExtremeGreed
}
