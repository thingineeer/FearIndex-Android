package th1ngjin.fearindex.domain.entity

/**
 * 프리미엄(= 광고 제거 IAP) 권한으로 열리는 기능 목록 (v1.9.4). iOS `PremiumFeature` 1:1.
 *
 * 프리미엄은 **정보량만 확장**한다 — 무료 기능(현재 점수 통계, 알림 30일)은 축소하지 않는다.
 * [analyticsKey] 는 analytics 파라미터(`feature`)로 그대로 전송된다 (iOS rawValue 동일).
 */
enum class PremiumFeature(val analyticsKey: String) {
    /** 점수별 과거 수익률 슬라이더 (임의 점수 탐색) */
    SCORE_EXPLORER("score_explorer"),

    /** 알림 내역 무제한 보관 (무료 30일) */
    NOTIFICATION_HISTORY_UNLIMITED("notification_history_unlimited"),
}
