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
    fun `대시보드 - 넓고 낮으면(폭 ≥ 높이×1_6) 가로 나열이 공백 없이 채운다`() {
        assertEquals(DashboardArrangement.ROW, WidgetLayoutMode.dashboardArrangement(250f, 57f))
        assertEquals(DashboardArrangement.ROW, WidgetLayoutMode.dashboardArrangement(160f, 99.9f))
        assertEquals(DashboardArrangement.ROW, WidgetLayoutMode.dashboardArrangement(250f, 124f))
    }

    @Test
    fun `대시보드 - 세로가 상대적으로 길면 리스트(좁으면 축소 리스트)`() {
        assertEquals(DashboardArrangement.LIST, WidgetLayoutMode.dashboardArrangement(110f, 300f))
        assertEquals(DashboardArrangement.LIST, WidgetLayoutMode.dashboardArrangement(123f, 96f))
        assertEquals(DashboardArrangement.LIST, WidgetLayoutMode.dashboardArrangement(320f, 250f))
    }

    @Test
    fun `가로 나열 모드 등급은 높이 85dp 이상에서만 (57dp 최소 크기에선 게이지+이름만)`() {
        assertEquals(false, WidgetLayoutMode.rowModeShowsRating(57f))
        assertEquals(false, WidgetLayoutMode.rowModeShowsRating(84.9f))
        assertEquals(true, WidgetLayoutMode.rowModeShowsRating(85f))
    }
}
