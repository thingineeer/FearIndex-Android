package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
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
 * 기본값은 Google 공식 테스트 인터스티셜 광고 ID — 프로덕션 출시 전 교체 필요.
 */
object InterstitialAdManager {

    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false
    private var currentAdUnitId: String = TEST_INTERSTITIAL_AD_UNIT_ID

    /**
     * 인터스티셜 광고를 백그라운드에서 로드한다.
     * 이미 로드 중이거나 로드된 상태면 무시.
     */
    fun loadAd(
        context: Context,
        adUnitId: String = TEST_INTERSTITIAL_AD_UNIT_ID,
    ) {
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
        val ad = interstitialAd
        if (ad == null) {
            // 광고 없음 → 다음 기회를 위해 미리 로드 시도
            loadAd(activity, currentAdUnitId)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadAd(activity, currentAdUnitId)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Timber.w("InterstitialAd show failed: ${error.message}")
                interstitialAd = null
                loadAd(activity, currentAdUnitId)
            }
        }
        ad.show(activity)
    }
}
