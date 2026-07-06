package th1ngjin.fearindex

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
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
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager
import th1ngjin.fearindex.presentation.component.AppOpenAdManager
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.entity.NotificationPermissionSyncPolicy
import th1ngjin.fearindex.domain.entity.NotificationSettings
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import th1ngjin.fearindex.notification.NotificationChannels
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentActivity = WeakReference<Activity>(null)

    companion object {
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
        val screenshotMode = ScreenshotMode.isEnabled()
        initFirebase(screenshotMode)
        setupNotificationChannels()
        if (!screenshotMode) {
            registerFCMToken()
            initAdMob()
            purchaseManager.start()
            trackCurrentActivity()
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
        // AdMob 초기화는 background dispatcher 에서 — main thread 차단으로 cold start 1-2초 추가되어
        // splash 체감 시간이 길어지던 이슈(QA#10 연결) 해결.
        appScope.launch {
            try {
                MobileAds.initialize(this@FearIndexApp) {
                    // SDK 초기화 완료 후 앱오픈 광고 preload (iOS: startAdMobSDK 콜백에서 preload).
                    appOpenAdManager.preloadIfNeeded(
                        this@FearIndexApp,
                        BuildConfig.ADMOB_APP_OPEN,
                        remoteConfig.adsConfig.value.appOpenAdConfig(),
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "AdMob init failed — placeholder config")
            }
        }
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
                    showAppOpenAdIfEligible()
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
