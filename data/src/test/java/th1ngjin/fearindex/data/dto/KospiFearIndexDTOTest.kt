package th1ngjin.fearindex.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiConfidence
import th1ngjin.fearindex.domain.entity.KospiSnapshotType
import java.time.Instant

class KospiFearIndexDTOTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `decode - latest snapshot을 KOSPI domain으로 변환한다`() {
        val response = json.decodeFromString<KospiPublicSnapshotResponse>(snapshotJson())

        val result = response.latest!!.toDomain(response.generatedAtInstant)

        assertEquals(44.9, result.fearIndex.score, 0.01)
        assertEquals(FearIndex.Rating.NEUTRAL, result.fearIndex.rating)
        assertEquals(Instant.ofEpochMilli(1_781_139_607_154), result.fearIndex.timestamp)
        assertEquals(KospiSnapshotType.INTRADAY, result.snapshotType)
        assertFalse(result.isFinal)
        assertFalse(result.isStale)
        assertEquals(KospiConfidence.HIGH, result.confidence)
        assertEquals(KospiCluster.PRICE, result.signals.first().cluster)
        assertEquals(44.0, result.clusterScores[KospiCluster.PRICE]!!, 0.01)
        assertEquals(null, result.clusterScores[KospiCluster.CREDIT])
    }

    @Test
    fun `decode - chartHistory가 있으면 display history로 우선 사용한다`() {
        val response = json.decodeFromString<KospiPublicSnapshotResponse>(
            snapshotJson(
                chartHistoryJson = """
                [
                  {"date":"2026-06-09","score":25.2,"rating":"fear","confidence":"high","clusterDivergence":1.0,"intScore":25},
                  {"date":"2026-06-10","score":18.4,"rating":"extreme_fear","confidence":"high","clusterDivergence":2.0,"intScore":18}
                ]
                """.trimIndent(),
            ),
        )

        val history = response.chartHistoryForDisplay.map { it.toDomain() }

        assertEquals(listOf(25.2, 18.4), history.map { it.score })
    }

    @Test
    fun `decode - unknown enum 문자열은 UNKNOWN으로 변환한다`() {
        val response = json.decodeFromString<KospiPublicSnapshotResponse>(
            snapshotJson(
                confidence = "very_high",
                snapshotType = "closing",
                signalCluster = "liquidity",
            ),
        )

        val result = response.latest!!.toDomain(response.generatedAtInstant)

        assertEquals(KospiConfidence.UNKNOWN, result.confidence)
        assertEquals(KospiSnapshotType.UNKNOWN, result.snapshotType)
        assertEquals(KospiCluster.UNKNOWN, result.signals.first().cluster)
    }

    @Test
    fun `toDomain - history intScore를 rating 기준으로 사용한다`() {
        val response = json.decodeFromString<KospiPublicSnapshotResponse>(
            snapshotJson(
                historyJson = """
                [
                  {"date":"2026-06-10","score":44.9,"rating":"fear","confidence":"high","clusterDivergence":1.0,"intScore":45}
                ]
                """.trimIndent(),
            ),
        )

        val result = response.history.first().toDomain()

        assertEquals(44.9, result.score, 0.01)
        assertEquals(FearIndex.Rating.NEUTRAL, result.rating)
        assertEquals(Instant.parse("2026-06-10T00:00:00Z"), result.timestamp)
    }

    private fun snapshotJson(
        confidence: String = "high",
        snapshotType: String = "intraday",
        signalCluster: String = "price",
        historyJson: String = defaultHistoryJson,
        chartHistoryJson: String? = null,
    ): String {
        val chartHistory = chartHistoryJson?.let { ""","chartHistory":$it""" } ?: ""
        return """
        {
          "version": 2,
          "generatedAt": "2026-06-11T01:00:06.211Z",
          "latest": {
            "score": 44.9,
            "rating": "fear",
            "confidence": "$confidence",
            "signals": [
              {"name":"kospiMomentum","score":55.5,"weight":0.18,"cluster":"$signalCluster"}
            ],
            "missingSignals": ["marketVolatility"],
            "clusterScores": {"price":44.0,"credit":null},
            "clusterDivergence": 6.7,
            "updatedAt": 1781139607154,
            "dataSource": "kis_ecos_v2",
            "date": "2026-06-11",
            "dataDate": "2026-06-11",
            "intScore": 45,
            "snapshotType": "$snapshotType",
            "isFinal": false,
            "stale": false
          },
          "history": $historyJson
          $chartHistory
        }
        """.trimIndent()
    }

    private val defaultHistoryJson = """
        [
          {"date":"2026-06-09","score":41.2,"rating":"fear","confidence":"high","clusterDivergence":2.3,"intScore":41},
          {"date":"2026-06-10","score":47.1,"rating":"neutral","confidence":"high","clusterDivergence":1.2,"intScore":47}
        ]
    """.trimIndent()
}
