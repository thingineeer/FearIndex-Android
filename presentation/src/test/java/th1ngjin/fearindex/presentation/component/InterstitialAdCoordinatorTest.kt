package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialAdCoordinatorTest {

    @Test
    fun `KOSPI 진입 광고는 impression callback에서 노출을 기록하고 dismiss에서 시청초를 기록한다`() {
        val controller = FakeInterstitialAdController(isReady = true)
        val reporter = FakeInterstitialAdReporter()
        var nowMillis = 10_000L
        val coordinator = InterstitialAdCoordinator(
            adController = controller,
            reporter = reporter,
            nowMillis = { nowMillis },
        )
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)

        assertTrue(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
        assertEquals("unit-id", controller.lastShownAdUnitId)
        assertEquals(0, coordinator.impressionCount)

        controller.showAd()
        controller.recordImpression()
        assertEquals(1, coordinator.impressionCount)
        assertEquals(listOf(1), reporter.impressions)

        nowMillis += 5_200L
        controller.dismiss()

        assertEquals(1, coordinator.impressionCount)
        assertEquals(listOf(5), reporter.dismissedSeconds)
        assertFalse(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
    }

    @Test
    fun `KOSPI 진입 광고 표시 실패는 세션 1회를 소진하지 않는다`() {
        val controller = FakeInterstitialAdController(isReady = true)
        val reporter = FakeInterstitialAdReporter()
        val coordinator = InterstitialAdCoordinator(
            adController = controller,
            reporter = reporter,
            nowMillis = { 10_000L },
        )
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)

        assertTrue(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
        controller.failToShow("not ready")

        assertEquals(0, coordinator.impressionCount)
        assertEquals(listOf("not ready"), reporter.failures)
        assertTrue(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
    }

    @Test
    fun `preload는 광고 비활성화 스크린샷 모드 빈 unit id에서 실행되지 않는다`() {
        val controller = FakeInterstitialAdController(isReady = false)
        val coordinator = InterstitialAdCoordinator(adController = controller)
        val context = mockk<Context>(relaxed = true)

        coordinator.preloadIfNeeded(context, "unit-id", InterstitialAdPolicyConfig(adsEnabled = false))
        coordinator.preloadIfNeeded(context, "unit-id", InterstitialAdPolicyConfig(canRequestAds = false))
        coordinator.preloadIfNeeded(context, "unit-id", InterstitialAdPolicyConfig(), screenshotMode = true)
        coordinator.preloadIfNeeded(context, "", InterstitialAdPolicyConfig())

        assertEquals(0, controller.preloadCalls)

        coordinator.preloadIfNeeded(context, "unit-id", InterstitialAdPolicyConfig())

        assertEquals(1, controller.preloadCalls)
    }


    @Test
    fun `KOSPI 진입 시 광고가 준비돼 있지 않으면 노출 대신 재로드를 건다`() {
        val controller = FakeInterstitialAdController(isReady = false)
        val coordinator = InterstitialAdCoordinator(adController = controller, nowMillis = { 10_000L })
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)

        assertFalse(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))

        assertEquals(1, controller.preloadCalls)
        assertEquals(null, controller.lastShownAdUnitId)
    }

    @Test
    fun `포그라운드 복귀는 10분 이상 백그라운드였으면 세션을 리셋하고 항상 preload를 건다`() {
        val controller = FakeInterstitialAdController(isReady = true)
        var nowMillis = 10_000L
        val coordinator = InterstitialAdCoordinator(adController = controller, nowMillis = { nowMillis })
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L)
        val context = mockk<Context>(relaxed = true)

        assertTrue(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
        controller.showAd(); controller.recordImpression(); controller.dismiss()
        assertFalse(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
        val preloadsBefore = controller.preloadCalls

        coordinator.recordBackgroundEntry()
        nowMillis += 5L * 60L * 1_000L
        coordinator.handleForegroundEntry(context, "unit-id", config)
        assertEquals(preloadsBefore + 1, controller.preloadCalls)
        assertFalse(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))

        coordinator.recordBackgroundEntry()
        nowMillis += 10L * 60L * 1_000L
        coordinator.handleForegroundEntry(context, "unit-id", config)
        assertEquals(preloadsBefore + 2, controller.preloadCalls)
        assertEquals(0, coordinator.impressionCount)
        assertTrue(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
    }

    @Test
    fun `세션 cap에 도달했으면 preload를 생략한다`() {
        val controller = FakeInterstitialAdController(isReady = true)
        val coordinator = InterstitialAdCoordinator(adController = controller, nowMillis = { 10_000L })
        val config = InterstitialAdPolicyConfig(cooldownMillis = 0L, sessionCap = 1)
        val context = mockk<Context>(relaxed = true)

        assertTrue(coordinator.showKospiEntryIfAvailable(mockk(relaxed = true), "unit-id", config))
        controller.showAd(); controller.recordImpression(); controller.dismiss()
        val preloadsAfterDismiss = controller.preloadCalls

        coordinator.preloadIfNeeded(context, "unit-id", config)

        assertEquals(preloadsAfterDismiss, controller.preloadCalls)
    }

    private class FakeInterstitialAdController(
        override var isReady: Boolean,
    ) : InterstitialAdController {
        var preloadCalls = 0
        var lastShownAdUnitId: String? = null
        private var callbacks: InterstitialAdCallbacks? = null

        override fun preload(context: Context, adUnitId: String) {
            preloadCalls += 1
        }

        override fun show(
            activity: Activity,
            adUnitId: String,
            callbacks: InterstitialAdCallbacks,
        ) {
            lastShownAdUnitId = adUnitId
            this.callbacks = callbacks
        }

        fun dismiss() {
            callbacks?.onDismissed?.invoke()
            callbacks = null
        }

        fun showAd() {
            callbacks?.onShown?.invoke()
        }

        fun recordImpression() {
            callbacks?.onImpression?.invoke()
        }

        fun failToShow(message: String) {
            callbacks?.onFailedToShow?.invoke(message)
            callbacks = null
        }
    }

    private class FakeInterstitialAdReporter : InterstitialAdReporter {
        val impressions = mutableListOf<Int>()
        val dismissedSeconds = mutableListOf<Int>()
        val failures = mutableListOf<String>()

        override fun onImpression(count: Int) {
            impressions += count
        }

        override fun onDismissed(durationSeconds: Int) {
            dismissedSeconds += durationSeconds
        }

        override fun onFailed(message: String) {
            failures += message
        }
    }
}
