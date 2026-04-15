package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 시장 인사이트 카드 — iOS MarketInsight 1:1 매핑.
 */
data class MarketInsight(
    val id: String,
    val type: InsightType,
    val indexType: FearIndexType,
    val title: String,
    val summary: String,
    val score: Int,
    val previousScore: Int?,
    val timestamp: Instant,
    val historicalEvents: List<HistoricalEvent>,
    val returns: HistoricalReturns?,
    val worstCase: HistoricalReturns?,
    val bestCase: HistoricalReturns?,
    val sampleCount: Int?,
    val velocity: FearVelocity?,
    val velocityHistory: List<FearIndex>,
)
