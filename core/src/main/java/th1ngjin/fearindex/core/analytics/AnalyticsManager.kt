package th1ngjin.fearindex.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import th1ngjin.fearindex.core.debug.ScreenshotMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics 이벤트 발송 매니저.
 *
 * iOS [AnalyticsManager.swift]와 동일한 이벤트 이름/파라미터를 사용하므로 Firebase Console
 * 대시보드에서 플랫폼 간 비교가 가능하다. 새로운 이벤트는 반드시 [AnalyticsEvent]에 케이스로
 * 추가하고, iOS 쪽도 동시에 반영해야 한다.
 *
 * **플랫폼 구분**: 한 Firebase 프로젝트를 iOS/Android가 공유하므로 앱 시작 시
 * [setStandardUserProperties]로 `platform`, `app_version`, `build_type`, `language`를
 * 반드시 설정해야 대시보드에서 플랫폼별 Funnel/Audience 분리가 가능하다.
 */
@Singleton
class AnalyticsManager @Inject constructor() {
    private val firebase: FirebaseAnalytics by lazy { Firebase.analytics }

    fun log(event: AnalyticsEvent) {
        if (ScreenshotMode.isEnabled()) return
        val bundle = event.parameters?.toBundle()
        firebase.logEvent(event.name, bundle)
        Timber.tag(TAG).d("event=%s params=%s", event.name, event.parameters ?: "{}")
    }

    fun logScreen(screen: AnalyticsScreen) {
        if (ScreenshotMode.isEnabled()) return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen.screenClass)
        }
        firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Timber.tag(TAG).d("screen=%s", screen.screenName)
    }

    fun setUserProperty(name: String, value: String?) {
        if (ScreenshotMode.isEnabled()) return
        firebase.setUserProperty(name, value)
        Timber.tag(TAG).d("userProperty %s=%s", name, value)
    }

    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        if (ScreenshotMode.isEnabled()) return
        firebase.setAnalyticsCollectionEnabled(enabled)
    }

    /**
     * 앱 시작 시 1회 호출. iOS와 같은 Firebase 프로젝트를 공유하므로 플랫폼 구분을 위한
     * UserProperty를 반드시 심는다. iOS `AnalyticsManager.swift`의 동명 함수와 쌍이 맞아야 한다.
     */
    fun setStandardUserProperties(
        appVersion: String,
        buildType: String,
        language: String,
    ) {
        setUserProperty(UserProperty.PLATFORM, "android")
        setUserProperty(UserProperty.APP_VERSION, appVersion)
        setUserProperty(UserProperty.BUILD_TYPE, buildType)
        setUserProperty(UserProperty.LANGUAGE, language)
    }

    object UserProperty {
        const val PLATFORM = "platform"
        const val APP_VERSION = "app_version"
        const val BUILD_TYPE = "build_type"
        const val LANGUAGE = "language"
    }

    private companion object {
        const val TAG = "Analytics"
    }
}

/**
 * Firebase Bundle 변환 — Map<String, Any> 타입 안전 매핑.
 * 지원: String, Int, Long, Double, Float, Boolean. 그 외는 toString().
 */
private fun Map<String, Any>.toBundle(): Bundle = Bundle().also { bundle ->
    for ((k, v) in this) {
        when (v) {
            is String -> bundle.putString(k, v)
            is Int -> bundle.putInt(k, v)
            is Long -> bundle.putLong(k, v)
            is Double -> bundle.putDouble(k, v)
            is Float -> bundle.putFloat(k, v)
            is Boolean -> bundle.putBoolean(k, v)
            else -> bundle.putString(k, v.toString())
        }
    }
}
