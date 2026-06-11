package th1ngjin.fearindex

import android.app.Application
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
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
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager
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
    @Inject lateinit var notificationRepository: Lazy<NotificationRepository>
    @Inject lateinit var deviceIdProvider: Lazy<DeviceIdProvider>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        val screenshotMode = ScreenshotMode.isEnabled()
        initFirebase(screenshotMode)
        setupNotificationChannels()
        if (!screenshotMode) {
            registerFCMToken()
            initAdMob()
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
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
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
                MobileAds.initialize(this@FearIndexApp)
            } catch (e: Exception) {
                Timber.w(e, "AdMob init failed — placeholder config")
            }
        }
    }

    private fun registerLifecycle(screenshotMode: Boolean) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    if (screenshotMode) return
                    analytics.log(AnalyticsEvent.앱포그라운드)
                    syncNotificationPermissionState()
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (screenshotMode) return
                    analytics.log(AnalyticsEvent.앱백그라운드)
                }
            },
        )
        if (screenshotMode) return
        analytics.log(AnalyticsEvent.앱시작)
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
