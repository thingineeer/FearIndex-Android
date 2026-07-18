package th1ngjin.fearindex.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * iOS `OnboardingEligibility` 미러 — 신규 설치 첫 실행에만 자동 투어 자격.
 * 판별 신호: 기존 버전이 모든 유저에게 남긴 deviceId 존재 여부.
 */
class OnboardingEligibilityTest {

    @Test
    fun `deviceId가 없으면 신규 설치 - 자격 부여`() {
        assertTrue(OnboardingEligibility.captureValue(deviceIdPresent = false))
    }

    @Test
    fun `deviceId가 이미 있으면 업데이트 유저 - 자격 없음`() {
        assertFalse(OnboardingEligibility.captureValue(deviceIdPresent = true))
    }
}
