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
    }
}
