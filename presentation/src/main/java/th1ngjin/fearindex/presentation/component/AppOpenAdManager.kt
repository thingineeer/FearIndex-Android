package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import th1ngjin.fearindex.core.ads.AppOpenAdConfig
import th1ngjin.fearindex.core.ads.AppOpenAdPolicy
import timber.log.Timber

/**
 * AdMob 앱오픈 광고 매니저 (GMA Next-Gen SDK) — iOS `AppOpenAdManager` + `AppOpenAdCoordinator` 대응 (2계층 통합).
 *
 * - Next-Gen 콜백은 백그라운드 스레드에서 오므로 상태 변경은 [mainHandler] 로 메인에 디스패치.
 * - 콜드스타트 최초 실행에는 절대 노출하지 않는다([AppOpenAdPolicy]가 backgroundEnteredAt로 판정).
 * - 백그라운드→포그라운드 복귀 시 [showOnForegroundIfEligible]로만 노출.
 * - 4시간 만료(iOS maxAdAge) + 세션 cap/cooldown 준수.
 * - 정책 게이트/세션 상태는 [AppOpenAdPolicy] 순수 로직에 위임하고 여기는 SDK 글루만.
 */
class AppOpenAdManager(
    private val policy: AppOpenAdPolicy = AppOpenAdPolicy(),
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var appOpenAd: AppOpenAd? = null
    private var isLoading: Boolean = false
    private var loadedAtMillis: Long = 0L
    private var isShowingAd: Boolean = false

    /** 강제 업데이트/스플래시 표시 중이면 앱오픈을 띄우지 않기 위한 외부 가드 플래그. */
    @Volatile
    var isForegroundBlocked: Boolean = false

    private val isFresh: Boolean
        get() = nowMillis() - loadedAtMillis < MAX_AD_AGE_MILLIS

    private val isReady: Boolean
        get() = appOpenAd != null && isFresh

    /** 백그라운드 진입(ON_STOP) 기록. 이후 포그라운드 복귀가 콜드스타트가 아님을 표시. */
    fun recordBackgroundEntry() {
        policy.recordBackgroundEntry(nowMillis())
    }

    /**
     * 정책 게이트 통과 시 광고를 미리 로드. cap 도달/미허용 시 폐기될 요청을 던지지 않는다.
     */
    fun preloadIfNeeded(context: Context, adUnitId: String, config: AppOpenAdConfig) {
        if (adUnitId.isBlank() || isAdScreenshotMode()) return
        if (!config.enabled) return
        if (isLoading || isReady) return
        if (policy.impressionCount >= config.sessionCap) return
        isLoading = true
        AppOpenAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    mainHandler.post {
                        appOpenAd = ad
                        loadedAtMillis = nowMillis()
                        isLoading = false
                        Timber.d("AppOpenAd loaded")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // iOS와 동일: 자동 backoff 없음. 다음 preloadIfNeeded 때 자연 재시도.
                    mainHandler.post {
                        isLoading = false
                        Timber.w("AppOpenAd load Error: ${adError.message}")
                    }
                }
            },
        )
    }

    /**
     * 포그라운드 복귀 시 자격 판정 후 노출. 콜드스타트/자격 미달이면 조용히 스킵하고 재로드만 시도.
     * iOS `showOnForegroundIfEligible`.
     */
    fun showOnForegroundIfEligible(
        activity: Activity,
        adUnitId: String,
        config: AppOpenAdConfig,
        isAdFree: Boolean,
        canRequestAds: Boolean,
    ) {
        if (isForegroundBlocked || isShowingAd) return
        val eligible = policy.canShowOnForeground(
            nowMillis = nowMillis(),
            isReady = isReady,
            isAdFree = isAdFree,
            canRequestAds = canRequestAds,
            config = config,
        )
        if (!eligible) {
            preloadIfNeeded(activity, adUnitId, config)
            return
        }
        val ad = appOpenAd ?: run {
            preloadIfNeeded(activity, adUnitId, config)
            return
        }
        // show() 직후 콜백이 오기 전에 중복 show 를 막기 위해 메인에서 선제 표시.
        isShowingAd = true
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                mainHandler.post { isShowingAd = true }
            }

            override fun onAdImpression() {
                mainHandler.post { policy.recordImpression(nowMillis()) }
            }

            override fun onAdDismissedFullScreenContent() {
                mainHandler.post {
                    appOpenAd = null
                    isShowingAd = false
                    preloadIfNeeded(activity, adUnitId, config)
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                mainHandler.post {
                    appOpenAd = null
                    isShowingAd = false
                    Timber.w("AppOpenAd show Error: ${fullScreenContentError.message}")
                    preloadIfNeeded(activity, adUnitId, config)
                }
            }
        }
        ad.show(activity)
    }

    fun resetSession() {
        policy.resetSession()
        appOpenAd = null
        loadedAtMillis = 0L
        isShowingAd = false
    }

    companion object {
        /** iOS maxAdAge = 4시간. 만료된 광고는 폐기하고 재로드. */
        private const val MAX_AD_AGE_MILLIS = 4L * 60L * 60L * 1_000L
    }
}
