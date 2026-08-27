package th1ngjin.fearindex.widget

/**
 * 차트 위젯 기간 세그먼트 — 앱 차트 탭과 동일 표기 (2026-08-28 사용자 결정: 3M/6M/1Y/3Y/5Y).
 * 선택은 위젯 인스턴스별 Glance state 에 저장한다.
 */
enum class WidgetChartPeriod(val label: String, val days: Int) {
    M3("3M", 90),
    M6("6M", 180),
    Y1("1Y", 365),
    Y3("3Y", 1095),
    Y5("5Y", 1825),
    ;

    /** 장기 기간은 x축을 연.월(yy.M)로, 단기는 월.일(M.d)로 표기. */
    val useYearMonthAxis: Boolean get() = days > 200

    /** 위젯 폭에 맞는 다운샘플 상한 — 5Y(1825점)를 원본 그대로 그리면 선이 뭉개진다. null = 원본. */
    val maxSamplePoints: Int?
        get() = when {
            days <= 200 -> null
            days <= 400 -> 180
            else -> 150
        }

    companion object {
        val DEFAULT = M3

        fun fromName(name: String?): WidgetChartPeriod =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
