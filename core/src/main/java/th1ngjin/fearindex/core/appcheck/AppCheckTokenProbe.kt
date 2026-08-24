package th1ngjin.fearindex.core.appcheck

import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** App Check 토큰을 선취득하지 못했을 때 — 서버 호출은 확정 401 이므로 호출 전에 끊는다. */
class AppCheckUnavailableException(
    val kind: AppCheckFailureKind,
    cause: Throwable,
) : Exception("App Check token unavailable ($kind): ${cause.message}", cause)

/**
 * App Check 토큰 선취득 + 실패 사유 로깅.
 *
 * Functions SDK 는 토큰 획득 실패를 삼키고 토큰 없이 요청한다 → 서버 401 만 남고 원인이 사라진다.
 * 보호 Callable 호출 전에 이 프로브로 토큰을 먼저 받아 (1) 실패 사유를 `AppCheckFailureKind` 로
 * 분류해 WARN 로그(release 는 Crashlytics non-fatal)로 남기고 (2) 확정 실패 호출은 보내지 않는다.
 */
@Singleton
open class AppCheckTokenProbe @Inject constructor() {

    /** 성공 시 정상 반환, 실패 시 [AppCheckUnavailableException]. */
    open suspend fun ensureToken() {
        try {
            Firebase.appCheck.getAppCheckToken(false).await()
        } catch (e: Exception) {
            val kind = AppCheckFailureClassifier.classify(e.message)
            Timber.w(
                AppCheckUnavailableException(kind, e),
                "[AppCheck] token fetch failed kind=%s error=%s: %s",
                kind, e.javaClass.simpleName, e.message,
            )
            throw AppCheckUnavailableException(kind, e)
        }
    }
}
