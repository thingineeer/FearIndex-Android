package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import th1ngjin.fearindex.core.ads.AdRetryPolicy
import timber.log.Timber

/**
 * AdMob 인터스티셜 광고 매니저 (GMA Next-Gen SDK).
 *
 * Next-Gen 콜백은 백그라운드 스레드에서 오므로 내부 상태 변경/재시도 스케줄/외부 콜백은 전부
 * [mainHandler] 로 메인 스레드에 디스패치한다(단일 스레드 상태 머신 유지).
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

    private val retryPolicy = AdRetryPolicy()
    private var retryCount: Int = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    override val isReady: Boolean
        get() = interstitialAd != null

    override fun preload(context: Context, adUnitId: String) {
        loadAd(context = context, adUnitId = adUnitId)
    }

    /**
     * 인터스티셜 광고를 백그라운드에서 로드한다.
     * 이미 로드 중이거나 로드된 상태면 무시.
     * @param isRetry 재시도 진입 여부(재시도 스케줄 자신은 카운터를 초기화하지 않음).
     */
    fun loadAd(
        context: Context,
        adUnitId: String,
        isRetry: Boolean = false,
    ) {
        if (adUnitId.isBlank() || isAdScreenshotMode()) return
        if (isLoading || interstitialAd != null) return
        currentAdUnitId = adUnitId
        isLoading = true
        if (!isRetry) {
            // 새 로드 사이클 시작 — 이전 재시도 스케줄/카운터 정리.
            mainHandler.removeCallbacksAndMessages(null)
            retryCount = 0
        }

        val appContext = context.applicationContext
        InterstitialAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mainHandler.post {
                        Timber.d("InterstitialAd loaded")
                        interstitialAd = ad
                        isLoading = false
                        retryCount = 0
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mainHandler.post {
                        Timber.w("InterstitialAd failed: ${adError.message}")
                        interstitialAd = null
                        isLoading = false
                        scheduleRetry(appContext, adUnitId, adError.code.name)
                    }
                }
            },
        )
    }

    /**
     * no-fill/네트워크 등 일시적 실패면 exponential backoff로 재로드 예약.
     * INVALID_REQUEST(설정 오류)나 최대 횟수 초과면 재시도하지 않는다.
     */
    private fun scheduleRetry(context: Context, adUnitId: String, errorCodeName: String) {
        if (!AdRetryPolicy.isRetryable(errorCodeName)) return
        val delay = retryPolicy.nextDelayMillis(retryCount) ?: return
        retryCount += 1
        mainHandler.postDelayed({ loadAd(context, adUnitId, isRetry = true) }, delay)
        Timber.d("InterstitialAd retry #$retryCount in ${delay}ms")
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

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                mainHandler.post { callbacks.onShown() }
            }

            override fun onAdImpression() {
                mainHandler.post { callbacks.onImpression() }
            }

            override fun onAdDismissedFullScreenContent() {
                mainHandler.post {
                    interstitialAd = null
                    callbacks.onDismissed()
                    loadAd(activity, currentAdUnitId)
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                mainHandler.post {
                    Timber.w("InterstitialAd show failed: ${fullScreenContentError.message}")
                    interstitialAd = null
                    callbacks.onFailedToShow(fullScreenContentError.message)
                    loadAd(activity, currentAdUnitId)
                }
            }
        }
        ad.show(activity)
    }
}
