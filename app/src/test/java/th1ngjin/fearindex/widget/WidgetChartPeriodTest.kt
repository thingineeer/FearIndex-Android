package th1ngjin.fearindex.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/** 차트 위젯 기간 세그먼트 — 앱 차트 탭과 동일 표기(3M/6M/1Y/3Y/5Y), 기본 3M. */
class WidgetChartPeriodTest {

    @Test
    fun `기간 5종과 일수`() {
        assertEquals(listOf("3M", "6M", "1Y", "3Y", "5Y"), WidgetChartPeriod.entries.map { it.label })
        assertEquals(listOf(90, 180, 365, 1095, 1825), WidgetChartPeriod.entries.map { it.days })
    }

    @Test
    fun `기본값은 3M`() {
        assertEquals(WidgetChartPeriod.M3, WidgetChartPeriod.DEFAULT)
    }

    @Test
    fun `저장된 이름으로 복원 - 모르는 값이면 기본값`() {
        assertEquals(WidgetChartPeriod.Y1, WidgetChartPeriod.fromName("Y1"))
        assertEquals(WidgetChartPeriod.M3, WidgetChartPeriod.fromName(null))
        assertEquals(WidgetChartPeriod.M3, WidgetChartPeriod.fromName("garbage"))
    }

    @Test
    fun `x축 날짜 포맷 - 6개월 이하는 월_일, 그 이상은 연_월`() {
        assertEquals(false, WidgetChartPeriod.M3.useYearMonthAxis)
        assertEquals(false, WidgetChartPeriod.M6.useYearMonthAxis)
        assertEquals(true, WidgetChartPeriod.Y1.useYearMonthAxis)
        assertEquals(true, WidgetChartPeriod.Y5.useYearMonthAxis)
    }

    @Test
    fun `장기 기간은 다운샘플링 - 5Y를 원본 그대로 그리면 선이 뭉개진다`() {
        assertEquals(listOf(null, null, 180, 150, 150), WidgetChartPeriod.entries.map { it.maxSamplePoints })
    }
}
