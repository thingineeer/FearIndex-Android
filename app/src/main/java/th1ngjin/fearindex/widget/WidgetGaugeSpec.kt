package th1ngjin.fearindex.widget

import th1ngjin.fearindex.domain.entity.FearIndexType
import kotlin.math.roundToInt

/**
 * iOS 홈 위젯과 동일한 아크 게이지 위젯 명세 (2026-08-27, 사용자 요청 — iOS 룩 통일).
 * 순수 계산만 — 그리기는 [WidgetGaugeRenderer].
 */
object WidgetGaugeSpec {
    /** 아크 전체 각도(270°)와 시작 각도(135° = 좌하단, iOS 게이지와 동일). */
    const val TOTAL_SWEEP_DEG = 270f
    const val START_ANGLE_DEG = 135f

    /** score(0~100) → 채워지는 아크 각도. 범위 밖은 클램프. */
    fun sweepAngle(score: Int): Float = TOTAL_SWEEP_DEG * score.coerceIn(0, 100) / 100f

    /** 전일 대비 변화. previousClose 없으면 null(표시 생략). */
    fun dailyChange(score: Int, previousClose: Double?): Int? =
        previousClose?.let { score - it.roundToInt() }

    /** 변화 화살표 — iOS 위젯 표기: 0 "→", 상승 "↑", 하락 "↓". */
    fun changeGlyph(delta: Int): String = when {
        delta > 0 -> "↑"
        delta < 0 -> "↓"
        else -> "→"
    }

    /** 지수 풀네임 — 작더라도 이니셜(G/K/C)보다 명확 (2026-08-27 사용자 결정, locale 불문 영문). */
    fun indexName(type: FearIndexType): String = when (type) {
        FearIndexType.MARKET -> "Global"
        FearIndexType.KOSPI -> "KOSPI"
        FearIndexType.CRYPTO -> "Crypto"
    }
}
