package th1ngjin.fearindex.presentation.feature.chart

import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import java.time.Instant

class ChartSelectionSummaryTest {

    private fun point(score: Double, iso: String) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.parse(iso),
    )

    @Test
    fun `점수는 반올림 표시, 등급은 원점수 기준`() {
        val summary = ChartSelectionSummary.of(point(54.6, "2026-09-01T15:00:00Z"))
        assertEquals(55, summary.score)
        assertEquals(FearIndex.Rating.NEUTRAL, summary.rating) // raw 54.6 < 55 → 중립
    }

    @Test
    fun `경계 55_0 은 탐욕`() {
        val summary = ChartSelectionSummary.of(point(55.0, "2026-09-01T15:00:00Z"))
        assertEquals(FearIndex.Rating.GREED, summary.rating)
    }

    @Test
    fun `날짜는 기존 툴팁과 같은 뉴욕 기준 yyyy_M_d`() {
        // 2026-09-02 03:00Z = 뉴욕 9/1 23:00
        val summary = ChartSelectionSummary.of(point(40.0, "2026-09-02T03:00:00Z"))
        assertEquals("2026/9/1", summary.dateText)
    }
}
