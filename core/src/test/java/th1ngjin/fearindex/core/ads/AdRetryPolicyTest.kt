package th1ngjin.fearindex.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 광고 로드 실패 시 재시도 backoff 정책 — 순수 로직.
 * exponential backoff (base*2^n), 최대 지연 cap, 최대 시도 횟수 초과 시 재시도 중단.
 */
class AdRetryPolicyTest {

    private val policy = AdRetryPolicy(
        baseDelayMillis = 2_000L,
        maxDelayMillis = 60_000L,
        maxRetries = 5,
    )

    @Test
    fun `첫 재시도는 base 지연`() {
        assertEquals(2_000L, policy.nextDelayMillis(previousRetryCount = 0))
    }

    @Test
    fun `재시도마다 지연이 2배로 증가 (exponential)`() {
        assertEquals(2_000L, policy.nextDelayMillis(0))  // 2s
        assertEquals(4_000L, policy.nextDelayMillis(1))  // 4s
        assertEquals(8_000L, policy.nextDelayMillis(2))  // 8s
        assertEquals(16_000L, policy.nextDelayMillis(3)) // 16s
    }

    @Test
    fun `지연은 maxDelay로 상한 clamp`() {
        // maxRetries=8 정책으로 큰 지수의 clamp를 검증 (2s * 2^5 = 64s 이지만 cap 60s)
        val longPolicy = AdRetryPolicy(baseDelayMillis = 2_000L, maxDelayMillis = 60_000L, maxRetries = 12)
        assertEquals(60_000L, longPolicy.nextDelayMillis(5))  // 64s → 60s
        assertEquals(60_000L, longPolicy.nextDelayMillis(10)) // 훨씬 큼 → 60s
    }

    @Test
    fun `최대 시도 횟수 초과면 null (재시도 중단)`() {
        // previousRetryCount 가 maxRetries 이상이면 더 이상 재시도 안 함
        assertNull(policy.nextDelayMillis(previousRetryCount = 5))
        assertNull(policy.nextDelayMillis(previousRetryCount = 6))
    }

    @Test
    fun `maxRetries 직전까지는 재시도 허용`() {
        // previousRetryCount = 4 → 5번째 시도 (아직 maxRetries=5 미만). 2s * 2^4 = 32s
        assertEquals(32_000L, policy.nextDelayMillis(4))
    }

    @Test
    fun `no-fill과 네트워크 오류는 재시도 대상, 잘못된 요청은 아님`() {
        // AdMob LoadAdError code: 0=INTERNAL, 1=INVALID_REQUEST, 2=NETWORK, 3=NO_FILL
        assertEquals(true, AdRetryPolicy.isRetryable(errorCode = 2)) // NETWORK_ERROR
        assertEquals(true, AdRetryPolicy.isRetryable(errorCode = 3)) // NO_FILL
        assertEquals(true, AdRetryPolicy.isRetryable(errorCode = 0)) // INTERNAL (일시적)
        assertEquals(false, AdRetryPolicy.isRetryable(errorCode = 1)) // INVALID_REQUEST (설정 오류 — 재시도 무의미)
    }
}
