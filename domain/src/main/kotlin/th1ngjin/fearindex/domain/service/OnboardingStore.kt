package th1ngjin.fearindex.domain.service

/**
 * 온보딩 투어 영속 상태 — iOS `OnboardingEligibility` + `@AppStorage` 미러.
 *
 * 자동 노출은 신규 설치 첫 실행 1회만. 자격/노출/위젯가이드 확인 여부를 영구 보관한다.
 */
interface OnboardingStore {

    /**
     * 프로세스 시작 시 1회 호출. 최초 실행에서만 신규 설치 여부를 판별해 영구 저장한다.
     * FCM 초기화(=deviceId 생성)보다 **먼저** 호출해야 신규 설치를 올바로 판별한다.
     */
    fun captureEligibilityIfNeeded()

    /** 신규 설치 유저인가 (업데이트 유저는 false). */
    fun isTourEligible(): Boolean

    /** 투어를 이미 봤는가 (자동 노출 1회 제한). */
    fun hasSeenTour(): Boolean

    /** 투어가 화면에 뜬 순간 기록 — 중간에 앱을 종료해도 재노출 없음. */
    fun markTourSeen()

    /** 위젯 사용법 가이드를 봤는가. */
    fun hasSeenWidgetGuide(): Boolean

    /** 위젯 사용법 가이드 진입 기록. */
    fun markWidgetGuideSeen()
}
