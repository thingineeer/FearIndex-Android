package th1ngjin.fearindex.widget

/**
 * 위젯 크기 → 레이아웃 모드 (Play 리뷰 "1×1 위젯 필요" 대응, 2026-08-27).
 *
 * 단일 지수 위젯은 2×2 로 추가되지만 1×1(약 57dp)까지 줄일 수 있다.
 * 셀 하나의 실측 폭이 런처마다 57~80dp 라 2셀 최소(110dp) 미만이면 COMPACT 로 본다.
 */
enum class WidgetLayoutMode {
    /** 1×1 등 초소형: 점수만 크게 표시 */
    COMPACT,

    /** 기존 2×2: 라벨 + 점수 + 등급 */
    FULL,
    ;

    companion object {
        const val COMPACT_THRESHOLD_DP = 100f

        fun from(widthDp: Float, heightDp: Float): WidgetLayoutMode =
            if (widthDp < COMPACT_THRESHOLD_DP || heightDp < COMPACT_THRESHOLD_DP) COMPACT else FULL

        /**
         * 1×1 카드 한 변(dp) — One UI 셀은 세로가 길어 카드가 셀을 그대로 채우면 길쭉해 보인다.
         * 짧은 변 기준 정사각으로 잘라 다른 앱 1×1 위젯과 같은 실루엣을 만든다.
         */
        fun squareCardSideDp(widthDp: Float, heightDp: Float): Float = minOf(widthDp, heightDp)

        /** LIST 모드에 필요한 최소 폭 — 이보다 좁으면 이름/등급이 잘려 세로 스택으로 전환. */
        const val DASHBOARD_LIST_MIN_WIDTH_DP = 170f

        /**
         * 통합 위젯 배치 — 리사이즈 어느 방향으로도 글자가 잘리지 않게 3모드.
         * 낮으면 가로 나열, 좁으면 세로 스택, 둘 다 충분하면 [게이지|이름/등급] 리스트.
         */
        fun dashboardArrangement(widthDp: Float, heightDp: Float): DashboardArrangement = when {
            heightDp < COMPACT_THRESHOLD_DP -> DashboardArrangement.ROW
            widthDp < DASHBOARD_LIST_MIN_WIDTH_DP -> DashboardArrangement.COLUMN
            else -> DashboardArrangement.LIST
        }
    }
}

/** 통합 위젯 배치 모드. */
enum class DashboardArrangement { ROW, COLUMN, LIST }
