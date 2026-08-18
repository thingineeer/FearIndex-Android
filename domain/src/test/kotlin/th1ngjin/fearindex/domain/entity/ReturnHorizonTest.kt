package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

/**
 * iOS `ReturnHorizon` / `HistoricalSampleCounts` / `ReturnDataTable.sourceRange` 1:1 (v1.9.4).
 */
class ReturnHorizonTest {

    private val returns = HistoricalReturns(1.1, 3.3, 6.6, 12.2)
    private val counts = HistoricalSampleCounts(21, 18, 12, 4)

    @Test
    fun `ReturnHorizon 은 HistoricalReturns 의 해당 필드를 읽는다`() {
        assertEquals(1.1, ReturnHorizon.ONE_MONTH.value(returns), 0.0)
        assertEquals(3.3, ReturnHorizon.THREE_MONTH.value(returns), 0.0)
        assertEquals(6.6, ReturnHorizon.SIX_MONTH.value(returns), 0.0)
        assertEquals(12.2, ReturnHorizon.ONE_YEAR.value(returns), 0.0)
    }

    @Test
    fun `ReturnHorizon 은 HistoricalSampleCounts 의 해당 필드를 읽는다`() {
        assertEquals(21, ReturnHorizon.ONE_MONTH.count(counts))
        assertEquals(18, ReturnHorizon.THREE_MONTH.count(counts))
        assertEquals(12, ReturnHorizon.SIX_MONTH.count(counts))
        assertEquals(4, ReturnHorizon.ONE_YEAR.count(counts))
    }

    @Test
    fun `순서와 analyticsKey 는 iOS rawValue 와 동일`() {
        assertEquals(
            listOf("oneMonth", "threeMonth", "sixMonth", "oneYear"),
            ReturnHorizon.entries.map { it.analyticsKey },
        )
    }

    @Test
    fun `HistoricalSampleCounts same 은 4개 horizon 모두 같은 값`() {
        assertEquals(HistoricalSampleCounts(7, 7, 7, 7), HistoricalSampleCounts.same(7))
    }

    @Test
    fun `ReturnDataPoint horizonSampleCounts 기본값은 sampleCount 를 모든 horizon 에 사용`() {
        val point = ReturnDataPoint(30, returns, returns, returns, sampleCount = 13)
        assertEquals(HistoricalSampleCounts.same(13), point.horizonSampleCounts)
    }

    @Test
    fun `ReturnDataPoint horizonSampleCounts 명시 시 그대로 보존`() {
        val point = ReturnDataPoint(30, returns, returns, returns, 13, counts)
        assertEquals(counts, point.horizonSampleCounts)
    }

    @Test
    fun `ReturnDataTable sourceRange 기본값은 null 이고 equals 에 포함`() {
        val base = ReturnDataTable(2, Instant.EPOCH, emptyList(), emptyList())
        assertNull(base.sourceRange)
        val range = DateRange(Instant.parse("2011-01-03T00:00:00Z"), Instant.parse("2026-04-24T00:00:00Z"))
        val ranged = base.copy(sourceRange = range)
        assertEquals(range, ranged.sourceRange)
        assert(ranged != base)
    }

    @Test
    fun `DateRange 는 start 가 end 보다 늦으면 예외`() {
        assertThrows(IllegalArgumentException::class.java) {
            DateRange(Instant.parse("2026-01-02T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"))
        }
        // start == end 허용
        DateRange(Instant.EPOCH, Instant.EPOCH)
    }
}
