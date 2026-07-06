package th1ngjin.fearindex.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 광고 로드 실패 시 재시도 backoff 정책 — 순수 로직. iOS AdBannerView 스펙 1:1.
 * retryDelays [5s, 15s, 45s] 3회 → 그 후 최종 1회 300s(5분). 이후 중단.
 */
class AdRetryPolicyTest {

    private val policy = AdRetryPolicy()

    @Test
    fun `초기 재시도는 5s, 15s, 45s 순서 (iOS retryDelays)`() {
        assertEquals(5_000L, policy.nextDelayMillis(previousRetryCount = 0))
        assertEquals(15_000L, policy.nextDelayMillis(previousRetryCount = 1))
        assertEquals(45_000L, policy.nextDelayMillis(previousRetryCount = 2))
    }

    @Test
    fun `초기 3회 후 최종 재시도는 300s (5분, 1회 한정)`() {
        // previousRetryCount = 3 → 초기 배열 소진, 최종 300s 1회
        assertEquals(300_000L, policy.nextDelayMillis(previousRetryCount = 3))
    }

    @Test
    fun `최종 재시도 이후는 null (재시도 완전 중단)`() {
        // previousRetryCount = 4 → 초기 3 + 최종 1 = 4회 모두 소진
        assertNull(policy.nextDelayMillis(previousRetryCount = 4))
        assertNull(policy.nextDelayMillis(previousRetryCount = 5))
    }

    @Test
    fun `no-fill과 네트워크 오류는 재시도 대상, 잘못된 요청은 아님`() {
        // AdMob LoadAdError code: 0=INTERNAL, 1=INVALID_REQUEST, 2=NETWORK, 3=NO_FILL
        assertEquals(true, AdRetryPolicy.isRetryable(errorCode = 2)) // NETWORK_ERROR
        assertEquals(true, AdRetryPolicy.isRetryable(errorCode = 3)) // NO_FILL
        assertEquals(true, AdRetryPolicy.isRetryable(errorCode = 0)) // INTERNAL (일시적)
        assertEquals(false, AdRetryPolicy.isRetryable(errorCode = 1)) // INVALID_REQUEST (설정 오류)
    }
}
