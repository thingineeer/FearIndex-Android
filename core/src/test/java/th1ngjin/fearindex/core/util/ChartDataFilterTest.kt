package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import java.time.Instant
import java.time.temporal.ChronoUnit

class ChartDataFilterTest {

    // ============================
    // filter
    // ============================

    @Test
    fun `filter - 90일 필터 시 90일 이내 데이터만 반환`() {
        val now = Instant.now()
        val data = listOf(
            createFearIndex(50.0, now.minus(30, ChronoUnit.DAYS)),
            createFearIndex(60.0, now.minus(60, ChronoUnit.DAYS)),
            createFearIndex(70.0, now.minus(89, ChronoUnit.DAYS)),
            createFearIndex(80.0, now.minus(100, ChronoUnit.DAYS)), // 90일 밖
            createFearIndex(90.0, now.minus(200, ChronoUnit.DAYS)), // 90일 밖
        )

        val result = ChartDataFilter.filter(data, days = 90)

        assertEquals(3, result.size)
        // 오름차순 정렬 확인
        assertTrue(result[0].timestamp <= result[1].timestamp)
        assertTrue(result[1].timestamp <= result[2].timestamp)
    }

    @Test
    fun `filter - 빈 데이터 시 빈 리스트`() {
        val result = ChartDataFilter.filter(emptyList(), days = 90)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter - 기간 내 데이터 없으면 최근 N개 폴백`() {
        val now = Instant.now()
        // 모든 데이터가 365일 이전
        val data = listOf(
            createFearIndex(50.0, now.minus(400, ChronoUnit.DAYS)),
            createFearIndex(60.0, now.minus(500, ChronoUnit.DAYS)),
            createFearIndex(70.0, now.minus(600, ChronoUnit.DAYS)),
        )

        // 90일 필터 → 기간 내 0개 → 폴백으로 최근 N개(maxSamplePoints=null → days=90, takeLast(90))
        val result = ChartDataFilter.filter(data, days = 90)

        // 3개 전부 반환 (데이터 수 < 90)
        assertEquals(3, result.size)
    }

    @Test
    fun `filter - maxSamplePoints 지정 시 샘플링`() {
        val now = Instant.now()
        val data = (0 until 100).map { i ->
            createFearIndex(i.toDouble(), now.minus(i.toLong(), ChronoUnit.DAYS))
        }

        val result = ChartDataFilter.filter(data, days = 365, maxSamplePoints = 10)

        assertEquals(10, result.size)
        // 마지막 데이터 포함 보장
        assertEquals(data.minByOrNull { it.timestamp }!!.timestamp, result.first().timestamp)
    }

    @Test
    fun `filter - maxSamplePoints가 데이터보다 크면 전체 반환`() {
        val now = Instant.now()
        val data = listOf(
            createFearIndex(50.0, now.minus(1, ChronoUnit.DAYS)),
            createFearIndex(60.0, now.minus(2, ChronoUnit.DAYS)),
        )

        val result = ChartDataFilter.filter(data, days = 90, maxSamplePoints = 100)

        assertEquals(2, result.size)
    }

    // ============================
    // findClosest
    // ============================

    @Test
    fun `findClosest - 정확히 매칭되는 날짜`() {
        val target = Instant.parse("2026-01-15T00:00:00Z")
        val data = listOf(
            createFearIndex(40.0, Instant.parse("2026-01-10T00:00:00Z")),
            createFearIndex(50.0, target),
            createFearIndex(60.0, Instant.parse("2026-01-20T00:00:00Z")),
        )

        val result = ChartDataFilter.findClosest(target, data)

        assertNotNull(result)
        assertEquals(50.0, result!!.score, 0.01)
    }

    @Test
    fun `findClosest - 가장 가까운 데이터 포인트 반환`() {
        val target = Instant.parse("2026-01-16T00:00:00Z")
        val data = listOf(
            createFearIndex(40.0, Instant.parse("2026-01-10T00:00:00Z")),
            createFearIndex(50.0, Instant.parse("2026-01-15T00:00:00Z")), // 1일 차이
            createFearIndex(60.0, Instant.parse("2026-01-20T00:00:00Z")), // 4일 차이
        )

        val result = ChartDataFilter.findClosest(target, data)

        assertNotNull(result)
        assertEquals(50.0, result!!.score, 0.01) // 1일 차이로 더 가까움
    }

    @Test
    fun `findClosest - 빈 데이터 시 null`() {
        val result = ChartDataFilter.findClosest(Instant.now(), emptyList())
        assertNull(result)
    }

    // ============================
    // findClosestBinary
    // ============================

    @Test
    fun `findClosestBinary - 정렬된 데이터에서 정확한 인덱스 검색`() {
        val data = (0 until 100).map { i ->
            createFearIndex(i.toDouble(), Instant.parse("2026-01-01T00:00:00Z").plus(i.toLong(), ChronoUnit.DAYS))
        }
        val target = Instant.parse("2026-01-01T00:00:00Z").plus(50, ChronoUnit.DAYS)

        val result = ChartDataFilter.findClosestBinary(target, data)

        assertNotNull(result)
        assertEquals(50.0, result!!.score, 0.01)
    }

    @Test
    fun `findClosestBinary - 빈 데이터 시 null`() {
        assertNull(ChartDataFilter.findClosestBinary(Instant.now(), emptyList()))
    }

    @Test
    fun `findClosestBinary - 데이터 1개`() {
        val single = createFearIndex(42.0, Instant.now())

        val result = ChartDataFilter.findClosestBinary(Instant.now(), listOf(single))

        assertNotNull(result)
        assertEquals(42.0, result!!.score, 0.01)
    }

    @Test
    fun `findClosestBinary - 경계 밖 날짜 시 가장 가까운 반환`() {
        val data = listOf(
            createFearIndex(10.0, Instant.parse("2026-03-01T00:00:00Z")),
            createFearIndex(20.0, Instant.parse("2026-03-15T00:00:00Z")),
            createFearIndex(30.0, Instant.parse("2026-03-30T00:00:00Z")),
        )
        // 데이터 범위 이전
        val beforeResult = ChartDataFilter.findClosestBinary(Instant.parse("2026-01-01T00:00:00Z"), data)
        assertEquals(10.0, beforeResult!!.score, 0.01)

        // 데이터 범위 이후
        val afterResult = ChartDataFilter.findClosestBinary(Instant.parse("2026-12-31T00:00:00Z"), data)
        assertEquals(30.0, afterResult!!.score, 0.01)
    }

    // ============================
    // nearestIndex
    // ============================

    @Test
    fun `nearestIndex - 왼쪽 끝 터치`() {
        val result = ChartDataFilter.nearestIndex(touchX = 0f, width = 1000f, size = 100)
        assertEquals(0, result)
    }

    @Test
    fun `nearestIndex - 오른쪽 끝 터치`() {
        val result = ChartDataFilter.nearestIndex(touchX = 1000f, width = 1000f, size = 100)
        assertEquals(99, result)
    }

    @Test
    fun `nearestIndex - 중간 터치`() {
        val result = ChartDataFilter.nearestIndex(touchX = 500f, width = 1000f, size = 100)
        // 500/1000 = 0.5 → base = (0.5 * 99).toInt() = 49
        assertTrue(result in 49..50)
    }

    @Test
    fun `nearestIndex - size 1이면 항상 0`() {
        assertEquals(0, ChartDataFilter.nearestIndex(touchX = 500f, width = 1000f, size = 1))
    }

    @Test
    fun `nearestIndex - touchX 음수면 0으로 클램프`() {
        val result = ChartDataFilter.nearestIndex(touchX = -100f, width = 1000f, size = 10)
        assertEquals(0, result)
    }

    @Test
    fun `nearestIndex - touchX가 width 초과면 마지막 인덱스`() {
        val result = ChartDataFilter.nearestIndex(touchX = 1500f, width = 1000f, size = 10)
        assertEquals(9, result)
    }

    // ============================
    // count
    // ============================

    @Test
    fun `count - 기간 내 데이터 개수 계산`() {
        val now = Instant.now()
        val data = listOf(
            createFearIndex(50.0, now.minus(10, ChronoUnit.DAYS)),
            createFearIndex(60.0, now.minus(50, ChronoUnit.DAYS)),
            createFearIndex(70.0, now.minus(100, ChronoUnit.DAYS)),
        )

        assertEquals(2, ChartDataFilter.count(data, days = 90))
        assertEquals(3, ChartDataFilter.count(data, days = 365))
        assertEquals(1, ChartDataFilter.count(data, days = 30))
    }

    private fun createFearIndex(score: Double, timestamp: Instant) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = timestamp,
    )
}
