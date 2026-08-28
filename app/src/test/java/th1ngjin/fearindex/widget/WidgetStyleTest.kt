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
    fun `경계 55 - 원점수 55_0은 GREED, 54_9는 NEUTRAL (iOS·서버 통일)`() {
        // 2026-08-27 결정: 등급은 원점수 기준 + 경계값은 윗 밴드 귀속 (iOS ..<55 / 서버 <55)
        assertEquals(FearIndex.Rating.GREED, FearIndex.Rating.from(55.0))
        assertEquals(FearIndex.Rating.NEUTRAL, FearIndex.Rating.from(54.9))
        assertNotEquals(widgetFearScoreColor(54), widgetFearScoreColor(FearIndex.Rating.GREED))
        assertEquals(widgetFearScoreColor(55), widgetFearScoreColor(FearIndex.Rating.GREED))
    }
}
