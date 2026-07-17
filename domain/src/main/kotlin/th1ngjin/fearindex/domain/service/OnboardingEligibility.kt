package th1ngjin.fearindex.domain.service

/**
 * 온보딩 투어 자동 노출 자격 판별 — iOS `OnboardingEligibility` 미러.
 *
 * 신규 설치 첫 실행에만 자격을 부여한다. 판별 신호는 기존 버전이 모든 유저에게 남겨온
 * 영속 값(`stuck_counter_prefs` 의 `deviceId`) 존재 여부.
 *
 * - 신규 설치: 첫 진입 시(FCM 초기화 전) deviceId 가 없으므로 자격 true 로 고정.
 * - 업데이트 유저: deviceId 가 이미 있으므로 자격 false 로 고정 → 투어가 절대 안 뜬다.
 *
 * 실제 저장/판별 순서는 [OnboardingStore.captureEligibilityIfNeeded] 가 수행한다.
 */
object OnboardingEligibility {

    /** 판별된 자격 값 — deviceId 신호가 없을 때(신규 설치)만 true. */
    fun captureValue(deviceIdPresent: Boolean): Boolean = !deviceIdPresent
}
