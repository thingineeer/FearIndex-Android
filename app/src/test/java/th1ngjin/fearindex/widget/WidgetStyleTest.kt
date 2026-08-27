package th1ngjin.fearindex.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex

class WidgetStyleTest {

    @Test
    fun `rating 오버로드 - 등급별 색이 점수 기반과 동일 매핑`() {
        assertEquals(widgetFearScoreColor(10), widgetFearScoreColor(FearIndex.Rating.EXTREME_FEAR))
        assertEquals(widgetFearScoreColor(30), widgetFearScoreColor(FearIndex.Rating.FEAR))
        assertEquals(widgetFearScoreColor(50), widgetFearScoreColor(FearIndex.Rating.NEUTRAL))
        assertEquals(widgetFearScoreColor(60), widgetFearScoreColor(FearIndex.Rating.GREED))
        assertEquals(widgetFearScoreColor(90), widgetFearScoreColor(FearIndex.Rating.EXTREME_GREED))
    }

    @Test
    fun `원점수 55_4 - 표시는 반올림 55여도 색은 탐욕(GREED)`() {
        // 2026-08-27 사용자 결정: 등급/색은 원점수 기준 (반올림 재판정 금지)
        val rating = FearIndex.Rating.from(55.4)
        assertEquals(FearIndex.Rating.GREED, rating)
        assertNotEquals(widgetFearScoreColor(55), widgetFearScoreColor(rating))
    }
}
