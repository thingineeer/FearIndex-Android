package th1ngjin.fearindex.variant

import android.util.Log
import th1ngjin.fearindex.core.crash.CrashReporter
import timber.log.Timber

/**
 * release 전용 Timber tree — WARN 이상을 Crashlytics 로그로, Throwable 이 붙은 WARN/ERROR 는
 * non-fatal 로 기록한다. 프리미엄 경로(수익률 fallback·알림내역 read/rewrite 실패 등)의
 * `Timber.w(t, ...)` 가 release 에서도 Firebase 에서 보이도록 하는 목적.
 * DEBUG/INFO 는 무시(노이즈·PII 방지).
 */
class CrashlyticsTree(private val reporter: CrashReporter) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = if (priority >= Log.ERROR) "E" else "W"
        reporter.log("$level/${tag ?: "App"}: $message")
        if (t != null) reporter.recordException(t)
    }
}
