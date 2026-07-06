package th1ngjin.fearindex.presentation.component

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.ads.AdRequestAvailability
import th1ngjin.fearindex.core.ads.AdRetryPolicy
import th1ngjin.fearindex.presentation.di.AdsEntryPoint

/**
 * AdMob Adaptive 배너 광고 컴포넌트.
 *
 * - 스크롤 콘텐츠 안에 배치되므로 inline adaptive banner 사용.
 * - SDK에 전달하는 폭은 화면 전체가 아니라 실제 parent content 폭을 사용해 ad frame resize를 피한다.
 * - 단위 ID는 호출자가 명시 (`BuildConfig.ADMOB_BANNER_HOME` 등) — debug/release 분기는 BuildConfig가 담당
 * - Preview/스크린샷 모드에서는 빈 뷰 렌더링
 */
@Composable
fun AdBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    screenName: String = "홈",
) {
    if (LocalInspectionMode.current) {
        return
    }
    // Play Store promo 스크린샷 촬영 모드 — `adb shell setprop debug.screenshot_mode 1` 으로 활성화.
    // 광고가 첨부된 promo 이미지는 AdMob 정책 위반 위험이 있어 명시적으로 숨긴다.
    if (isAdScreenshotMode()) {
        return
    }

    val context = LocalContext.current
    val adsEntryPoint = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AdsEntryPoint::class.java)
    }
    val analytics = remember(adsEntryPoint) {
        adsEntryPoint.analyticsManager()
    }
    val remoteConfig = remember(adsEntryPoint) {
        adsEntryPoint.remoteConfigManager()
    }
    val purchaseManager = remember(adsEntryPoint) {
        adsEntryPoint.purchaseManager()
    }
    // 광고 제거 IAP 구매자에게는 배너 자체를 렌더하지 않는다.
    val isAdFree by purchaseManager.isAdFree.collectAsStateWithLifecycle()
    val adsConfig by remoteConfig.adsConfig.collectAsStateWithLifecycle()
    val canRequestAds by AdRequestAvailability.canRequestAds.collectAsStateWithLifecycle()
    if (isAdFree || !canRequestAds || !adsConfig.adsEnabled || adUnitId.isBlank()) {
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = bannerAdWidthDp(maxWidth.value) ?: return@BoxWithConstraints
        val adSize = remember(context, widthDp) {
            AdSize.getInlineAdaptiveBannerAdSize(widthDp, bannerAdMaxHeightDp())
        }

        // 인라인 어댑티브 배너는 로드 전 adSize.height가 0일 수 있어, 컨테이너를 그 값으로
        // 고정하면 광고가 수신돼도 0높이/클립으로 안 보인다. onAdLoaded 후 실제 AdView 높이를
        // state로 끌어올려 컨테이너 높이를 갱신한다. (배너 미표시 버그 수정)
        var loadedHeightDp by remember(adUnitId, adSize) { mutableStateOf<Int?>(null) }
        val containerHeightDp = resolveBannerHeightDp(
            estimatedHeightDp = adSize.height,
            loadedHeightDp = loadedHeightDp,
        )

        // AdView와 재시도 스케줄을 (adUnitId, adSize) 키로 remember 해 리컴포지션마다 재생성되지
        // 않도록 유지한다. onAdFailedToLoad(no-fill/네트워크) 시 exponential backoff로 재요청해
        // 첫 요청 실패가 그대로 빈 슬롯으로 남지 않게 한다.
        val retryHandler = remember { Handler(Looper.getMainLooper()) }
        val retryPolicy = remember { AdRetryPolicy() }
        val adView = remember(adUnitId, adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                var retryCount = 0
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        retryCount = 0
                        // iOS(AdBannerView.swift) 와 동일: 로드 후 SDK가 확정한 adSize 실제 높이
                        // 를 clamp 없이 컨테이너에 반영. (렌더 height 는 레이아웃 타이밍에 부정확)
                        val loadedDp = this@apply.adSize?.height
                            ?.takeIf { it > 0 }
                            ?: adSize.height
                        loadedHeightDp = loadedDp
                        analytics.log(AnalyticsEvent.배너광고노출(화면 = screenName))
                    }

                    override fun onAdClicked() {
                        analytics.log(AnalyticsEvent.배너광고클릭(화면 = screenName))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        analytics.log(AnalyticsEvent.배너광고실패(에러메시지 = error.message))
                        if (!AdRetryPolicy.isRetryable(error.code)) return
                        val delay = retryPolicy.nextDelayMillis(retryCount) ?: return
                        retryCount += 1
                        retryHandler.postDelayed({ loadAd(AdRequest.Builder().build()) }, delay)
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView) {
            onDispose {
                retryHandler.removeCallbacksAndMessages(null)
                adView.destroy()
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeightDp.dp),
            factory = { adView },
        )
    }
}
