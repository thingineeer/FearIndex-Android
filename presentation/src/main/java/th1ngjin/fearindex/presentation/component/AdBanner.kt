package th1ngjin.fearindex.presentation.component

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.presentation.di.AnalyticsEntryPoint

/**
 * AdMob 배너 광고 컴포넌트.
 *
 * 기본값은 Google 공식 테스트 배너 광고 ID이며, 실제 프로덕션 출시 전에는
 * AdMob 콘솔에서 발급한 단위 ID로 교체해야 한다.
 */
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID,
    screenName: String = "홈",
) {
    // v1.0.1 내부 테스트 빌드: 광고 전체 비활성화 (다음 버전에서 아래 return 제거)
    return

    // Compose Preview/스크린샷 모드에서는 실제 AdView 생성 시 crash 가능 → 빈 뷰 렌더링
    if (LocalInspectionMode.current) {
        return
    }

    val context = LocalContext.current
    val analytics = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
            .analyticsManager()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
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
