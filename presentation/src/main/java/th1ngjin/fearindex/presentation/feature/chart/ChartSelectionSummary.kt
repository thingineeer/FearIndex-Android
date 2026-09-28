package th1ngjin.fearindex.presentation.feature.chart

import th1ngjin.fearindex.domain.entity.FearIndex
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 히스토리 차트에서 선택한 포인트를 차트 **밖** 헤더에 표시하기 위한 요약.
 * iOS `FearHistoryChartView.selectedValueHeader` 대칭 — 차트 안엔 세로선·선택 점만 남긴다.
 * 점수는 반올림 표시, 등급은 원점수 기준(엔티티 rating).
 */
data class ChartSelectionSummary(
    val score: Int,
    val rating: FearIndex.Rating,
    val dateText: String,
) {
    companion object {
        private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")
            .withZone(ZoneId.of("America/New_York"))

        fun of(point: FearIndex): ChartSelectionSummary = ChartSelectionSummary(
            score = point.score.roundToInt(),
            rating = point.rating,
            dateText = dateFormatter.format(point.timestamp),
        )
    }
}
