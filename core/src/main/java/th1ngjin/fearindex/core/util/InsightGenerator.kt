package th1ngjin.fearindex.core.util

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.HistoricalEvent
import th1ngjin.fearindex.domain.entity.InsightType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import java.time.Instant

/**
 * 인사이트 카드 생성 엔진 — iOS buildMarketInsights 포팅.
 *
 * score/history/returnDataTable을 기반으로 최대 6종 인사이트 카드를 생성.
 */
object InsightGenerator {

    fun generateInsights(
        score: Int,
        indexType: FearIndexType,
        history: List<FearIndex>,
        returnDataTable: ReturnDataTable?,
    ): List<MarketInsight> {
        val now = Instant.now()
        val previousScore = history.getOrNull(1)?.roundedScore
        val insights = mutableListOf<MarketInsight>()

        // 1. buySignal — score <= 25 또는 >= 75
        buildBuySignal(score, indexType, previousScore, now, returnDataTable)
            ?.let(insights::add)

        // 2. historicalReturn — iOS parity: 항상 생성 (matched 비어도 카드 렌더)
        insights.add(
            buildHistoricalReturn(score, indexType, previousScore, now, returnDataTable),
        )

        // 3. returnChart — score <= 30 또는 >= 70
        buildReturnChart(score, indexType, previousScore, now, returnDataTable)
            ?.let(insights::add)

        // 4. drawdownTolerance — 항상
        insights.add(
            buildDrawdownTolerance(score, indexType, previousScore, now, returnDataTable),
        )

        // 5. nudge — 항상
        insights.add(buildNudge(score, indexType, previousScore, now))

        // 6. fearVelocity — history >= 8일
        buildFearVelocity(score, indexType, previousScore, now, history)
            ?.let(insights::add)

        return insights
    }

    private fun buildBuySignal(
        score: Int,
        indexType: FearIndexType,
        previousScore: Int?,
        now: Instant,
        table: ReturnDataTable?,
    ): MarketInsight? {
        // iOS parity: 극단 구간(≤25 또는 ≥75)에서만 생성.
        if (score in 26..74) return null

        val interpolated = table?.let {
            ReturnDataInterpolator.interpolate(score, it.dataPoints)
        }

        val basis = indexTypeLabel(indexType)
        val title = if (score <= 25) "매수 기회 시그널" else "과열 경고"
        val summary = if (score <= 25) {
            "$basis 과거 이 구간 매수 시 평균 수익률이 높았습니다"
        } else {
            "극단적 탐욕 구간입니다. 과거 데이터에 따르면 조정 가능성이 높습니다."
        }

        return MarketInsight(
            id = "buy_signal_${indexType.name.lowercase()}_$score",
            type = InsightType.BUY_SIGNAL,
            indexType = indexType,
            title = title,
            summary = summary,
            score = score,
            previousScore = previousScore,
            timestamp = now,
            historicalEvents = emptyList(),
            returns = interpolated?.returns,
            worstCase = interpolated?.worstCase,
            bestCase = interpolated?.bestCase,
            sampleCount = interpolated?.sampleCount,
            velocity = null,
            velocityHistory = emptyList(),
        )
    }

    /**
     * iOS parity: 점수와 무관하게 **항상** 생성.
     * matched 이벤트가 없어도 빈 리스트로 카드 렌더 (Detail Sheet에서 안내 문구 표시).
     * iOS v1.7.8 문구: "현재 X점과 비슷했던 과거 시점"
     */
    private fun buildHistoricalReturn(
        score: Int,
        indexType: FearIndexType,
        previousScore: Int?,
        now: Instant,
        table: ReturnDataTable?,
    ): MarketInsight {
        val events = table?.historicalEvents.orEmpty()
        val matched = ReturnDataInterpolator.matchingEvents(score, events, limit = 3)

        val historicalEvents = matched.map { entry ->
            HistoricalEvent(
                date = entry.date.toString().take(10),
                score = entry.score,
                description = entry.descriptionKey,
                returnAfter1M = entry.returnAfter?.oneMonth,
                returnAfter3M = entry.returnAfter?.threeMonth,
                returnAfter6M = entry.returnAfter?.sixMonth,
                returnAfter1Y = entry.returnAfter?.oneYear,
            )
        }

        val basis = indexTypeLabel(indexType)
        val summary = if (matched.isNotEmpty()) {
            "$basis 기준 — 현재 $score 점과 비슷했던 과거 ${matched.size}개 시점"
        } else {
            "$basis 기준 — 현재 $score 점 구간의 과거 참고 데이터가 제한적입니다"
        }

        // v1.7.9 v2: DetailSheet 상단 "현재 N점에서 매수 시" 통계 카드 표시용으로
        // returnData 보간값을 returns/worstCase/bestCase에 채움.
        val interpolated = table?.let {
            ReturnDataInterpolator.interpolate(score, it.dataPoints)
        }

        return MarketInsight(
            id = "historical_return_${indexType.name.lowercase()}_$score",
            type = InsightType.HISTORICAL_RETURN,
            indexType = indexType,
            title = "그때 매수했다면?",
            summary = summary,
            score = score,
            previousScore = previousScore,
            timestamp = now,
            historicalEvents = historicalEvents,
            returns = interpolated?.returns,
            worstCase = interpolated?.worstCase,
            bestCase = interpolated?.bestCase,
            sampleCount = interpolated?.sampleCount,
            velocity = null,
            velocityHistory = emptyList(),
        )
    }

    private fun buildReturnChart(
        score: Int,
        indexType: FearIndexType,
        previousScore: Int?,
        now: Instant,
        table: ReturnDataTable?,
    ): MarketInsight? {
        // iOS parity: 극단 근처(≤30 또는 ≥70)에서만 생성.
        if (score in 31..69) return null

        val interpolated = table?.let {
            ReturnDataInterpolator.interpolate(score, it.dataPoints)
        }

        val returnChartTitle = if (indexType == FearIndexType.CRYPTO) {
            "Bitcoin에 투자했다면?"
        } else {
            "S&P 500에 투자했다면?"
        }

        return MarketInsight(
            id = "return_chart_${indexType.name.lowercase()}_$score",
            type = InsightType.RETURN_CHART,
            indexType = indexType,
            title = returnChartTitle,
            summary = "이 구간에서 투자했다면 예상 수익률은 어떨까요?",
            score = score,
            previousScore = previousScore,
            timestamp = now,
            historicalEvents = emptyList(),
            returns = interpolated?.returns,
            worstCase = interpolated?.worstCase,
            bestCase = interpolated?.bestCase,
            sampleCount = interpolated?.sampleCount,
            velocity = null,
            velocityHistory = emptyList(),
        )
    }

    private fun buildDrawdownTolerance(
        score: Int,
        indexType: FearIndexType,
        previousScore: Int?,
        now: Instant,
        table: ReturnDataTable?,
    ): MarketInsight {
        val interpolated = table?.let {
            ReturnDataInterpolator.interpolate(score, it.dataPoints)
        }

        val summary = when {
            score <= 24 -> "극단적 공포 구간의 최대 낙폭과 1년 수익률을 확인하세요."
            score <= 44 -> "공포 구간에서의 리스크와 기대 수익을 비교합니다."
            score <= 55 -> "중립 구간의 변동성과 예상 수익률입니다."
            score <= 75 -> "탐욕 구간에서의 하락 리스크를 점검하세요."
            else -> "극단적 탐욕 구간은 큰 조정이 올 수 있습니다."
        }

        return MarketInsight(
            id = "drawdown_${indexType.name.lowercase()}_$score",
            type = InsightType.DRAWDOWN_TOLERANCE,
            indexType = indexType,
            title = "최대 낙폭 분석 (${indexTypeLabel(indexType)})",
            summary = summary,
            score = score,
            previousScore = previousScore,
            timestamp = now,
            historicalEvents = emptyList(),
            returns = interpolated?.returns,
            worstCase = interpolated?.worstCase,
            bestCase = interpolated?.bestCase,
            sampleCount = interpolated?.sampleCount,
            velocity = null,
            velocityHistory = emptyList(),
        )
    }

    private fun buildNudge(
        score: Int,
        indexType: FearIndexType,
        previousScore: Int?,
        now: Instant,
    ): MarketInsight {
        val (title, summary) = nudgeMessage(score)

        return MarketInsight(
            id = "nudge_${indexType.name.lowercase()}_$score",
            type = InsightType.NUDGE,
            indexType = indexType,
            title = title,
            summary = summary,
            score = score,
            previousScore = previousScore,
            timestamp = now,
            historicalEvents = emptyList(),
            returns = null,
            worstCase = null,
            bestCase = null,
            sampleCount = null,
            velocity = null,
            velocityHistory = emptyList(),
        )
    }

    private fun buildFearVelocity(
        score: Int,
        indexType: FearIndexType,
        previousScore: Int?,
        now: Instant,
        history: List<FearIndex>,
    ): MarketInsight? {
        val velocity = FearVelocityCalculator.calculate(history) ?: return null

        val trendText = when (velocity.trend) {
            th1ngjin.fearindex.domain.entity.VelocityTrend.CRASH_ACCELERATING -> "공포가 가속되고 있습니다"
            th1ngjin.fearindex.domain.entity.VelocityTrend.CRASH_DECELERATING -> "공포 속도가 둔화되고 있습니다"
            th1ngjin.fearindex.domain.entity.VelocityTrend.STABLE -> "시장이 안정적입니다"
            th1ngjin.fearindex.domain.entity.VelocityTrend.RALLY_ACCELERATING -> "탐욕이 가속되고 있습니다"
            th1ngjin.fearindex.domain.entity.VelocityTrend.RALLY_DECELERATING -> "탐욕 속도가 둔화되고 있습니다"
        }

        val inflectionNote = if (velocity.isInflectionPoint) " (변곡점 감지)" else ""

        // 스파크라인용 최근 15일 히스토리
        val sparklineHistory = history.take(minOf(15, history.size))

        return MarketInsight(
            id = "velocity_${indexType.name.lowercase()}_$score",
            type = InsightType.FEAR_VELOCITY,
            indexType = indexType,
            title = "공포지수 변화 속도",
            summary = "$trendText$inflectionNote",
            score = score,
            previousScore = previousScore,
            timestamp = now,
            historicalEvents = emptyList(),
            returns = null,
            worstCase = null,
            bestCase = null,
            sampleCount = null,
            velocity = velocity,
            velocityHistory = sparklineHistory,
        )
    }

    private fun nudgeMessage(score: Int): Pair<String, String> = when {
        score <= 24 -> "심리 조언" to
            "극단적 공포 구간 — 역사적으로 좋은 매수 기회가 많았습니다"
        score <= 44 -> "심리 조언" to
            "공포 구간 — 신중하되 기회를 놓치지 마세요"
        score <= 55 -> "심리 조언" to
            "중립 구간 — 시장이 방향을 찾고 있습니다"
        score <= 75 -> "심리 조언" to
            "탐욕 구간 — 수익 실현을 고려해보세요"
        else -> "심리 조언" to
            "극단적 탐욕 — 과열 신호, 리스크 관리가 중요합니다"
    }
}

/**
 * 지수 종류별 기준 종목 라벨 — iOS와 완전 동일.
 *  - MARKET → "S&P 500"
 *  - CRYPTO → "Bitcoin"
 *
 * InsightGenerator와 InsightDetailSheet 양쪽에서 공유.
 */
fun indexTypeLabel(indexType: FearIndexType): String = when (indexType) {
    FearIndexType.MARKET -> "S&P 500"
    FearIndexType.CRYPTO -> "Bitcoin"
}
