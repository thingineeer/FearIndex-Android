package th1ngjin.fearindex.domain.defaults

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.domain.entity.DateRange
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.HistoricalSampleCounts
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.util.ScoreExplorerStats
import java.time.Instant

/**
 * 번들 기본 수익률 테이블 — 2026-08-18 집계(iOS v1.9.4 output/{market,crypto}.json) 스팟 검증.
 * 값이 어긋나면 `python3 scripts/gen-default-return-data.py` 재실행 여부부터 확인.
 */
class DefaultReturnDataTest {

    @Test
    fun `세 테이블 모두 101 포인트, 점수 0~100 순서`() {
        for (table in tables) {
            assertEquals(101, table.dataPoints.size)
            assertEquals((0..100).toList(), table.dataPoints.map { it.score })
        }
    }

    @Test
    fun `세 테이블 모두 슬라이더 범위 존재`() {
        for (table in tables) {
            assertNotNull(ScoreExplorerStats.scoreRange(table))
        }
    }

    @Test
    fun `market 20 48 75 스팟 값`() {
        assertPoint(DefaultReturnData.market, 20,
            r(1.7, 5.3, 8.2, 12.5), r(-5.3, -1.9, -7.3, -8.3), r(7.2, 10.8, 21.2, 30.8), 34, c(34, 34, 33, 32))
        assertPoint(DefaultReturnData.market, 48,
            r(0.7, 2.8, 6.8, 11.3), r(-3.8, -5.8, -4.0, -2.5), r(4.7, 8.7, 14.2, 22.1), 53, c(53, 53, 53, 49))
        assertPoint(DefaultReturnData.market, 75,
            r(0.7, 2.4, 6.2, 14.4), r(-3.8, -3.2, 1.2, -5.2), r(4.8, 11.0, 11.1, 25.1), 54, c(54, 54, 54, 54))
    }

    @Test
    fun `crypto 20 48 75 스팟 값`() {
        assertPoint(DefaultReturnData.crypto, 20,
            r(3.7, 2.9, 9.2, 43.7), r(-13.8, -26.1, -38.9, -38.8), r(35.1, 45.9, 56.1, 128.1), 72, c(72, 69, 69, 58))
        assertPoint(DefaultReturnData.crypto, 48,
            r(3.7, 22.2, 65.4, 134.8), r(-17.3, -26.0, -41.2, -40.1), r(28.1, 99.5, 216.2, 315.0), 48, c(48, 48, 47, 39))
        assertPoint(DefaultReturnData.crypto, 75,
            r(-0.6, 11.3, 74.7, 80.8), r(-15.2, -33.2, -25.9, -48.5), r(23.4, 58.3, 294.3, 313.0), 49, c(49, 49, 49, 49))
    }

    @Test
    fun `market crypto sourceRange 는 fngRange, kospi 는 null`() {
        assertEquals(DateRange(day("2011-01-03"), day("2026-08-17")), DefaultReturnData.market.sourceRange)
        assertEquals(DateRange(day("2018-02-01"), day("2026-08-18")), DefaultReturnData.crypto.sourceRange)
        assertNull(DefaultReturnData.kospi.sourceRange)
    }

    @Test
    fun `kospi 검증 행은 horizonSampleCounts 가 sampleCount 와 동일 (레거시 포맷 보존)`() {
        val p32 = DefaultReturnData.kospi.dataPoints.first { it.score == 32 }
        assertEquals(13, p32.sampleCount)
        assertEquals(HistoricalSampleCounts.same(13), p32.horizonSampleCounts)
    }

    // ---- helpers ----

    private val tables: List<ReturnDataTable>
        get() = listOf(DefaultReturnData.market, DefaultReturnData.kospi, DefaultReturnData.crypto)

    private fun assertPoint(
        table: ReturnDataTable,
        score: Int,
        returns: HistoricalReturns,
        worst: HistoricalReturns,
        best: HistoricalReturns,
        n: Int,
        counts: HistoricalSampleCounts,
    ) {
        val expected = ReturnDataPoint(score, returns, worst, best, n, counts)
        assertEquals(expected, table.dataPoints.first { it.score == score })
    }

    private fun r(m1: Double, m3: Double, m6: Double, y1: Double) = HistoricalReturns(m1, m3, m6, y1)

    private fun c(m1: Int, m3: Int, m6: Int, y1: Int) = HistoricalSampleCounts(m1, m3, m6, y1)

    private fun day(iso: String): Instant = Instant.parse("${iso}T00:00:00Z")
}
