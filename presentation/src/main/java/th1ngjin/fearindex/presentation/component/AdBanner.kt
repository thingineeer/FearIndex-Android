package th1ngjin.fearindex.presentation.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.DisplayMetrics
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
 * AdMob Adaptive 배너 광고 컴포넌트.
 *
 * - `getCurrentOrientationAnchoredAdaptiveBannerAdSize`로 디바이스 폭에 맞게 자동 사이즈 결정
 *   → 320×50 고정보다 수익률 +20~40% (Google 권장)
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
                setAdSize(adaptiveAdSize(ctx))
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

private fun adaptiveAdSize(context: Context): AdSize {
    val activity = context.findActivity()
    val metrics = if (activity != null) {
        DisplayMetrics().also { activity.windowManager.defaultDisplay.getMetrics(it) }
    } else {
        context.resources.displayMetrics
    }
    val dpWidth = (metrics.widthPixels / metrics.density).toInt().coerceAtLeast(320)
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, dpWidth)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
