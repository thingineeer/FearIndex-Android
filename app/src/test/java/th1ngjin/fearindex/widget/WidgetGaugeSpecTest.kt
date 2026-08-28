package th1ngjin.fearindex.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType

class WidgetGaugeSpecTest {

    @Test
    fun `sweep - 0점은 0도, 100점은 270도, 50점은 135도`() {
        assertEquals(0f, WidgetGaugeSpec.sweepAngle(0))
        assertEquals(270f, WidgetGaugeSpec.sweepAngle(100))
        assertEquals(135f, WidgetGaugeSpec.sweepAngle(50))
    }

    @Test
    fun `sweep - 범위 밖 점수는 클램프`() {
        assertEquals(0f, WidgetGaugeSpec.sweepAngle(-5))
        assertEquals(270f, WidgetGaugeSpec.sweepAngle(120))
    }

    @Test
    fun `dailyChange - previousClose 반올림 후 차이, 없으면 null`() {
        assertEquals(6, WidgetGaugeSpec.dailyChange(71, 65.4))
        assertEquals(-1, WidgetGaugeSpec.dailyChange(51, 51.6)) // 52 로 반올림
        assertEquals(0, WidgetGaugeSpec.dailyChange(55, 55.2))
        assertNull(WidgetGaugeSpec.dailyChange(55, null))
    }

    @Test
    fun `changeGlyph - iOS 위젯 표기(0 화살표, 상승, 하락)`() {
        assertEquals("→", WidgetGaugeSpec.changeGlyph(0))
        assertEquals("↑", WidgetGaugeSpec.changeGlyph(6))
        assertEquals("↓", WidgetGaugeSpec.changeGlyph(-1))
    }

    @Test
    fun `indexName - Global KOSPI Crypto 풀네임`() {
        assertEquals("Global", WidgetGaugeSpec.indexName(FearIndexType.MARKET))
        assertEquals("KOSPI", WidgetGaugeSpec.indexName(FearIndexType.KOSPI))
        assertEquals("Crypto", WidgetGaugeSpec.indexName(FearIndexType.CRYPTO))
    }
}
