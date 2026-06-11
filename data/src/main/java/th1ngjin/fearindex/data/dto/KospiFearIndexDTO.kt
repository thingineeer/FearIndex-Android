package th1ngjin.fearindex.data.dto

import kotlinx.serialization.Serializable
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiConfidence
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.entity.KospiSignalScore
import th1ngjin.fearindex.domain.entity.KospiSnapshotType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Serializable
data class KospiPublicSnapshotResponse(
    val version: Int,
    val generatedAt: String,
    val latest: KospiLatestDTO? = null,
    val history: List<KospiHistoryDTO> = emptyList(),
    val chartHistory: List<KospiHistoryDTO>? = null,
    val historyCount: Int? = null,
) {
    val generatedAtInstant: Instant
        get() = parseInstant(generatedAt) ?: Instant.EPOCH

    val chartHistoryForDisplay: List<KospiHistoryDTO>
        get() = chartHistory ?: history
}

@Serializable
data class KospiLatestDTO(
    val score: Double,
    val rating: String,
    val confidence: String,
    val signals: List<KospiSignalScoreDTO> = emptyList(),
    val missingSignals: List<String> = emptyList(),
    val clusterScores: Map<String, Double?> = emptyMap(),
    val clusterDivergence: Double = 0.0,
    val updatedAt: Double,
    val dataSource: String,
    val date: String,
    val dataDate: String,
    val intScore: Int? = null,
    val snapshotType: String = "intraday",
    val isFinal: Boolean? = null,
    val stale: Boolean = false,
) {
    fun toDomain(generatedAt: Instant): KospiFearIndex {
        val resolvedSnapshotType = KospiSnapshotType.from(snapshotType)
        val timestamp = parseMilliseconds(updatedAt) ?: parseDay(dataDate) ?: generatedAt
        val displayedScore = intScore ?: score.roundToInt()
        return KospiFearIndex(
            fearIndex = FearIndex(
                score = score,
                rating = FearIndex.Rating.from(displayedScore.toDouble()),
                timestamp = timestamp,
                previousClose = score,
                previous1Week = score,
                previous1Month = score,
                previous1Year = score,
            ),
            snapshotType = resolvedSnapshotType,
            isFinal = isFinal ?: (resolvedSnapshotType == KospiSnapshotType.CLOSE),
            isStale = stale,
            dataDate = dataDate,
            generatedAt = generatedAt,
            confidence = KospiConfidence.from(confidence),
            signals = signals.map { it.toDomain() },
            missingSignals = missingSignals,
            clusterScores = clusterScores.toDomainClusterScores(),
            clusterDivergence = clusterDivergence,
        )
    }
}

@Serializable
data class KospiSignalScoreDTO(
    val name: String,
    val score: Double,
    val weight: Double,
    val cluster: String,
) {
    fun toDomain(): KospiSignalScore = KospiSignalScore(
        name = name,
        score = score,
        weight = weight,
        cluster = KospiCluster.from(cluster),
    )
}

@Serializable
data class KospiHistoryDTO(
    val date: String,
    val score: Double,
    val rating: String,
    val confidence: String,
    val clusterDivergence: Double = 0.0,
    val intScore: Int? = null,
) {
    fun toDomain(): FearIndex {
        val displayedScore = intScore ?: score.roundToInt()
        return FearIndex(
            score = score,
            rating = FearIndex.Rating.from(displayedScore.toDouble()),
            timestamp = parseDay(date) ?: Instant.EPOCH,
            previousClose = score,
            previous1Week = score,
            previous1Month = score,
            previous1Year = score,
        )
    }
}

private fun Map<String, Double?>.toDomainClusterScores(): Map<KospiCluster, Double?> =
    mapNotNull { (key, value) ->
        val cluster = KospiCluster.from(key)
        if (cluster == KospiCluster.UNKNOWN) null else cluster to value
    }.toMap()

private fun parseInstant(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()

private fun parseDay(value: String): Instant? =
    runCatching {
        LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC)
    }.getOrNull()

private fun parseMilliseconds(value: Double): Instant? =
    if (value > 0) Instant.ofEpochMilli(value.toLong()) else null
