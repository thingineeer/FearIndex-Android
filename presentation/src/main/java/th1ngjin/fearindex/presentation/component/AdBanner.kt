package th1ngjin.fearindex.presentation.component

import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
    val adsConfig by remoteConfig.adsConfig.collectAsStateWithLifecycle()
    val canRequestAds by AdRequestAvailability.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequestAds || !adsConfig.adsEnabled || adUnitId.isBlank()) {
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = bannerAdWidthDp(maxWidth.value) ?: return@BoxWithConstraints
        val adSize = remember(context, widthDp) {
            AdSize.getInlineAdaptiveBannerAdSize(widthDp, bannerAdMaxHeightDp())
        }

        key(adUnitId, adSize) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adSize.height.dp),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(adSize)
                        this.adUnitId = adUnitId
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            adSize.getHeightInPixels(ctx),
                        )
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                analytics.log(AnalyticsEvent.배너광고노출(화면 = screenName))
                            }

                            override fun onAdClicked() {
                                analytics.log(AnalyticsEvent.배너광고클릭(화면 = screenName))
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                analytics.log(AnalyticsEvent.배너광고실패(에러메시지 = error.message))
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
            )
        }
    }
}
