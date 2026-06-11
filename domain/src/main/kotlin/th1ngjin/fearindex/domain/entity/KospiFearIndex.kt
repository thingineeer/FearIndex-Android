package th1ngjin.fearindex.domain.entity

import java.time.Instant

data class KospiFearIndex(
    val fearIndex: FearIndex,
    val snapshotType: KospiSnapshotType,
    val isFinal: Boolean,
    val isStale: Boolean = false,
    val dataDate: String,
    val generatedAt: Instant,
    val confidence: KospiConfidence,
    val signals: List<KospiSignalScore>,
    val missingSignals: List<String>,
    val clusterScores: Map<KospiCluster, Double?>,
    val clusterDivergence: Double,
)

enum class KospiSnapshotType(val serverName: String) {
    INTRADAY("intraday"),
    CLOSE("close"),
    UNKNOWN("unknown");

    companion object {
        fun from(value: String?): KospiSnapshotType =
            entries.firstOrNull { it.serverName == value } ?: UNKNOWN
    }
}

enum class KospiConfidence(val serverName: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    UNKNOWN("unknown");

    companion object {
        fun from(value: String?): KospiConfidence =
            entries.firstOrNull { it.serverName == value } ?: UNKNOWN
    }
}

enum class KospiCluster(val serverName: String) {
    PRICE("price"),
    BREADTH("breadth"),
    SENTIMENT("sentiment"),
    CREDIT("credit"),
    UNKNOWN("unknown");

    companion object {
        fun from(value: String?): KospiCluster =
            entries.firstOrNull { it.serverName == value } ?: UNKNOWN
    }
}

data class KospiSignalScore(
    val name: String,
    val score: Double,
    val weight: Double,
    val cluster: KospiCluster,
)
