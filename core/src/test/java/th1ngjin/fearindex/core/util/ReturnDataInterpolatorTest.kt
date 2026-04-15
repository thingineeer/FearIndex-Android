package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnEventEntry
import java.time.Instant

class ReturnDataInterpolatorTest {

    // ============================
    // interpolate
    // ============================

    @Test
    fun `interpolate - 정확히 일치하는 데이터 포인트`() {
        val dataPoints = listOf(
            createDataPoint(score = 20, oneMonth = 5.0),
            createDataPoint(score = 50, oneMonth = 10.0),
            createDataPoint(score = 80, oneMonth = 15.0),
        )

        val result = ReturnDataInterpolator.interpolate(50, dataPoints)

        assertNotNull(result)
        assertEquals(10.0, result!!.returns.oneMonth, 0.01)
    }

    @Test
    fun `interpolate - 두 데이터 포인트 사이 선형 보간`() {
        val dataPoints = listOf(
            createDataPoint(score = 20, oneMonth = 10.0),
            createDataPoint(score = 80, oneMonth = 20.0),
        )

        // score=50은 20과 80 사이 → t = (50-20)/(80-20) = 0.5
        // 보간: 10.0 + (20.0 - 10.0) * 0.5 = 15.0
        val result = ReturnDataInterpolator.interpolate(50, dataPoints)

        assertNotNull(result)
        assertEquals(15.0, result!!.returns.oneMonth, 0.01)
    }

    @Test
    fun `interpolate - 빈 데이터 시 null`() {
        val result = ReturnDataInterpolator.interpolate(50, emptyList())
        assertNull(result)
    }

    @Test
    fun `interpolate - 데이터 1개일 때 그 값 반환`() {
        val dataPoints = listOf(createDataPoint(score = 30, oneMonth = 7.5))

        val result = ReturnDataInterpolator.interpolate(50, dataPoints)

        assertNotNull(result)
        assertEquals(7.5, result!!.returns.oneMonth, 0.01)
    }

    @Test
    fun `interpolate - score가 범위 밖(0 미만)이면 0으로 클램프`() {
        val dataPoints = listOf(
            createDataPoint(score = 0, oneMonth = 5.0),
            createDataPoint(score = 100, oneMonth = 25.0),
        )

        val result = ReturnDataInterpolator.interpolate(-10, dataPoints)

        assertNotNull(result)
        // clamped to 0, 정확히 score=0에 매칭
        assertEquals(5.0, result!!.returns.oneMonth, 0.01)
    }

    @Test
    fun `interpolate - score가 범위 밖(100 초과)이면 100으로 클램프`() {
        val dataPoints = listOf(
            createDataPoint(score = 0, oneMonth = 5.0),
            createDataPoint(score = 100, oneMonth = 25.0),
        )

        val result = ReturnDataInterpolator.interpolate(120, dataPoints)

        assertNotNull(result)
        // clamped to 100, 정확히 score=100에 매칭
        assertEquals(25.0, result!!.returns.oneMonth, 0.01)
    }

    @Test
    fun `interpolate - worstCase와 bestCase도 보간`() {
        val dp1 = ReturnDataPoint(
            score = 20,
            returns = HistoricalReturns(10.0, 20.0, 30.0, 40.0),
            worstCase = HistoricalReturns(-5.0, -10.0, -15.0, -20.0),
            bestCase = HistoricalReturns(25.0, 50.0, 75.0, 100.0),
            sampleCount = 100,
        )
        val dp2 = ReturnDataPoint(
            score = 80,
            returns = HistoricalReturns(20.0, 40.0, 60.0, 80.0),
            worstCase = HistoricalReturns(-10.0, -20.0, -30.0, -40.0),
            bestCase = HistoricalReturns(50.0, 100.0, 150.0, 200.0),
            sampleCount = 200,
        )

        // t = (50-20)/(80-20) = 0.5
        val result = ReturnDataInterpolator.interpolate(50, listOf(dp1, dp2))!!

        assertEquals(15.0, result.returns.oneMonth, 0.01)
        assertEquals(-7.5, result.worstCase.oneMonth, 0.01)
        assertEquals(37.5, result.bestCase.oneMonth, 0.01)
        assertEquals(150, result.sampleCount)
    }

    // ============================
    // matchingEvents
    // ============================

    @Test
    fun `matchingEvents - score 근처 3개 이벤트 추출`() {
        val events = listOf(
            createEvent(id = "a", score = 10),
            createEvent(id = "b", score = 45),
            createEvent(id = "c", score = 50),
            createEvent(id = "d", score = 55),
            createEvent(id = "e", score = 90),
        )

        val result = ReturnDataInterpolator.matchingEvents(score = 50, events = events)

        assertEquals(3, result.size)
        // score 50에 가장 가까운 3개: c(0), b(5)/d(5), ...
        assertEquals("c", result[0].id) // 차이 0
    }

    @Test
    fun `matchingEvents - 빈 이벤트 리스트`() {
        val result = ReturnDataInterpolator.matchingEvents(score = 50, events = emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `matchingEvents - limit 지정`() {
        val events = (0..10).map { createEvent(id = "e$it", score = it * 10) }

        val result = ReturnDataInterpolator.matchingEvents(score = 50, events = events, limit = 2)

        assertEquals(2, result.size)
    }

    @Test
    fun `matchingEvents - 동일 score 차이 시 score 작은 것 우선`() {
        val events = listOf(
            createEvent(id = "a", score = 40),
            createEvent(id = "b", score = 60),
        )

        // 두 이벤트 모두 score=50에서 10 차이
        val result = ReturnDataInterpolator.matchingEvents(score = 50, events = events, limit = 2)

        assertEquals(2, result.size)
        assertEquals(40, result[0].score) // score 작은 것 우선
        assertEquals(60, result[1].score)
    }

    private fun createDataPoint(score: Int, oneMonth: Double) = ReturnDataPoint(
        score = score,
        returns = HistoricalReturns(oneMonth, oneMonth * 2, oneMonth * 3, oneMonth * 4),
        worstCase = HistoricalReturns(-oneMonth, -oneMonth * 2, -oneMonth * 3, -oneMonth * 4),
        bestCase = HistoricalReturns(oneMonth * 2, oneMonth * 4, oneMonth * 6, oneMonth * 8),
        sampleCount = score * 2,
    )

    private fun createEvent(id: String, score: Int) = ReturnEventEntry(
        id = id,
        date = Instant.now(),
        score = score,
        descriptionKey = "event_$id",
    )
}
