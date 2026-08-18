package th1ngjin.fearindex.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.defaults.DefaultReturnData
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.HistoricalSampleCounts
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.entity.ReturnHorizon
import java.time.Instant

/**
 * iOS `ScoreExplorerStatsTests` 1:1 포팅 (v1.9.4 점수별 과거 수익률 슬라이더).
 * 정확 버킷 · 슬라이더 범위 · 저표본 판정 — 보간 금지.
 */
class ScoreExplorerStatsTest {

    // ---- Score Range ----

    @Test
    fun `번들 Market 은 0 부터 97 (96 은 n=0 이지만 범위 안)`() {
        assertEquals(0..97, ScoreExplorerStats.scoreRange(DefaultReturnData.market))
    }

    @Test
    fun `번들 Crypto 는 5 부터 95`() {
        assertEquals(5..95, ScoreExplorerStats.scoreRange(DefaultReturnData.crypto))
    }

    @Test
    fun `번들 KOSPI 는 검증 버킷 8 부터 64`() {
        assertEquals(8..64, ScoreExplorerStats.scoreRange(DefaultReturnData.kospi))
    }

    @Test
    fun `표본이 하나도 없으면 null`() {
        assertNull(ScoreExplorerStats.scoreRange(table(emptyList())))
        assertNull(ScoreExplorerStats.scoreRange(table(listOf(dp(30, n = 0), dp(70, n = 0)))))
    }

    @Test
    fun `정렬되지 않은 포인트도 min max 를 정확히 산출, 단일 버킷은 a부터a`() {
        assertEquals(12..70, ScoreExplorerStats.scoreRange(table(listOf(dp(70, n = 2), dp(12, n = 1), dp(40, n = 0)))))
        assertEquals(55..55, ScoreExplorerStats.scoreRange(table(listOf(dp(55, n = 9)))))
    }

    // ---- Exact Bucket ----

    @Test
    fun `점수가 정확히 일치하는 버킷의 값을 그대로 반환`() {
        val t = table(listOf(dp(10, n = 3, counts = c(3, 3, 2, 1), mean = r(1.5, 2.5, 3.5, 4.5))))
        val p = ScoreExplorerStats.point(10, t)
        assertNotNull(p)
        requireNotNull(p)
        assertEquals(10, p.score)
        assertEquals(r(1.5, 2.5, 3.5, 4.5), p.returns)
        assertEquals(r(-1.0, -2.0, -3.0, -4.0), p.worstCase)
        assertEquals(r(5.0, 6.0, 7.0, 8.0), p.bestCase)
        assertEquals(c(3, 3, 2, 1), p.sampleCounts)
    }

    @Test
    fun `범위 안 n==0 점수는 이웃에 데이터가 있어도 null (보간하지 않음)`() {
        val t = table(listOf(dp(10, n = 3), dp(11, n = 0), dp(12, n = 4)))
        assertEquals(10..12, ScoreExplorerStats.scoreRange(t))
        assertNull(ScoreExplorerStats.point(11, t))
        assertEquals(10, ScoreExplorerStats.point(10, t)?.score)
        assertEquals(12, ScoreExplorerStats.point(12, t)?.score)
    }

    @Test
    fun `데이터 포인트 자체가 없는 점수는 null (가장 가까운 점 fallback 없음)`() {
        val t = table(listOf(dp(10, n = 3), dp(12, n = 4)))
        assertNull(ScoreExplorerStats.point(11, t))
        assertNull(ScoreExplorerStats.point(9, t))
        assertNull(ScoreExplorerStats.point(100, t))
    }

    @Test
    fun `번들 Market 96 점은 null, 95 97 은 값 있음`() {
        val m = DefaultReturnData.market
        assertNull(ScoreExplorerStats.point(96, m))
        assertEquals(3, ScoreExplorerStats.point(95, m)?.sampleCount(ReturnHorizon.ONE_MONTH))
        assertEquals(1, ScoreExplorerStats.point(97, m)?.sampleCount(ReturnHorizon.ONE_MONTH))
    }

    @Test
    fun `horizonSampleCounts 가 없으면 sampleCount 를 모든 horizon 에 사용 (KOSPI 검증 행)`() {
        val p = ScoreExplorerStats.point(32, DefaultReturnData.kospi)
        assertNotNull(p)
        requireNotNull(p)
        for (horizon in ReturnHorizon.entries) {
            assertEquals(13, p.sampleCount(horizon))
        }
    }

    @Test
    fun `번들 3개 테이블 모두 범위가 있고, Market 20 48 75 는 테이블 dp 를 보간 없이 그대로 감싼다`() {
        for (t in listOf(DefaultReturnData.market, DefaultReturnData.kospi, DefaultReturnData.crypto)) {
            assertNotNull(ScoreExplorerStats.scoreRange(t))
        }
        val m = DefaultReturnData.market
        for (score in listOf(20, 48, 75)) {
            val expected = ScoreExplorerPoint(m.dataPoints.first { it.score == score })
            assertEquals(expected, ScoreExplorerStats.point(score, m))
        }
    }

    // ---- Low Sample / Horizon Counts ----

    private val point = ScoreExplorerPoint(dp(20, n = 5, counts = c(5, 4, 1, 0)))

    @Test
    fun `저표본 기준은 5 (n 이 5 미만)`() {
        assertEquals(5, ScoreExplorerStats.LOW_SAMPLE_THRESHOLD)
    }

    @Test
    fun `horizon 별 표본 수를 그대로 반환`() {
        assertEquals(5, point.sampleCount(ReturnHorizon.ONE_MONTH))
        assertEquals(4, point.sampleCount(ReturnHorizon.THREE_MONTH))
        assertEquals(1, point.sampleCount(ReturnHorizon.SIX_MONTH))
        assertEquals(0, point.sampleCount(ReturnHorizon.ONE_YEAR))
    }

    @Test
    fun `isLowSample 은 n 이 5 미만일 때만 true (5 는 false, 0 도 true)`() {
        assertFalse(point.isLowSample(ReturnHorizon.ONE_MONTH))
        assertTrue(point.isLowSample(ReturnHorizon.THREE_MONTH))
        assertTrue(point.isLowSample(ReturnHorizon.SIX_MONTH))
        assertTrue(point.isLowSample(ReturnHorizon.ONE_YEAR))
    }

    @Test
    fun `hasSample 은 n 이 0 초과`() {
        assertTrue(point.hasSample(ReturnHorizon.ONE_MONTH))
        assertTrue(point.hasSample(ReturnHorizon.SIX_MONTH))
        assertFalse(point.hasSample(ReturnHorizon.ONE_YEAR))
    }

    @Test
    fun `ScoreExplorerPoint 는 ReturnDataPoint 를 보간 없이 감싼다`() {
        val src = dp(42, n = 7, counts = c(7, 6, 5, 4), mean = r(1.0, 2.0, 3.0, 4.0))
        val wrapped = ScoreExplorerPoint(src)
        assertEquals(
            ScoreExplorerPoint(42, r(1.0, 2.0, 3.0, 4.0), r(-1.0, -2.0, -3.0, -4.0), r(5.0, 6.0, 7.0, 8.0), c(7, 6, 5, 4)),
            wrapped,
        )
    }

    // ---- Clamp ----

    @Test
    fun `clamp 는 범위 안으로 고정`() {
        assertEquals(10, ScoreExplorerStats.clamp(3, 10..90))
        assertEquals(90, ScoreExplorerStats.clamp(120, 10..90))
        assertEquals(50, ScoreExplorerStats.clamp(50, 10..90))
        assertEquals(10, ScoreExplorerStats.clamp(10, 10..90))
        assertEquals(90, ScoreExplorerStats.clamp(90, 10..90))
    }

    // ---- helpers ----

    private fun r(m1: Double, m3: Double, m6: Double, y1: Double) = HistoricalReturns(m1, m3, m6, y1)

    private fun c(m1: Int, m3: Int, m6: Int, y1: Int) = HistoricalSampleCounts(m1, m3, m6, y1)

    private fun dp(
        score: Int,
        n: Int,
        counts: HistoricalSampleCounts? = null,
        mean: HistoricalReturns = r(1.0, 2.0, 3.0, 4.0),
    ): ReturnDataPoint = if (counts == null) {
        ReturnDataPoint(score, mean, r(-1.0, -2.0, -3.0, -4.0), r(5.0, 6.0, 7.0, 8.0), n)
    } else {
        ReturnDataPoint(score, mean, r(-1.0, -2.0, -3.0, -4.0), r(5.0, 6.0, 7.0, 8.0), n, counts)
    }

    private fun table(points: List<ReturnDataPoint>): ReturnDataTable =
        ReturnDataTable(2, Instant.ofEpochSecond(1_700_000_000), points, emptyList())
}
