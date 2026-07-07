package th1ngjin.fearindex.core.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앱오픈 광고 노출 정책 — 순수 로직. iOS `AppOpenAdCoordinator.canAttemptForegroundShow` 1:1.
 *
 * 핵심: 콜드스타트 최초 실행은 절대 제외(backgroundEnteredAt 없음).
 * 백그라운드에 최소 체류시간 이상 머물다 복귀할 때만, 세션 cap/cooldown 이내에서 노출.
 */
class AppOpenAdPolicyTest {

    private val config = AppOpenAdConfig(
        enabled = true,
        sessionCap = 2,
        cooldownMillis = 600_000L,
        minBackgroundMillis = 30_000L,
    )

    private fun policy() = AppOpenAdPolicy()

    @Test
    fun `콜드스타트 최초 실행은 노출 안 함 (백그라운드 진입 기록 없음)`() {
        val p = policy()
        // 백그라운드 진입 기록 없이(=콜드스타트) 바로 포그라운드 자격 판정
        assertFalse(
            p.canShowOnForeground(
                nowMillis = 100_000L,
                isReady = true,
                canRequestAds = true,
                config = config,
            ),
        )
    }

    @Test
    fun `백그라운드 30초 이상 머물다 복귀하면 노출 허용`() {
        val p = policy()
        p.recordBackgroundEntry(nowMillis = 0L)
        assertTrue(
            p.canShowOnForeground(
                nowMillis = 30_000L, // 정확히 30초
                isReady = true,
                canRequestAds = true,
                config = config,
            ),
        )
    }

    @Test
    fun `백그라운드 체류가 최소시간 미만이면 노출 안 함`() {
        val p = policy()
        p.recordBackgroundEntry(nowMillis = 0L)
        assertFalse(
            p.canShowOnForeground(
                nowMillis = 29_999L,
                isReady = true,
                canRequestAds = true,
                config = config,
            ),
        )
    }

    @Test
    fun `canRequestAds false면 노출 안 함`() {
        val p = policy()
        p.recordBackgroundEntry(nowMillis = 0L)
        assertFalse(
            p.canShowOnForeground(60_000L, isReady = true, canRequestAds = false, config = config),
        )
    }

    @Test
    fun `config disabled면 노출 안 함`() {
        val p = policy()
        p.recordBackgroundEntry(nowMillis = 0L)
        assertFalse(
            p.canShowOnForeground(60_000L, isReady = true, canRequestAds = true, config = config.copy(enabled = false)),
        )
    }

    @Test
    fun `광고 미로드(isReady false)면 노출 안 함`() {
        val p = policy()
        p.recordBackgroundEntry(nowMillis = 0L)
        assertFalse(
            p.canShowOnForeground(60_000L, isReady = false, canRequestAds = true, config = config),
        )
    }

    @Test
    fun `세션 cap 도달하면 노출 안 함`() {
        val p = policy()
        // 2회 노출 기록
        p.recordBackgroundEntry(0L)
        p.recordImpression(30_000L)
        p.recordBackgroundEntry(700_000L)
        p.recordImpression(730_000L)
        // 3번째 시도 (cap=2 도달)
        p.recordBackgroundEntry(1_400_000L)
        assertFalse(
            p.canShowOnForeground(1_430_000L, isReady = true, canRequestAds = true, config = config),
        )
    }

    @Test
    fun `cooldown 이내 재노출 시도는 노출 안 함`() {
        val p = policy()
        p.recordBackgroundEntry(0L)
        p.recordImpression(30_000L) // 첫 노출 at 30s
        // cooldown 600s 이내에 다시 복귀
        p.recordBackgroundEntry(100_000L)
        assertFalse(
            p.canShowOnForeground(130_000L, isReady = true, canRequestAds = true, config = config),
        )
    }

    @Test
    fun `cooldown 경과 후에는 재노출 허용`() {
        val p = policy()
        p.recordBackgroundEntry(0L)
        p.recordImpression(30_000L) // 첫 노출 at 30s
        // cooldown 600s 경과
        p.recordBackgroundEntry(700_000L)
        assertTrue(
            p.canShowOnForeground(730_000L, isReady = true, canRequestAds = true, config = config),
        )
    }

    @Test
    fun `노출 후 backgroundEnteredAt 리셋되어 같은 포그라운드에서 재노출 안 됨`() {
        val p = policy()
        p.recordBackgroundEntry(0L)
        p.recordImpression(30_000L) // 이 안에서 backgroundEnteredAt 소비
        // 같은 포그라운드 세션에서 다시 판정 (백그라운드 재진입 없이) → 콜드스타트와 동일하게 차단
        assertFalse(
            p.canShowOnForeground(40_000L, isReady = true, canRequestAds = true, config = config.copy(cooldownMillis = 0L)),
        )
    }
}
