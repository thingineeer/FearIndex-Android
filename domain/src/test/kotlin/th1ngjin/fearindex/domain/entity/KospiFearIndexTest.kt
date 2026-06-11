package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class KospiFearIndexTest {

    @Test
    fun `init - iOS KOSPI snapshot 필드를 보존한다`() {
        val generatedAt = Instant.parse("2026-06-10T06:00:00Z")
        val fearIndex = createFearIndex(score = 62.4)
        val signal = KospiSignalScore(
            name = "kospi_momentum",
            score = 70.0,
            weight = 0.2,
            cluster = KospiCluster.PRICE,
        )

        val kospi = KospiFearIndex(
            fearIndex = fearIndex,
            snapshotType = KospiSnapshotType.INTRADAY,
            isFinal = false,
            isStale = true,
            dataDate = "2026-06-10",
            generatedAt = generatedAt,
            confidence = KospiConfidence.HIGH,
            signals = listOf(signal),
            missingSignals = listOf("credit_spread"),
            clusterScores = mapOf(KospiCluster.PRICE to 70.0, KospiCluster.CREDIT to null),
            clusterDivergence = 12.5,
        )

        assertEquals(fearIndex, kospi.fearIndex)
        assertEquals(KospiSnapshotType.INTRADAY, kospi.snapshotType)
        assertFalse(kospi.isFinal)
        assertTrue(kospi.isStale)
        assertEquals("2026-06-10", kospi.dataDate)
        assertEquals(generatedAt, kospi.generatedAt)
        assertEquals(KospiConfidence.HIGH, kospi.confidence)
        assertEquals(listOf(signal), kospi.signals)
        assertEquals(listOf("credit_spread"), kospi.missingSignals)
        assertEquals(70.0, kospi.clusterScores[KospiCluster.PRICE])
        assertEquals(null, kospi.clusterScores[KospiCluster.CREDIT])
        assertEquals(12.5, kospi.clusterDivergence, 0.0)
    }

    @Test
    fun `init - isStale 기본값은 false다`() {
        val kospi = createKospiFearIndex()

        assertFalse(kospi.isStale)
    }

    @Test
    fun `from - 알 수 없는 서버 enum은 unknown으로 매핑한다`() {
        assertEquals(KospiSnapshotType.UNKNOWN, KospiSnapshotType.from("closing"))
        assertEquals(KospiConfidence.UNKNOWN, KospiConfidence.from("very-high"))
        assertEquals(KospiCluster.UNKNOWN, KospiCluster.from("liquidity"))
    }

    private fun createKospiFearIndex() = KospiFearIndex(
        fearIndex = createFearIndex(score = 48.0),
        snapshotType = KospiSnapshotType.CLOSE,
        isFinal = true,
        dataDate = "2026-06-09",
        generatedAt = Instant.parse("2026-06-09T07:00:00Z"),
        confidence = KospiConfidence.MEDIUM,
        signals = emptyList(),
        missingSignals = emptyList(),
        clusterScores = emptyMap(),
        clusterDivergence = 0.0,
    )

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.parse("2026-06-10T06:00:00Z"),
    )
}
