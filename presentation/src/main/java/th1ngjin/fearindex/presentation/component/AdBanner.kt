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
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.ads.AdRequestAvailability
import th1ngjin.fearindex.core.ads.AdSdkState
import th1ngjin.fearindex.core.ads.AdRetryPolicy
import th1ngjin.fearindex.presentation.di.AdsEntryPoint
import th1ngjin.fearindex.presentation.feature.onboarding.LocalOnboardingTour

/**
 * AdMob Adaptive 배너 광고 컴포넌트 (GMA Next-Gen SDK).
 *
 * - 스크롤 콘텐츠 안에 배치되므로 inline adaptive banner 사용.
 * - Next-Gen SDK 콜백은 백그라운드 스레드에서 오므로 Compose state/Analytics 갱신은 메인 Handler 로 넘긴다.
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
    // 온보딩 투어 중에는 배너를 숨긴다(카드/하이라이트 가림 + 불필요한 노출 방지).
    if (LocalOnboardingTour.current?.isActive == true) {
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
    // Next-Gen SDK 는 MobileAds.initialize 완료 전 load 시 UninitializedPropertyAccessException 위험 —
    // 초기화가 끝난 뒤에만 AdView 를 만들고 로드한다(완료 시 리컴포지션으로 자연 진입).
    val sdkInitialized by AdSdkState.isInitialized.collectAsStateWithLifecycle()
    val blockedBy = buildString {
        if (isAdFree) append("adFree ")
        if (!canRequestAds) append("consent ")
        if (!adsConfig.adsEnabled) append("rcOff ")
        if (adUnitId.isBlank()) append("noUnit ")
        if (!sdkInitialized) append("sdkInit ")
    }.trim()
    // 게이트 상태 변화를 release logcat 에서도 추적 (수익 직결 — 첫 진입 미노출 진단용)
    var lastLoggedGate by remember(adUnitId) { mutableStateOf<String?>(null) }
    if (lastLoggedGate != blockedBy) {
        lastLoggedGate = blockedBy
        android.util.Log.i(
            AD_LOG_TAG,
            if (blockedBy.isEmpty()) "[$screenName] 게이트 통과 → 배너 진입" else "[$screenName] 게이트 차단: $blockedBy",
        )
    }
    if (blockedBy.isNotEmpty()) {
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
        // Next-Gen SDK: 요청은 BannerAdRequest(단위 ID + 크기), 로드는 AdView.loadAd(request, callback),
        // 이벤트는 로드된 BannerAd.adEventCallback. 모든 콜백은 백그라운드 스레드 → mainHandler 로 디스패치.
        val mainHandler = remember { Handler(Looper.getMainLooper()) }
        val retryPolicy = remember { AdRetryPolicy() }
        val slot = remember(adUnitId, adSize) {
            BannerAdSlot(android.widget.FrameLayout(context)).also { slot ->
                slot.container.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                // ⚠️ 재시도는 반드시 "새 AdView" 로 — Next-Gen SDK 는 같은 AdView 에 loadAd 를
                // 다시 호출하면 CANCELLED("publisher action")+NO_FILL 쌍으로 재경매가 무산된다
                // (S22 콜드스타트 실측: 동일 뷰 재시도 연속 실패 vs 새 뷰 즉시 fill — bugs-fixed 66번).
                lateinit var startLoad: (Int) -> Unit
                startLoad = { retryCount ->
                    if (!slot.disposed) {
                        val adView = AdView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        }
                        slot.adView?.destroy()
                        slot.adView = adView
                        slot.container.removeAllViews()
                        slot.container.addView(adView)
                        val loadCallback = object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(ad: BannerAd) {
                                // destroy 됐거나(늦은 콜백) 다른 재시도 뷰로 교체된 경우 무시.
                                if (slot.disposed || slot.adView !== adView) return
                                android.util.Log.i(AD_LOG_TAG, "[$screenName] onAdLoaded (retryCount=$retryCount)")
                                slot.loaded = true
                                slot.failedSinceLoad = false
                                val loadedDp = ad.getAdSize().height.takeIf { it > 0 } ?: adSize.height
                                ad.adEventCallback = object : BannerAdEventCallback {
                                    override fun onAdClicked() {
                                        mainHandler.post {
                                            analytics.log(AnalyticsEvent.배너광고클릭(화면 = screenName))
                                        }
                                    }
                                }
                                mainHandler.post {
                                    loadedHeightDp = loadedDp
                                    analytics.log(AnalyticsEvent.배너광고노출(화면 = screenName))
                                }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                if (slot.disposed || slot.adView !== adView) return
                                mainHandler.post {
                                    if (slot.disposed || slot.adView !== adView) return@post
                                    analytics.log(AnalyticsEvent.배너광고실패(에러메시지 = adError.message))
                                    slot.failedSinceLoad = true
                                    if (!AdRetryPolicy.isRetryable(adError.code.name)) {
                                        android.util.Log.w(
                                            AD_LOG_TAG,
                                            "[$screenName] 로드 실패 code=${adError.code.name} msg=${adError.message} → 재시도 안 함(비재시도 코드)",
                                        )
                                        return@post
                                    }
                                    val delay = retryPolicy.nextDelayMillis(retryCount)
                                    if (delay == null) {
                                        android.util.Log.w(
                                            AD_LOG_TAG,
                                            "[$screenName] 로드 실패 code=${adError.code.name} msg=${adError.message} → 재시도 소진(retryCount=$retryCount)",
                                        )
                                        return@post
                                    }
                                    android.util.Log.w(
                                        AD_LOG_TAG,
                                        "[$screenName] 로드 실패 code=${adError.code.name} msg=${adError.message} → ${delay}ms 후 재시도(#${retryCount + 1})",
                                    )
                                    slot.retryPending = true
                                    mainHandler.postDelayed({
                                        slot.retryPending = false
                                        startLoad(retryCount + 1)
                                    }, delay)
                                }
                            }
                        }
                        android.util.Log.i(AD_LOG_TAG, "[$screenName] loadAd 요청 (unit=…${adUnitId.takeLast(6)}, retryCount=$retryCount)")
                        adView.loadAd(BannerAdRequest.Builder(adUnitId, adSize).build(), loadCallback)
                    }
                }
                slot.restart = { startLoad(0) }
                startLoad(0)
            }
        }

        // 앱 복귀(ON_RESUME) 시 미로드+미예약 상태면 새 로드 사이클 시작 — backoff 소진(최대 300s 대기) 후
        // 영구 빈 슬롯으로 남는 것을 방지한다. 이미 로드됐거나 재시도가 예약돼 있으면 아무것도 안 한다.
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, slot) {
            var firstResumeSeen = false
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    // 첫 컴포지션 직후의 ON_RESUME 은 초기 로드와 중복이므로 건너뛴다.
                    if (!firstResumeSeen) {
                        firstResumeSeen = true
                        return@LifecycleEventObserver
                    }
                    if (!slot.disposed && !slot.loaded && slot.failedSinceLoad && !slot.retryPending) {
                        android.util.Log.i(AD_LOG_TAG, "[$screenName] 복귀 재시도 — 미로드 슬롯 새 사이클 시작")
                        slot.restart?.invoke()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        DisposableEffect(slot) {
            onDispose {
                slot.disposed = true
                mainHandler.removeCallbacksAndMessages(null)
                slot.adView?.destroy()
                slot.adView = null
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeightDp.dp),
            factory = { slot.container },
        )
    }
}

/** 배너 진단 로그 태그 — release 에서도 logcat 확인 가능 (어댑터 진단 FearIndexAdapters 와 짝) */
private const val AD_LOG_TAG = "FearIndexAds"

/**
 * remember 로 유지되는 배너 슬롯 — 컨테이너와 "현재" AdView, dispose 플래그를 함께 들고 있어
 * ① 백그라운드 스레드의 늦은 SDK 콜백(destroy 시 CANCELLED 등)이 죽은 뷰에 재시도를 걸지 못하게 하고
 * ② 재시도마다 새 AdView 로 교체해도 (컨테이너 유지로) Compose AndroidView 는 재구성되지 않게 한다.
 */
private class BannerAdSlot(val container: android.widget.FrameLayout) {
    @Volatile
    var adView: AdView? = null

    @Volatile
    var disposed: Boolean = false

    /** 이 슬롯에서 광고가 한 번이라도 로드됐는가 (복귀 재시도 게이트) */
    @Volatile
    var loaded: Boolean = false

    /** 마지막 시도가 실패로 끝났는가 (복귀 재시도 게이트) */
    @Volatile
    var failedSinceLoad: Boolean = false

    /** backoff 재시도가 예약돼 있는가 (복귀 재시도와 중복 방지) */
    @Volatile
    var retryPending: Boolean = false

    /** 복귀(ON_RESUME) 시 새 로드 사이클을 시작하는 훅 — remember 블록의 startLoad(0) */
    @Volatile
    var restart: (() -> Unit)? = null
}
