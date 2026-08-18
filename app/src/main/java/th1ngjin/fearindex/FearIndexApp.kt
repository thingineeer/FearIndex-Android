package th1ngjin.fearindex

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.glance.appwidget.updateAll
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import java.lang.ref.WeakReference
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.appcheck.AppCheckInitializer
import th1ngjin.fearindex.core.crash.CrashReporter
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.core.ads.AdRequestAvailability
import th1ngjin.fearindex.core.ads.AdSdkState
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager
import th1ngjin.fearindex.presentation.component.AppOpenAdManager
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.entity.NotificationPermissionSyncPolicy
import th1ngjin.fearindex.domain.entity.NotificationSettings
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import th1ngjin.fearindex.domain.service.OnboardingStore
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import th1ngjin.fearindex.notification.NotificationChannels
import th1ngjin.fearindex.notification.NotificationHistoryRecorder
import th1ngjin.fearindex.widget.CryptoFearWidget
import th1ngjin.fearindex.widget.DashboardFearWidget
import th1ngjin.fearindex.widget.FearWidgetUpdateWorker
import th1ngjin.fearindex.variant.VariantHooks
import th1ngjin.fearindex.widget.KospiFearWidget
import th1ngjin.fearindex.widget.MarketFearWidget
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class FearIndexApp : Application() {

    @Inject lateinit var analytics: AnalyticsManager
    @Inject lateinit var crashReporter: CrashReporter
    @Inject lateinit var remoteConfig: RemoteConfigManager
    @Inject lateinit var appCheck: AppCheckInitializer
    @Inject lateinit var purchaseManager: PurchaseManager
    @Inject lateinit var notificationRepository: Lazy<NotificationRepository>
    @Inject lateinit var deviceIdProvider: Lazy<DeviceIdProvider>
    @Inject lateinit var onboardingStore: OnboardingStore
    @Inject lateinit var notificationHistoryUseCase: Lazy<NotificationHistoryUseCase>

    /** 알림 센터 잔류 알림 → 내역 동기화 (경로 3, 포그라운드 진입 시). */
    private val historyRecorder by lazy { NotificationHistoryRecorder(notificationHistoryUseCase.get()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentActivity = WeakReference<Activity>(null)

    companion object {
        private const val ADMOB_APP_ID_META_KEY = "com.google.android.gms.ads.APPLICATION_ID"
        private const val ADMOB_APP_ID_FALLBACK = "ca-app-pub-5283496525222246~1308884877"

        /**
         * 앱오픈 광고 매니저 — Application 스코프 단일 인스턴스.
         * 강제 업데이트/스플래시 표시 중 노출 차단을 위해 MainActivity 가 [AppOpenAdManager.isForegroundBlocked]
         * 를 세팅할 수 있도록 노출한다.
         */
        val appOpenAdManager = AppOpenAdManager()
    }

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        // 신규 설치 첫 실행 자격 판별 — FCM(deviceId 생성)보다 먼저 확정해야 한다.
        onboardingStore.captureEligibilityIfNeeded()
        val screenshotMode = ScreenshotMode.isEnabled()
        initFirebase(screenshotMode)
        setupNotificationChannels()
        if (!screenshotMode) {
            registerFCMToken()
            initAdMob()
            purchaseManager.start()
            // debug 빌드: 저장된 결제 테스트 오버라이드 재적용 (release 는 no-op)
            VariantHooks.onApplicationCreate(this, purchaseManager)
            trackCurrentActivity()
            FearWidgetUpdateWorker.schedule(this)
        }
        registerLifecycle(screenshotMode)
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initFirebase(screenshotMode: Boolean) {
        if (screenshotMode) {
            Timber.i("Screenshot mode enabled; skipping Firebase initialization and remote startup calls")
            return
        }

        initializeFirebaseApp()
        appCheck.initialize(isDebug = BuildConfig.DEBUG)
        analytics.setAnalyticsCollectionEnabled(true)
        crashReporter.setCollectionEnabled(true)

        val buildType = if (BuildConfig.DEBUG) "debug" else "release"
        val language = Locale.getDefault().toLanguageTag()

        analytics.setStandardUserProperties(
            appVersion = BuildConfig.VERSION_NAME,
            buildType = buildType,
            language = language,
        )
        crashReporter.setStandardKeys(
            appVersion = BuildConfig.VERSION_NAME,
            buildType = buildType,
            language = language,
        )
        appScope.launch { remoteConfig.fetchAndActivate() }
    }

    private fun initializeFirebaseApp() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return

        val options = FirebaseOptions.fromResource(this)
        checkNotNull(options) {
            "Firebase options missing. Ensure google-services.json is present for ${BuildConfig.APPLICATION_ID}."
        }
        FirebaseApp.initializeApp(this, options)
    }

    private fun setupNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NotificationChannels.FEAR_INDEX_ALERTS,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun registerFCMToken() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Timber.d("FCM token obtained: ${token.take(10)}...")
            if (BuildConfig.DEBUG) {
                Timber.d("FCM_FULL_TOKEN_FOR_TESTING: $token")
            }
            val deviceId = deviceIdProvider.get().loadDeviceId()
            appScope.launch {
                try {
                    notificationRepository.get().registerFCMToken(deviceId, token)
                    Timber.d("FCM token registered on startup")
                } catch (e: Exception) {
                    Timber.e(e, "FCM token registration failed on startup")
                }
            }
        }
    }

    private fun initAdMob() {
        // GMA Next-Gen SDK: initialize 는 반드시 background thread 에서(메인 호출 시 ANR 위험, 공식 가이드).
        // App ID 는 Manifest meta-data(com.google.android.gms.ads.APPLICATION_ID)를 단일 출처로 읽는다 —
        // UMP SDK 도 같은 meta-data 를 요구하므로 Manifest 항목은 유지.
        appScope.launch {
            try {
                MobileAds.initialize(
                    this@FearIndexApp,
                    InitializationConfig.Builder(admobApplicationId()).build(),
                ) {
                    // 어댑터 초기화 완료 콜백은 백그라운드 스레드 → 앱오픈 preload 는 메인으로 디스패치
                    // (iOS: startAdMobSDK 콜백에서 preload).
                    AdSdkState.markInitialized()
                    mainHandler.post {
                        appOpenAdManager.preloadIfNeeded(
                            this@FearIndexApp,
                            BuildConfig.ADMOB_APP_OPEN,
                            remoteConfig.adsConfig.value.appOpenAdConfig(),
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "AdMob init failed — placeholder config")
            }
        }
    }

    /** Manifest `com.google.android.gms.ads.APPLICATION_ID` meta-data (manifestPlaceholders admobAppId). */
    private fun admobApplicationId(): String {
        val metaData = runCatching {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
        }.getOrNull()
        return metaData?.getString(ADMOB_APP_ID_META_KEY)?.takeIf { it.isNotBlank() }
            ?: ADMOB_APP_ID_FALLBACK.also { Timber.w("AdMob APPLICATION_ID meta-data missing; using fallback") }
    }

    /** 앱오픈 광고는 Activity present 가 필요하므로 최상단 Activity 를 약참조로 추적. */
    private fun trackCurrentActivity() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivity = WeakReference(activity)
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun registerLifecycle(screenshotMode: Boolean) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    if (screenshotMode) return
                    analytics.log(AnalyticsEvent.앱포그라운드)
                    syncNotificationPermissionState()
                    historyRecorder.syncActiveNotifications(this@FearIndexApp)
                    showAppOpenAdIfEligible()
                    refreshWidgets()
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (screenshotMode) return
                    analytics.log(AnalyticsEvent.앱백그라운드)
                    // 백그라운드 진입 기록 — 이후 복귀가 콜드스타트가 아님을 표시(콜드스타트 노출 방지).
                    appOpenAdManager.recordBackgroundEntry()
                }
            },
        )
        if (screenshotMode) return
        analytics.log(AnalyticsEvent.앱시작)
    }

    /** 백그라운드→포그라운드 복귀 시 앱오픈 광고 노출 시도 (콜드스타트/자격 미달이면 자동 스킵). */
    private fun showAppOpenAdIfEligible() {
        val activity = currentActivity.get() ?: return
        appOpenAdManager.showOnForegroundIfEligible(
            activity = activity,
            adUnitId = BuildConfig.ADMOB_APP_OPEN,
            config = remoteConfig.adsConfig.value.appOpenAdConfig(),
            isAdFree = purchaseManager.isAdFree.value,
            canRequestAds = AdRequestAvailability.canRequestAds.value,
        )
    }

    /** 앱 포그라운드 진입 시 홈 화면 위젯을 즉시 최신 지수로 갱신. */
    private fun refreshWidgets() {
        appScope.launch {
            try {
                MarketFearWidget().updateAll(this@FearIndexApp)
                KospiFearWidget().updateAll(this@FearIndexApp)
                CryptoFearWidget().updateAll(this@FearIndexApp)
                DashboardFearWidget().updateAll(this@FearIndexApp)
            } catch (e: Exception) {
                Timber.w(e, "Widget foreground refresh failed")
            }
        }
    }

    private fun syncNotificationPermissionState() {
        appScope.launch {
            val settings = notificationRepository.get().loadSettingsLocal()
            val systemAuthorized = NotificationManagerCompat
                .from(this@FearIndexApp)
                .areNotificationsEnabled()
            val action = NotificationPermissionSyncPolicy.foregroundAction(
                systemAuthorized = systemAuthorized,
                appNotificationEnabled = settings.notificationEnabled,
            )
            if (action == NotificationPermissionSyncPolicy.Action.DISABLE_AND_SYNC_SERVER) {
                syncNotificationDisabled(settings)
            }
        }
    }

    private suspend fun syncNotificationDisabled(settings: NotificationSettings) {
        try {
            notificationRepository.get().updateSettings(
                deviceIdProvider.get().loadDeviceId(),
                settings.copy(notificationEnabled = false),
            )
            Timber.d("Notification permission revoked; synced disabled state")
        } catch (e: Exception) {
            Timber.e(e, "Notification permission sync failed")
        }
    }
}
