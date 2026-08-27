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

    // ── 카드가 셀을 그대로 채우면 One UI 의 세로로 긴 셀에서 길쭉해 보인다 ──

    @Test
    fun `1x1 카드는 짧은 변 기준 정사각`() {
        assertEquals(57f, WidgetLayoutMode.squareCardSideDp(57f, 92f), 0.01f)
        assertEquals(60f, WidgetLayoutMode.squareCardSideDp(80f, 60f), 0.01f)
    }

    // ── 통합 위젯 배치 3모드: 낮으면 가로 나열, 좁으면 세로 스택, 넓으면 리스트 ──

    @Test
    fun `대시보드 - 높이 100dp 미만이면 가로 나열(ROW)`() {
        assertEquals(DashboardArrangement.ROW, WidgetLayoutMode.dashboardArrangement(250f, 57f))
        assertEquals(DashboardArrangement.ROW, WidgetLayoutMode.dashboardArrangement(160f, 99.9f))
    }

    @Test
    fun `대시보드 - 높이는 충분한데 폭 170dp 미만이면 세로 스택(COLUMN)`() {
        assertEquals(DashboardArrangement.COLUMN, WidgetLayoutMode.dashboardArrangement(110f, 300f))
        assertEquals(DashboardArrangement.COLUMN, WidgetLayoutMode.dashboardArrangement(169.9f, 150f))
    }

    @Test
    fun `대시보드 - 폭·높이 모두 충분하면 리스트(LIST)`() {
        assertEquals(DashboardArrangement.LIST, WidgetLayoutMode.dashboardArrangement(170f, 100f))
        assertEquals(DashboardArrangement.LIST, WidgetLayoutMode.dashboardArrangement(320f, 250f))
    }
}
