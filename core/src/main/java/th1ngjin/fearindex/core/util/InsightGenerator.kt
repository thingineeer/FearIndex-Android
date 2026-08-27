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

        // Structural fields only — UI reads via InsightText helper (i18n).
        // These strings are fallback/debug only and never reach production UI.
        val basis = indexTypeLabel(indexType)
        val title = if (score <= 25) "BuySignal" else "OverheatWarning"
        val summary = if (score <= 25) {
            "$basis historical buys in this range had strong average returns"
        } else {
            "Extreme greed zone — correction likely per historical data"
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

        // Structural fields only — UI uses InsightText helper for i18n.
        val basis = indexTypeLabel(indexType)
        val summary = if (matched.isNotEmpty()) {
            "$basis — ${matched.size} past periods similar to $score"
        } else {
            "$basis — limited data near $score"
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
            "If you invested in Bitcoin?"
        } else {
            "If you invested in S&P 500?"
        }

        return MarketInsight(
            id = "return_chart_${indexType.name.lowercase()}_$score",
            type = InsightType.RETURN_CHART,
            indexType = indexType,
            title = returnChartTitle,
            summary = "What if you invested in this range?",
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

        // Fallback summary — UI uses InsightText helper for i18n.
        val summary = when {
            score <= 24 -> "Extreme fear drawdown"
            score <= 44 -> "Fear drawdown"
            score <= 54 -> "Neutral drawdown"
            score <= 74 -> "Greed drawdown"
            else -> "Extreme greed drawdown"
        }

        return MarketInsight(
            id = "drawdown_${indexType.name.lowercase()}_$score",
            type = InsightType.DRAWDOWN_TOLERANCE,
            indexType = indexType,
            title = "Max Drawdown (${indexTypeLabel(indexType)})",
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

        // Fallback trend text — UI uses InsightText helper for i18n.
        val trendText = when (velocity.trend) {
            th1ngjin.fearindex.domain.entity.VelocityTrend.CRASH_ACCELERATING -> "Crash accelerating"
            th1ngjin.fearindex.domain.entity.VelocityTrend.CRASH_DECELERATING -> "Crash slowing"
            th1ngjin.fearindex.domain.entity.VelocityTrend.STABLE -> "Stable"
            th1ngjin.fearindex.domain.entity.VelocityTrend.RALLY_ACCELERATING -> "Rally accelerating"
            th1ngjin.fearindex.domain.entity.VelocityTrend.RALLY_DECELERATING -> "Rally slowing"
        }

        val inflectionNote = if (velocity.isInflectionPoint) " (inflection detected)" else ""

        val sparklineHistory = history.take(minOf(15, history.size))

        return MarketInsight(
            id = "velocity_${indexType.name.lowercase()}_$score",
            type = InsightType.FEAR_VELOCITY,
            indexType = indexType,
            title = "Fear Index Velocity",
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

    // Fallback nudge — UI uses InsightText helper for i18n.
    private fun nudgeMessage(score: Int): Pair<String, String> = when {
        score <= 24 -> "Nudge" to "Extreme fear — historically a good buying opportunity"
        score <= 44 -> "Nudge" to "Fear — be cautious but stay alert"
        score <= 54 -> "Nudge" to "Neutral — market is searching for direction"
        score <= 74 -> "Nudge" to "Greed — consider taking profits"
        else -> "Nudge" to "Extreme greed — manage risk"
    }
}

/**
 * 지수 종류별 기준 종목 라벨 — iOS와 완전 동일.
 *  - MARKET → "S&P 500"
 *  - KOSPI → "KOSPI"
 *  - CRYPTO → "Bitcoin"
 *
 * InsightGenerator와 InsightDetailSheet 양쪽에서 공유.
 */
fun indexTypeLabel(indexType: FearIndexType): String = when (indexType) {
    FearIndexType.MARKET -> "S&P 500"
    FearIndexType.KOSPI -> "KOSPI"
    FearIndexType.CRYPTO -> "Bitcoin"
}
