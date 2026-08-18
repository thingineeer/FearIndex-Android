package th1ngjin.fearindex.variant

import android.content.Context
import androidx.compose.runtime.Composable
import th1ngjin.fearindex.core.crash.CrashReporter
import th1ngjin.fearindex.core.purchases.PurchaseManager
import timber.log.Timber

/**
 * 빌드 변형별 훅 — release: 전부 no-op. (debug 소스셋의 동명 object 가 개발자 도구를 주입한다.)
 * release 빌드에는 개발자 결제 테스트 관련 심볼이 존재하지 않는다.
 */
object VariantHooks {
    fun onApplicationCreate(context: Context, purchaseManager: PurchaseManager) = Unit

    /** release: WARN 이상 Timber 로그를 Crashlytics(로그 + non-fatal)로 — Firebase 초기화 후 1회. */
    fun plantLogging(crashReporter: CrashReporter) {
        Timber.plant(CrashlyticsTree(crashReporter))
    }

    /** 설정 화면 하단 debug 섹션 — release 없음. */
    fun settingsDebugSection(purchaseManager: PurchaseManager): (@Composable () -> Unit)? = null
}
