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

        /**
         * 통합 위젯 배치 — 넓고 낮으면(폭 ≥ 높이×1.6) 가로 나열이 공백 없이 폭을 채우고,
         * 세로가 길면 [게이지|이름/등급] 리스트. 좁은 폭은 리스트를 축소해서 수용.
         * 셀 세로 스택은 셀당 게이지+텍스트 2줄이 안 들어가 실험 후 폐기(2026-08-28).
         */
        fun dashboardArrangement(widthDp: Float, heightDp: Float): DashboardArrangement =
            if (widthDp >= heightDp * 1.6f) DashboardArrangement.ROW else DashboardArrangement.LIST

        /** ROW 모드 등급 표시 여부 — 최소 높이(57dp)에선 게이지+이름만 들어간다. */
        fun rowModeShowsRating(heightDp: Float): Boolean = heightDp >= 85f
    }
}

/** 통합 위젯 배치 모드. */
enum class DashboardArrangement { ROW, LIST }
