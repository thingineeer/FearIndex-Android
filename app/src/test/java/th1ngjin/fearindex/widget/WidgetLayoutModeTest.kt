package th1ngjin.fearindex.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutModeTest {

    @Test
    fun `1x1 크기(57dp)는 COMPACT`() {
        assertEquals(WidgetLayoutMode.COMPACT, WidgetLayoutMode.from(57f, 57f))
        assertEquals(WidgetLayoutMode.COMPACT, WidgetLayoutMode.from(80f, 80f))
    }

    @Test
    fun `한 축만 작아도 COMPACT (1x2, 2x1 리사이즈)`() {
        assertEquals(WidgetLayoutMode.COMPACT, WidgetLayoutMode.from(57f, 140f))
        assertEquals(WidgetLayoutMode.COMPACT, WidgetLayoutMode.from(140f, 57f))
    }

    @Test
    fun `기존 2x2(110dp 이상)는 FULL`() {
        assertEquals(WidgetLayoutMode.FULL, WidgetLayoutMode.from(110f, 110f))
        assertEquals(WidgetLayoutMode.FULL, WidgetLayoutMode.from(180f, 180f))
    }

    @Test
    fun `경계값 100dp 는 FULL, 99dp 는 COMPACT`() {
        assertEquals(WidgetLayoutMode.FULL, WidgetLayoutMode.from(100f, 100f))
        assertEquals(WidgetLayoutMode.COMPACT, WidgetLayoutMode.from(99.9f, 100f))
    }
}
