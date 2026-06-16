package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.domain.entity.AggregateStats
import th1ngjin.fearindex.domain.entity.EventMatch
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiConfidence
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.entity.KospiSignalScore
import th1ngjin.fearindex.domain.entity.KospiSnapshotType
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.entity.OptionalReturns
import th1ngjin.fearindex.domain.entity.Similarity
import th1ngjin.fearindex.domain.entity.SimilarEventsResult
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import java.time.Instant
import kotlin.math.sin

internal object ScreenshotFixtures {
    private val now: Instant = Instant.parse("2026-06-11T06:00:00Z")

    fun marketCurrent(): FearIndex = FearIndex(
        score = 27.0,
        rating = FearIndex.Rating.FEAR,
        timestamp = now,
        previousClose = 27.0,
        previous1Week = 54.0,
        previous1Month = 67.0,
        previous1Year = 64.0,
    )

    fun kospiCurrent(): KospiFearIndex = KospiFearIndex(
        fearIndex = FearIndex(
            score = 24.0,
            rating = FearIndex.Rating.EXTREME_FEAR,
            timestamp = now,
            previousClose = 28.0,
            previous1Week = 38.0,
            previous1Month = 58.0,
            previous1Year = 61.0,
        ),
        snapshotType = KospiSnapshotType.INTRADAY,
        isFinal = false,
        isStale = false,
        dataDate = "2026-06-11",
        generatedAt = now,
        confidence = KospiConfidence.HIGH,
        signals = listOf(
            KospiSignalScore("kospiMomentum", 22.0, 0.18, KospiCluster.PRICE),
            KospiSignalScore("kospiBreadth", 27.0, 0.16, KospiCluster.BREADTH),
            KospiSignalScore("usdKrwStress", 31.0, 0.12, KospiCluster.SENTIMENT),
        ),
        missingSignals = emptyList(),
        clusterScores = mapOf(
            KospiCluster.PRICE to 22.0,
            KospiCluster.BREADTH to 27.0,
            KospiCluster.SENTIMENT to 31.0,
            KospiCluster.CREDIT to 35.0,
        ),
        clusterDivergence = 4.2,
    )

    fun cryptoCurrent(): FearIndex = FearIndex(
        score = 39.0,
        rating = FearIndex.Rating.FEAR,
        timestamp = now,
        previousClose = 43.0,
        previous1Week = 48.0,
        previous1Month = 55.0,
        previous1Year = 72.0,
    )

    fun history(days: Int, center: Double, amplitude: Double): List<FearIndex> {
        val count = days.coerceAtLeast(1)
        return (count - 1 downTo 0).map { daysAgo ->
            val index = count - 1 - daysAgo
            val score = (center + sin(index / 5.5) * amplitude - daysAgo * 0.08)
                .coerceIn(5.0, 95.0)
            FearIndex(
                score = score,
                rating = FearIndex.Rating.from(score),
                timestamp = now.minusSeconds(daysAgo * 86_400L),
            )
        }
    }

    fun marketIndices(): List<MarketIndex> = listOf(
        MarketIndex("^KS11", "KOSPI", price = 2864.12, change = -41.2, changePercent = -1.42),
        MarketIndex("^KQ11", "KOSDAQ", price = 802.45, change = -6.9, changePercent = -0.86),
        MarketIndex("^IXIC", "Nasdaq", price = 17896.21, change = 131.4, changePercent = 0.74),
        MarketIndex("^GSPC", "S&P 500", price = 5421.03, change = 16.7, changePercent = 0.31),
        MarketIndex("^DJI", "Dow Jones", price = 38974.12, change = -70.1, changePercent = -0.18),
    )

    fun stuckCounter(status: StuckStatus = StuckStatus.NONE): StuckCounterResult =
        StuckCounterResult(
            stuckCount = 112,
            safeCount = 71,
            totalResponded = 183,
            stuckPercentage = 61.2,
            safePercentage = 38.8,
            myStatus = status,
        )

    fun similarEvents(indexType: FearIndexType): SimilarEventsResult = SimilarEventsResult(
        indexType = indexType,
        currentScore = when (indexType) {
            FearIndexType.MARKET -> 27
            FearIndexType.KOSPI -> 24
            FearIndexType.CRYPTO -> 39
        },
        updatedAt = now,
        matches = listOf(
            EventMatch(
                eventId = "covid",
                score = 25,
                distance = 2,
                similarity = Similarity.CLOSE,
                date = "2020-03-23",
                titleKey = "event_covid",
                descriptionKey = "event_covid",
                returnAfter = OptionalReturns(
                    oneMonth = 12.7,
                    threeMonth = 34.1,
                    sixMonth = 48.3,
                    oneYear = 76.4,
                ),
                isOngoing = false,
                isPinned = true,
            ),
        ),
        aggregateStats = AggregateStats(
            score = 25,
            sampleCount = 18,
            avgReturn = HistoricalReturns(
                oneMonth = 4.8,
                threeMonth = 9.6,
                sixMonth = 15.4,
                oneYear = 21.8,
            ),
            maxDrawdown = HistoricalReturns(
                oneMonth = -2.1,
                threeMonth = -4.7,
                sixMonth = -8.2,
                oneYear = -11.3,
            ),
            bestReturn = HistoricalReturns(
                oneMonth = 14.2,
                threeMonth = 28.5,
                sixMonth = 45.0,
                oneYear = 72.6,
            ),
        ),
    )
}
