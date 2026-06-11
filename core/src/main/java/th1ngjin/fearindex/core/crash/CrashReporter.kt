package th1ngjin.fearindex.core.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import th1ngjin.fearindex.core.debug.ScreenshotMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Crashlytics 래퍼.
 *
 * iOS와 같은 Firebase 프로젝트를 공유하므로 custom key `platform="android"`를 반드시 설정해
 * 대시보드에서 플랫폼별 크래시 필터링이 가능하도록 한다.
 */
@Singleton
class CrashReporter @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics by lazy { Firebase.crashlytics }

    fun setStandardKeys(appVersion: String, buildType: String, language: String) {
        if (ScreenshotMode.isEnabled()) return
        crashlytics.setCustomKey(Key.PLATFORM, "android")
        crashlytics.setCustomKey(Key.APP_VERSION, appVersion)
        crashlytics.setCustomKey(Key.BUILD_TYPE, buildType)
        crashlytics.setCustomKey(Key.LANGUAGE, language)
    }

    fun log(message: String) {
        if (ScreenshotMode.isEnabled()) return
        crashlytics.log(message)
    }

    fun recordException(throwable: Throwable) {
        if (ScreenshotMode.isEnabled()) return
        crashlytics.recordException(throwable)
    }

    fun setUserId(id: String) {
        if (ScreenshotMode.isEnabled()) return
        crashlytics.setUserId(id)
    }

    fun setCollectionEnabled(enabled: Boolean) {
        if (ScreenshotMode.isEnabled()) return
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    object Key {
        const val PLATFORM = "platform"
        const val APP_VERSION = "app_version"
        const val BUILD_TYPE = "build_type"
        const val LANGUAGE = "language"
    }
}
