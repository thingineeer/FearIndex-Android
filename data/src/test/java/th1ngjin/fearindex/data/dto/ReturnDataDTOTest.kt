package th1ngjin.fearindex.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.domain.entity.DateRange
import th1ngjin.fearindex.domain.entity.HistoricalSampleCounts
import java.time.Instant

/**
 * ReturnDataDTO 파싱 테스트.
 *
 * Firestore `DocumentSnapshot.data`는 `Map<String, Any?>` — 숫자는 Long/Double 혼용,
 * 필드 누락/타입 불일치 가능성이 상시 존재. fromMap은 이 모든 케이스를 안전하게 다뤄야 함.
 */
class ReturnDataDTOTest {

    @Test
    fun `fromMap - 정상 문서 파싱`() {
        val raw = buildValidRaw()
        val dto = ReturnDataDTO.fromMap(raw)

        assertNotNull(dto)
        requireNotNull(dto)
        assertEquals(2, dto.version)
        assertEquals(101, dto.dataPoints.size)
        assertEquals(1, dto.historicalEvents.size)

        val event = dto.historicalEvents.first()
        assertEquals("iran-war-2025", event.id)
        assertEquals(8, event.score)
        assertEquals("insight.event.iranWar", event.descriptionKey)
        assertNotNull(event.returnAfter)
    }

    @Test
    fun `fromMap - version 누락 시 null 반환`() {
        val raw = buildValidRaw().toMutableMap().apply { remove("version") }
        assertNull(ReturnDataDTO.fromMap(raw))
    }

    @Test
    fun `fromMap - dataPoints 개수가 101 아니면 null`() {
        val shortPoints = (0..50).map { buildPointMap(it) }
        val raw = buildValidRaw().toMutableMap().apply { put("dataPoints", shortPoints) }
        assertNull(ReturnDataDTO.fromMap(raw))
    }

    @Test
    fun `fromMap - Long과 Double 숫자 타입 모두 허용`() {
        val raw = buildValidRaw().toMutableMap().apply {
            put("version", 2L) // Long
            put("updatedAt", 1_713_225_600) // Int
        }
        val dto = ReturnDataDTO.fromMap(raw)
        assertNotNull(dto)
        assertEquals(2, dto!!.version)
    }

    @Test
    fun `toDomain - Instant 변환`() {
        val dto = ReturnDataDTO.fromMap(buildValidRaw())!!
        val table = dto.toDomain()
        assertEquals(101, table.dataPoints.size)
        val event = table.historicalEvents.first()
        // 2025-06-13 00:00 UTC = 1749772800
        assertEquals(1_749_772_800L, event.date.epochSecond)
    }

    @Test
    fun `fromMap - returnAfter 누락 이벤트도 허용`() {
        val eventMap = buildEventMap().toMutableMap().apply { remove("returnAfter") }
        val raw = buildValidRaw().toMutableMap().apply {
            put("historicalEvents", listOf(eventMap))
        }
        val dto = ReturnDataDTO.fromMap(raw)!!
        assertNull(dto.historicalEvents.first().returnAfter)
    }

    // ---- v1.9.4: horizonCounts / sourceFngRange ----

    @Test
    fun `fromMap - horizonCounts 있으면 horizonSampleCounts 로 디코딩`() {
        val pointMap = buildPointMap(0).toMutableMap().apply {
            put("horizonCounts", mapOf("oneMonth" to 12L, "threeMonth" to 11, "sixMonth" to 9.0, "oneYear" to 4L))
        }
        val raw = buildValidRaw().toMutableMap().apply {
            put("dataPoints", listOf(pointMap) + (1..100).map { buildPointMap(it) })
        }
        val point = ReturnDataDTO.fromMap(raw)!!.toDomain().dataPoints.first()
        assertEquals(HistoricalSampleCounts(12, 11, 9, 4), point.horizonSampleCounts)
        assertEquals(12, point.sampleCount)
    }

    @Test
    fun `fromMap - horizonCounts 없으면 sampleCount 를 모든 horizon 에 적용`() {
        val point = ReturnDataDTO.fromMap(buildValidRaw())!!.toDomain().dataPoints.first()
        assertEquals(HistoricalSampleCounts.same(12), point.horizonSampleCounts)
    }

    @Test
    fun `fromMap - horizonCounts 필드가 일부 누락이면 무시하고 sampleCount fallback`() {
        val pointMap = buildPointMap(0).toMutableMap().apply {
            put("horizonCounts", mapOf("oneMonth" to 12, "threeMonth" to 11))
        }
        val raw = buildValidRaw().toMutableMap().apply {
            put("dataPoints", listOf(pointMap) + (1..100).map { buildPointMap(it) })
        }
        val point = ReturnDataDTO.fromMap(raw)!!.toDomain().dataPoints.first()
        assertEquals(HistoricalSampleCounts.same(12), point.horizonSampleCounts)
    }

    @Test
    fun `toDomain - sourceFngRange 있으면 sourceRange (UTC 자정)`() {
        val raw = buildValidRaw().toMutableMap().apply {
            put("sourceFngRange", mapOf("from" to "2011-01-03", "to" to "2026-08-17"))
        }
        val table = ReturnDataDTO.fromMap(raw)!!.toDomain()
        assertEquals(
            DateRange(Instant.parse("2011-01-03T00:00:00Z"), Instant.parse("2026-08-17T00:00:00Z")),
            table.sourceRange,
        )
    }

    @Test
    fun `toDomain - sourceScoreRange (kospi) 도 sourceRange 로 매핑, sourceFngRange 가 우선`() {
        val kospiRaw = buildValidRaw().toMutableMap().apply {
            put("sourceScoreRange", mapOf("from" to "2020-01-02", "to" to "2026-04-27"))
        }
        assertEquals(
            DateRange(Instant.parse("2020-01-02T00:00:00Z"), Instant.parse("2026-04-27T00:00:00Z")),
            ReturnDataDTO.fromMap(kospiRaw)!!.toDomain().sourceRange,
        )
        val bothRaw = kospiRaw.toMutableMap().apply {
            put("sourceFngRange", mapOf("from" to "2011-01-03", "to" to "2026-08-17"))
        }
        assertEquals(Instant.parse("2011-01-03T00:00:00Z"), ReturnDataDTO.fromMap(bothRaw)!!.toDomain().sourceRange?.start)
    }

    @Test
    fun `toDomain - source range 없으면 sourceRange null (레거시 문서)`() {
        assertNull(ReturnDataDTO.fromMap(buildValidRaw())!!.toDomain().sourceRange)
    }

    @Test
    fun `toDomain - source range 날짜 파싱 실패 또는 from 이 to 보다 늦으면 null`() {
        val badFormat = buildValidRaw().toMutableMap().apply {
            put("sourceFngRange", mapOf("from" to "2011/01/03", "to" to "2026-08-17"))
        }
        assertNull(ReturnDataDTO.fromMap(badFormat)!!.toDomain().sourceRange)

        val reversed = buildValidRaw().toMutableMap().apply {
            put("sourceFngRange", mapOf("from" to "2026-08-17", "to" to "2011-01-03"))
        }
        assertNull(ReturnDataDTO.fromMap(reversed)!!.toDomain().sourceRange)

        val missingTo = buildValidRaw().toMutableMap().apply {
            put("sourceFngRange", mapOf("from" to "2011-01-03"))
        }
        assertNull(ReturnDataDTO.fromMap(missingTo)!!.toDomain().sourceRange)
    }

    // ---- helpers ----

    private fun buildValidRaw(): Map<String, Any?> = mapOf(
        "version" to 2,
        "updatedAt" to 1_713_225_600.0,
        "dataPoints" to (0..100).map { buildPointMap(it) },
        "historicalEvents" to listOf(buildEventMap()),
    )

    private fun buildPointMap(score: Int): Map<String, Any?> = mapOf(
        "score" to score,
        "returns" to buildReturnsMap(0.0),
        "worstCase" to buildReturnsMap(-5.0),
        "bestCase" to buildReturnsMap(10.0),
        "sampleCount" to 12,
    )

    private fun buildReturnsMap(base: Double): Map<String, Any?> = mapOf(
        "oneMonth" to base,
        "threeMonth" to base,
        "sixMonth" to base,
        "oneYear" to base,
    )

    private fun buildEventMap(): Map<String, Any?> = mapOf(
        "id" to "iran-war-2025",
        "date" to "2025-06-13",
        "score" to 8,
        "descriptionKey" to "insight.event.iranWar",
        "returnAfter" to buildReturnsMap(5.2),
    )
}
