package th1ngjin.fearindex.domain.util

import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.HistoricalSampleCounts
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.entity.ReturnHorizon

/**
 * 슬라이더로 고른 점수의 정확 버킷 통계. iOS `ScoreExplorerPoint` 1:1 (v1.9.4).
 * 값 = mean(평균) / p10(비관) / p90(낙관), horizon 별 표본 수 포함.
 */
data class ScoreExplorerPoint(
    val score: Int,
    val returns: HistoricalReturns,
    val worstCase: HistoricalReturns,
    val bestCase: HistoricalReturns,
    val sampleCounts: HistoricalSampleCounts,
) {
    /** `ReturnDataPoint` 를 그대로 감싼다 (보간 없음). */
    constructor(point: ReturnDataPoint) : this(
        score = point.score,
        returns = point.returns,
        worstCase = point.worstCase,
        bestCase = point.bestCase,
        sampleCounts = point.horizonSampleCounts,
    )

    /** horizon 별 표본 수 ("표본 N일") */
    fun sampleCount(horizon: ReturnHorizon): Int = horizon.count(sampleCounts)

    /** 표본이 1개 이상인가 (0 이면 셀 "—") */
    fun hasSample(horizon: ReturnHorizon): Boolean = sampleCount(horizon) > 0

    /** 저표본 (n < 5) — 배지 노출 조건. n == 0 도 포함. */
    fun isLowSample(horizon: ReturnHorizon): Boolean =
        sampleCount(horizon) < ScoreExplorerStats.LOW_SAMPLE_THRESHOLD
}

/**
 * 점수별 과거 수익률 슬라이더의 순수 계산. iOS `ScoreExplorerStats` 1:1.
 * - 정확 버킷만 반환한다. **보간 금지** (`ReturnDataInterpolator` 는 현재 점수 카드 전용).
 * - 슬라이더 범위 = 표본(n>0)이 있는 최소~최대 점수.
 */
object ScoreExplorerStats {

    /** 저표본 판정 기준 (n < 5 → 배지). SimilarEvents 카드의 기존 기준과 동일. */
    const val LOW_SAMPLE_THRESHOLD = 5

    /** 표본이 있는 점수의 최소..최대. 하나도 없으면 null. */
    fun scoreRange(table: ReturnDataTable): IntRange? {
        val populated = table.dataPoints.filter { it.sampleCount > 0 }.map { it.score }
        val lower = populated.minOrNull() ?: return null
        val upper = populated.maxOrNull() ?: return null
        return lower..upper
    }

    /** [score] 와 정확히 일치하는 버킷. 포인트가 없거나 표본이 0 이면 null. */
    fun point(score: Int, table: ReturnDataTable): ScoreExplorerPoint? {
        val match = table.dataPoints.firstOrNull { it.score == score } ?: return null
        if (match.sampleCount <= 0) return null
        return ScoreExplorerPoint(match)
    }

    /** 점수를 슬라이더 범위 안으로 클램프. */
    fun clamp(score: Int, range: IntRange): Int = score.coerceIn(range.first, range.last)
}
