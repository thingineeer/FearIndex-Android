package th1ngjin.fearindex

import android.app.Application
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.appcheck.AppCheckInitializer
import th1ngjin.fearindex.core.crash.CrashReporter
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class FearIndexApp : Application() {

    @Inject lateinit var analytics: AnalyticsManager
    @Inject lateinit var crashReporter: CrashReporter
    @Inject lateinit var remoteConfig: RemoteConfigManager
    @Inject lateinit var appCheck: AppCheckInitializer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        initFirebase()
        setupNotificationChannels()
        initAdMob()
        registerLifecycle()
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initFirebase() {
        appCheck.initialize(isDebug = BuildConfig.DEBUG)

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

    private fun setupNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            "fear_index_alerts",
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun initAdMob() {
        try {
            MobileAds.initialize(this)
        } catch (e: Exception) {
            Timber.w(e, "AdMob init failed — placeholder config")
        }
    }

    private fun registerLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    analytics.log(AnalyticsEvent.앱포그라운드)
                }

                override fun onStop(owner: LifecycleOwner) {
                    analytics.log(AnalyticsEvent.앱백그라운드)
                }
            },
        )
        analytics.log(AnalyticsEvent.앱시작)
    }
}
