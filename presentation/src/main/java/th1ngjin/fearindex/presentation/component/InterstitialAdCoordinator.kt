package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.domain.entity.FearIndexType

data class InterstitialAdCallbacks(
    val onShown: () -> Unit,
    val onImpression: () -> Unit,
    val onDismissed: () -> Unit,
    val onFailedToShow: (String) -> Unit,
)

interface InterstitialAdController {
    val isReady: Boolean
    fun preload(context: Context, adUnitId: String)
    fun show(activity: Activity, adUnitId: String, callbacks: InterstitialAdCallbacks)
}

interface InterstitialAdReporter {
    fun onImpression(count: Int)
    fun onDismissed(durationSeconds: Int)
    fun onFailed(message: String)
}

class AnalyticsInterstitialAdReporter(
    private val analytics: AnalyticsManager,
) : InterstitialAdReporter {
    override fun onImpression(count: Int) {
        analytics.log(AnalyticsEvent.인터스티셜광고노출(노출횟수 = count))
    }

    override fun onDismissed(durationSeconds: Int) {
        analytics.log(AnalyticsEvent.인터스티셜광고닫기(시청초 = durationSeconds))
    }

    override fun onFailed(message: String) {
        analytics.log(AnalyticsEvent.인터스티셜광고실패(에러메시지 = message))
    }
}

object NoOpInterstitialAdReporter : InterstitialAdReporter {
    override fun onImpression(count: Int) = Unit
    override fun onDismissed(durationSeconds: Int) = Unit
    override fun onFailed(message: String) = Unit
}

class InterstitialAdCoordinator(
    private val adController: InterstitialAdController = InterstitialAdManager,
    private val reporter: InterstitialAdReporter = NoOpInterstitialAdReporter,
    private val policy: InterstitialAdPolicy = InterstitialAdPolicy(),
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() },
) {
    val impressionCount: Int get() = policy.impressionCount

    fun preloadIfNeeded(
        context: Context,
        adUnitId: String,
        config: InterstitialAdPolicyConfig,
        screenshotMode: Boolean = isAdScreenshotMode(),
    ) {
        if (!config.adsEnabled || !config.canRequestAds || !config.interstitialEnabled) return
        if (screenshotMode || adUnitId.isBlank()) return
        adController.preload(context, adUnitId)
    }

    fun shouldScheduleKospiEntry(
        previousType: FearIndexType,
        selectedType: FearIndexType,
        config: InterstitialAdPolicyConfig,
    ): Boolean = policy.shouldScheduleKospiEntry(previousType, selectedType, config)

    fun showKospiEntryIfAvailable(
        activity: Activity,
        adUnitId: String,
        config: InterstitialAdPolicyConfig,
    ): Boolean {
        if (adUnitId.isBlank()) return false
        if (!policy.canShowKospiEntry(adController.isReady, nowMillis(), config)) return false
        return showIfAvailable(activity, adUnitId, config, kospiEntry = true)
    }

    private fun showIfAvailable(
        activity: Activity,
        adUnitId: String,
        config: InterstitialAdPolicyConfig,
        kospiEntry: Boolean,
    ): Boolean {
        if (adUnitId.isBlank()) return false
        if (!policy.canShow(adController.isReady, nowMillis(), config)) return false

        policy.markShowing()
        val startedAtMillis = nowMillis()
        adController.show(
            activity = activity,
            adUnitId = adUnitId,
            callbacks = InterstitialAdCallbacks(
                onShown = {
                    policy.recordShown(nowMillis = nowMillis(), kospiEntry = kospiEntry)
                },
                onImpression = {
                    policy.recordShown(nowMillis = nowMillis(), kospiEntry = kospiEntry)
                    reporter.onImpression(policy.impressionCount)
                },
                onDismissed = {
                    policy.recordDismissed()
                    reporter.onDismissed(durationSeconds(startedAtMillis, nowMillis()))
                    preloadIfNeeded(activity, adUnitId, config)
                },
                onFailedToShow = { message ->
                    policy.recordFailedToShow()
                    reporter.onFailed(message)
                    preloadIfNeeded(activity, adUnitId, config)
                },
            ),
        )
        return true
    }

    private fun durationSeconds(startedAtMillis: Long, endedAtMillis: Long): Int =
        ((endedAtMillis - startedAtMillis).coerceAtLeast(0L) / 1_000L).toInt()
}
