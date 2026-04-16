package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * `insights/similarEvents_{market,crypto}` 문서를 매핑한 결과.
 * iOS `SimilarEventsResult`와 1:1 대응.
 */
data class SimilarEventsResult(
    val indexType: FearIndexType,
    val currentScore: Int,
    val updatedAt: Instant,
    val matches: List<EventMatch>,
    val aggregateStats: AggregateStats?,
) {
    companion object {
        val EMPTY = SimilarEventsResult(
            indexType = FearIndexType.MARKET,
            currentScore = 0,
            updatedAt = Instant.EPOCH,
            matches = emptyList(),
            aggregateStats = null,
        )
    }
}

data class EventMatch(
    val eventId: String,
    val score: Int,
    val distance: Int,
    val similarity: Similarity,
    val date: String,
    val titleKey: String,
    val descriptionKey: String,
    val returnAfter: OptionalReturns,
    val isOngoing: Boolean,
)

enum class Similarity(val raw: String) {
    VERY("very"),
    CLOSE("close"),
    MODERATE("moderate"),
    FAR("far");

    companion object {
        fun fromString(value: String): Similarity = entries.firstOrNull { it.raw == value } ?: FAR
    }
}

data class OptionalReturns(
    val oneMonth: Double?,
    val threeMonth: Double?,
    val sixMonth: Double?,
    val oneYear: Double?,
)

data class AggregateStats(
    val score: Int,
    val sampleCount: Int,
    val avgReturn: HistoricalReturns,
)
