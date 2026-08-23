package th1ngjin.fearindex.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType

class InterstitialAdPolicyTest {

    @Test
    fun `기본 정책은 iOS와 동일한 세션 cap 쿨다운과 Android 5초 진입 지연을 가진다`() {
        val config = InterstitialAdPolicyConfig()

        assertEquals(2, config.sessionCap)
        assertEquals(180_000L, config.cooldownMillis)
        assertEquals(5_000L, config.kospiEntryDelayMillis)
    }

    @Test
    fun `광고 비활성화 또는 준비 안됨이면 노출할 수 없다`() {
        val policy = InterstitialAdPolicy()

        assertFalse(policy.canShow(isReady = true, nowMillis = 1_000L, config = InterstitialAdPolicyConfig(adsEnabled = false)))
        assertFalse(policy.canShow(isReady = true, nowMillis = 1_000L, config = InterstitialAdPolicyConfig(interstitialEnabled = false)))
        assertFalse(policy.canShow(isReady = true, nowMillis = 1_000L, config = InterstitialAdPolicyConfig(canRequestAds = false)))
        assertFalse(policy.canShow(isReady = false, nowMillis = 1_000L, config = InterstitialAdPolicyConfig()))
    }

    @Test
    fun `세션 cap에 도달하면 추가 노출을 막는다`() {
        val policy = InterstitialAdPolicy()
        val config = InterstitialAdPolicyConfig(sessionCap = 2, cooldownMillis = 0L)

        assertTrue(policy.canShow(isReady = true, nowMillis = 1_000L, config = config))
        policy.recordShown(nowMillis = 1_000L)
        assertTrue(policy.canShow(isReady = true, nowMillis = 2_000L, config = config))
        policy.recordShown(nowMillis = 2_000L)

        assertFalse(policy.canShow(isReady = true, nowMillis = 3_000L, config = config))
        assertEquals(2, policy.impressionCount)
    }

    @Test
    fun `쿨다운이 지나기 전에는 노출할 수 없다`() {
        val policy = InterstitialAdPolicy()
        val config = InterstitialAdPolicyConfig(cooldownMillis = 180_000L)

        policy.recordShown(nowMillis = 10_000L)

        assertFalse(policy.canShow(isReady = true, nowMillis = 189_999L, config = config))
        assertTrue(policy.canShow(isReady = true, nowMillis = 190_000L, config = config))
    }

    @Test
    fun `KOSPI 홈 진입 광고는 market 또는 crypto에서 kospi로 진입할 때만 예약한다`() {
        val policy = InterstitialAdPolicy()
        val config = InterstitialAdPolicyConfig()

        assertTrue(policy.shouldScheduleKospiEntry(FearIndexType.MARKET, FearIndexType.KOSPI, config))
        assertTrue(policy.shouldScheduleKospiEntry(FearIndexType.CRYPTO, FearIndexType.KOSPI, config))
        assertFalse(policy.shouldScheduleKospiEntry(FearIndexType.KOSPI, FearIndexType.KOSPI, config))
        assertFalse(policy.shouldScheduleKospiEntry(FearIndexType.MARKET, FearIndexType.CRYPTO, config))
        assertFalse(
            policy.shouldScheduleKospiEntry(
                FearIndexType.MARKET,
                FearIndexType.KOSPI,
                InterstitialAdPolicyConfig(canRequestAds = false),
            ),
        )
    }

    @Test
    fun `KOSPI 홈 진입 광고는 실제 노출 성공 후 세션당 1회만 허용한다`() {
        val policy = InterstitialAdPolicy()
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)

        assertTrue(policy.shouldScheduleKospiEntry(FearIndexType.MARKET, FearIndexType.KOSPI, config))
        policy.recordShown(nowMillis = 1_000L, kospiEntry = true)

        assertFalse(policy.shouldScheduleKospiEntry(FearIndexType.CRYPTO, FearIndexType.KOSPI, config))
    }

    @Test
    fun `한 show cycle에서 노출 기록은 한 번만 반영한다`() {
        val policy = InterstitialAdPolicy()

        policy.markShowing()
        policy.recordShown(nowMillis = 1_000L, kospiEntry = true)
        policy.recordShown(nowMillis = 1_100L, kospiEntry = true)
        policy.recordDismissed()

        assertEquals(1, policy.impressionCount)
        assertFalse(policy.canShowKospiEntry(isReady = true, nowMillis = 2_000L, config = InterstitialAdPolicyConfig(cooldownMillis = 0L)))
    }

    @Test
    fun `백그라운드 30분 이상 후 복귀하면 세션이 리셋돼 KOSPI 진입 광고를 다시 노출할 수 있다`() {
        val policy = InterstitialAdPolicy()
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)
        policy.markShowing()
        policy.recordShown(nowMillis = 1_000L, kospiEntry = true)
        policy.recordDismissed()
        assertFalse(policy.canShowKospiEntry(isReady = true, nowMillis = 2_000L, config = config))

        policy.recordBackgroundEntry(nowMillis = 10_000L)
        val didReset = policy.handleForegroundEntry(nowMillis = 10_000L + 30L * 60L * 1_000L)

        assertTrue(didReset)
        assertEquals(0, policy.impressionCount)
        assertTrue(policy.canShowKospiEntry(isReady = true, nowMillis = 10_000L + 30L * 60L * 1_000L, config = config))
        assertTrue(policy.shouldScheduleKospiEntry(FearIndexType.MARKET, FearIndexType.KOSPI, config))
    }

    @Test
    fun `백그라운드 30분 미만 복귀는 같은 세션이라 리셋하지 않는다`() {
        val policy = InterstitialAdPolicy()
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)
        policy.markShowing()
        policy.recordShown(nowMillis = 1_000L, kospiEntry = true)
        policy.recordDismissed()

        policy.recordBackgroundEntry(nowMillis = 10_000L)
        val didReset = policy.handleForegroundEntry(nowMillis = 10_000L + 10L * 60L * 1_000L)

        assertFalse(didReset)
        assertEquals(1, policy.impressionCount)
        assertFalse(policy.canShowKospiEntry(isReady = true, nowMillis = 20_000L, config = config))
    }

    @Test
    fun `백그라운드 기록이 없는 콜드스타트 복귀는 리셋하지 않고 기록은 1회만 소비된다`() {
        val policy = InterstitialAdPolicy()
        policy.markShowing()
        policy.recordShown(nowMillis = 1_000L, kospiEntry = true)
        policy.recordDismissed()

        assertFalse(policy.handleForegroundEntry(nowMillis = 100L * 60L * 60L * 1_000L))
        assertEquals(1, policy.impressionCount)

        policy.recordBackgroundEntry(nowMillis = 10_000L)
        assertTrue(policy.handleForegroundEntry(nowMillis = 10_000L + 31L * 60L * 1_000L))
        // 같은 백그라운드 기록으로 두 번 리셋되지 않는다
        policy.markShowing()
        policy.recordShown(nowMillis = 20_000L, kospiEntry = true)
        policy.recordDismissed()
        assertFalse(policy.handleForegroundEntry(nowMillis = 10_000L + 120L * 60L * 1_000L))
        assertEquals(1, policy.impressionCount)
    }
}
