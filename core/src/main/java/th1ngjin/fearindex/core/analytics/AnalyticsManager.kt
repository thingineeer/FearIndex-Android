package th1ngjin.fearindex.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
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
 * 호출 예:
 * ```
 * @Inject lateinit var analytics: AnalyticsManager
 * analytics.log(AnalyticsEvent.탭선택(탭이름 = "홈"))
 * analytics.logScreen(AnalyticsScreen.홈)
 * ```
 */
@Singleton
class AnalyticsManager @Inject constructor() {
    private val firebase: FirebaseAnalytics = Firebase.analytics

    fun log(event: AnalyticsEvent) {
        val bundle = event.parameters?.toBundle()
        firebase.logEvent(event.name, bundle)
        Timber.tag("Analytics").d("event=%s params=%s", event.name, event.parameters ?: "{}")
    }

    fun logScreen(screen: AnalyticsScreen) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen.screenClass)
        }
        firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Timber.tag("Analytics").d("screen=%s", screen.screenName)
    }

    fun setUserProperty(name: String, value: String?) {
        firebase.setUserProperty(name, value)
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
