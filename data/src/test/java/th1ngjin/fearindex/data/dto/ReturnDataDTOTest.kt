package th1ngjin.fearindex.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

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
