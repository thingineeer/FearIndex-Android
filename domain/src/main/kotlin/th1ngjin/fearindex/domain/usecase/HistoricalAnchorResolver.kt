package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexDateContext
import java.time.Instant
import java.time.LocalDate

/** 비교 카드 앵커 묶음 (전일/1주전/1개월전/1년전). */
data class FearIndexHistoricalAnchors(
    val previousClose: Double,
    val previous1Week: Double,
    val previous1Month: Double,
    val previous1Year: Double?,
)

/**
 * 비교 카드 앵커를 **날짜 기반**으로 계산한다 (배열 인덱스 아님).
 *
 * 정책 (iOS `HistoricalAnchorResolver` 와 1:1 대응):
 * - history 를 indexType 타임존의 날짜(00:00)로 정규화 후 오름차순 정렬
 * - previousClose: 기준일 **이전(<)** 의 가장 최신 포인트
 * - previous1Week/Month/Year: (기준일 - 7일/1개월/1년) **이하(<=)** 의 가장 최신 포인트
 * - 못 찾으면 previousClose/1Week/1Month 는 current.score 로 fallback, previous1Year 는 null
 *
 * 윤달/휴장일로 정확한 N일 전 데이터가 없어도, 그 날짜 이하의 가장 최신 데이터를 고르므로
 * 배열 인덱스 방식의 오차(예: data[30] ≠ 1개월 전)가 사라진다.
 */
object HistoricalAnchorResolver {

    fun enrich(
        current: FearIndex,
        history: List<FearIndex>,
        context: FearIndexDateContext,
        referenceDate: Instant? = null,
    ): FearIndex {
        val anchors = resolve(current, history, context, referenceDate)
        return current.copy(
            previousClose = anchors.previousClose,
            previous1Week = anchors.previous1Week,
            previous1Month = anchors.previous1Month,
            previous1Year = anchors.previous1Year,
        )
    }

    fun resolve(
        current: FearIndex,
        history: List<FearIndex>,
        context: FearIndexDateContext,
        referenceDate: Instant? = null,
    ): FearIndexHistoricalAnchors {
        val zone = context.zoneId
        val latestDay = (referenceDate ?: current.timestamp).atZone(zone).toLocalDate()
        // (날짜, score) 로 정규화 후 날짜 오름차순 정렬
        val points = history
            .map { it.timestamp.atZone(zone).toLocalDate() to it.score }
            .sortedBy { it.first }
        val currentScore = current.score

        return FearIndexHistoricalAnchors(
            previousClose = scoreBefore(points, latestDay) ?: currentScore,
            previous1Week = scoreOnOrBefore(points, latestDay.minusDays(7)) ?: currentScore,
            previous1Month = scoreOnOrBefore(points, latestDay.minusMonths(1)) ?: currentScore,
            previous1Year = scoreOnOrBefore(points, latestDay.minusYears(1)),
        )
    }

    /** 기준일 이전(<)의 가장 최신 score. */
    private fun scoreBefore(points: List<Pair<LocalDate, Double>>, latestDay: LocalDate): Double? =
        points.lastOrNull { it.first < latestDay }?.second

    /** target 이하(<=)의 가장 최신 score. */
    private fun scoreOnOrBefore(points: List<Pair<LocalDate, Double>>, target: LocalDate): Double? =
        points.lastOrNull { it.first <= target }?.second
}
