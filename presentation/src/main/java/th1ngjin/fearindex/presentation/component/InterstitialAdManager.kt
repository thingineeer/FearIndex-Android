package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber

/**
 * AdMob 인터스티셜 광고 매니저.
 *
 * 사용 패턴:
 *   1. [loadAd] 로 미리 로드
 *   2. 임계 이벤트(예: N회 액션) 발생 시 [showIfReady] 호출
 *   3. 노출 후 자동으로 다음 광고를 다시 로드
 *
 * 단위 ID는 호출자가 명시 (`BuildConfig.ADMOB_INTERSTITIAL`)하고, debug/release 분기는 BuildConfig가 담당.
 */
object InterstitialAdManager : InterstitialAdController {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false
    private var currentAdUnitId: String = ""

    override val isReady: Boolean
        get() = interstitialAd != null

    override fun preload(context: Context, adUnitId: String) {
        loadAd(context = context, adUnitId = adUnitId)
    }

    /**
     * 인터스티셜 광고를 백그라운드에서 로드한다.
     * 이미 로드 중이거나 로드된 상태면 무시.
     */
    fun loadAd(
        context: Context,
        adUnitId: String,
    ) {
        if (adUnitId.isBlank() || isAdScreenshotMode()) return
        if (isLoading || interstitialAd != null) return
        currentAdUnitId = adUnitId
        isLoading = true

        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.d("InterstitialAd loaded")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("InterstitialAd failed: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            },
        )
    }

    /**
     * 로드된 인터스티셜이 있으면 표시하고, 노출 후 다음 광고를 미리 로드한다.
     * 로드된 광고가 없으면 조용히 skip하고 다음 로드만 트리거.
     */
    fun showIfReady(activity: Activity) {
        show(
            activity = activity,
            adUnitId = currentAdUnitId,
            callbacks = InterstitialAdCallbacks(
                onShown = {},
                onImpression = {},
                onDismissed = {},
                onFailedToShow = {},
            ),
        )
    }

    override fun show(
        activity: Activity,
        adUnitId: String,
        callbacks: InterstitialAdCallbacks,
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            callbacks.onFailedToShow("Activity is not resumed")
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            // 광고 없음 → 다음 기회를 위해 미리 로드 시도
            callbacks.onFailedToShow("InterstitialAd is not ready")
            loadAd(activity, adUnitId.ifBlank { currentAdUnitId })
            return
        }
        currentAdUnitId = adUnitId.ifBlank { currentAdUnitId }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                callbacks.onShown()
            }

            override fun onAdImpression() {
                callbacks.onImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                callbacks.onDismissed()
                loadAd(activity, currentAdUnitId)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("InterstitialAd show failed: ${error.message}")
                interstitialAd = null
                callbacks.onFailedToShow(error.message)
                loadAd(activity, currentAdUnitId)
            }
        }
        ad.show(activity)
    }
}
