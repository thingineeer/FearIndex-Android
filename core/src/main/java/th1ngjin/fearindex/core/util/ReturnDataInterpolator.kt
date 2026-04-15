package th1ngjin.fearindex.core.util

import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnEventEntry
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 공포지수 스코어 기반 수익률 선형 보간 유틸리티.
 *
 * iOS `ReturnDataInterpolator.swift`와 1:1 대응.
 * - score(0~100)에 대한 returns/worstCase/bestCase 보간
 * - score 근처 이벤트 추출 (matchingEvents)
 */
object ReturnDataInterpolator {

    /**
     * 주어진 score에 대해 가장 가까운 두 데이터 포인트 사이를 선형 보간.
     *
     * @param score 공포지수 스코어 (0~100)
     * @param dataPoints score별 수익률 데이터 배열
     * @return 보간된 수익률 결과, 데이터가 비어있으면 null
     */
    fun interpolate(
        score: Int,
        dataPoints: List<ReturnDataPoint>,
    ): InterpolatedReturns? {
        if (dataPoints.isEmpty()) return null

        val clamped = score.coerceIn(0, 100)
        val sorted = dataPoints.sortedBy { it.score }

        // 정확히 일치하는 데이터 포인트가 있으면 바로 반환
        sorted.firstOrNull { it.score == clamped }?.let { exact ->
            return InterpolatedReturns(
                returns = exact.returns,
                worstCase = exact.worstCase,
                bestCase = exact.bestCase,
                sampleCount = exact.sampleCount,
            )
        }

        val lower = sorted.lastOrNull { it.score <= clamped }
        val upper = sorted.firstOrNull { it.score >= clamped }

        // lower/upper 중 하나라도 없으면 가장 가까운 값 사용
        if (lower == null || upper == null) {
            val nearest = sorted.first()
            return InterpolatedReturns(
                returns = nearest.returns,
                worstCase = nearest.worstCase,
                bestCase = nearest.bestCase,
                sampleCount = nearest.sampleCount,
            )
        }

        // lower == upper인 경우 (동일 score)
        if (lower.score == upper.score) {
            return InterpolatedReturns(
                returns = lower.returns,
                worstCase = lower.worstCase,
                bestCase = lower.bestCase,
                sampleCount = lower.sampleCount,
            )
        }

        // t in [0, 1] 보장 (부동소수점 오차 방지)
        val rawT = (clamped - lower.score).toDouble() / (upper.score - lower.score).toDouble()
        val t = rawT.coerceIn(0.0, 1.0)

        return InterpolatedReturns(
            returns = lerp(lower.returns, upper.returns, t),
            worstCase = lerp(lower.worstCase, upper.worstCase, t),
            bestCase = lerp(lower.bestCase, upper.bestCase, t),
            sampleCount = lerpInt(lower.sampleCount, upper.sampleCount, t),
        )
    }

    /**
     * score 근처 이벤트 추출.
     *
     * @param score 기준 공포지수 스코어
     * @param events 전체 이벤트 목록
     * @param limit 최대 반환 개수 (기본 3)
     * @return score 차이가 적은 순으로 정렬된 이벤트 목록
     */
    fun matchingEvents(
        score: Int,
        events: List<ReturnEventEntry>,
        limit: Int = 3,
    ): List<ReturnEventEntry> {
        return events
            .sortedWith(
                compareBy<ReturnEventEntry> { abs(it.score - score) }
                    .thenBy { it.score }
                    .thenBy { it.id },
            )
            .take(limit)
    }

    // -- Private --

    private fun lerp(a: HistoricalReturns, b: HistoricalReturns, t: Double): HistoricalReturns {
        return HistoricalReturns(
            oneMonth = a.oneMonth + (b.oneMonth - a.oneMonth) * t,
            threeMonth = a.threeMonth + (b.threeMonth - a.threeMonth) * t,
            sixMonth = a.sixMonth + (b.sixMonth - a.sixMonth) * t,
            oneYear = a.oneYear + (b.oneYear - a.oneYear) * t,
        )
    }

    private fun lerpInt(a: Int, b: Int, t: Double): Int {
        return (a.toDouble() + (b.toDouble() - a.toDouble()) * t).roundToInt()
    }
}

/**
 * 보간된 수익률 결과.
 * iOS `InterpolatedReturns`와 1:1 대응.
 */
data class InterpolatedReturns(
    val returns: HistoricalReturns,
    val worstCase: HistoricalReturns,
    val bestCase: HistoricalReturns,
    val sampleCount: Int,
)
