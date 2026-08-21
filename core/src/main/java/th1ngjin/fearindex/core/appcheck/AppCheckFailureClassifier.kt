package th1ngjin.fearindex.core.appcheck

/**
 * App Check 토큰 발급 실패 사유 분류.
 *
 * Functions SDK 는 App Check 토큰 획득에 실패하면 토큰 없이 요청을 보내고 서버는 401 `Unauthenticated`
 * 만 돌려주므로, 클라이언트가 직접 원인을 남기지 않으면 "왜 실패했는지"를 끝내 알 수 없다
 * (2026-08-21: 두 달간 Android 프로덕션 전수 실패를 Firebase 등록 인증서 불일치로 특정하기까지 그랬다).
 */
enum class AppCheckFailureKind {
    /** App Check 백엔드가 attestation 을 거부(403) — 등록 인증서/패키지 불일치, 미등록 앱, 사이드로드 등. */
    ATTESTATION_REJECTED,

    /** Play Integrity API 자체를 쓸 수 없음 — Play 스토어/서비스 부재·구버전, API 미지원. */
    PLAY_INTEGRITY_UNAVAILABLE,

    /** Play Integrity 호출 제한(TOO_MANY_REQUESTS). */
    THROTTLED,

    /** 네트워크/Google 서버 일시 장애 — 재시도로 회복 가능. */
    NETWORK,

    UNKNOWN,
}

object AppCheckFailureClassifier {

    private val networkMarkers = listOf(
        "NETWORK_ERROR", "GOOGLE_SERVER_UNAVAILABLE", "Unable to resolve host",
        "timeout", "timed out", "Software caused connection abort", "ECONNRESET", "UnknownHost",
    )
    private val unavailableMarkers = listOf(
        "PLAY_STORE_NOT_FOUND", "API_NOT_AVAILABLE", "PLAY_SERVICES_NOT_FOUND",
        "PLAY_SERVICES_VERSION_OUTDATED", "PLAY_STORE_VERSION_OUTDATED",
        "PLAY_STORE_ACCOUNT_NOT_FOUND", "CANNOT_BIND_TO_SERVICE", "APP_NOT_INSTALLED",
    )

    fun classify(message: String?): AppCheckFailureKind {
        if (message.isNullOrBlank()) return AppCheckFailureKind.UNKNOWN
        return when {
            message.contains("TOO_MANY_REQUESTS", ignoreCase = true) -> AppCheckFailureKind.THROTTLED
            networkMarkers.any { message.contains(it, ignoreCase = true) } -> AppCheckFailureKind.NETWORK
            unavailableMarkers.any { message.contains(it, ignoreCase = true) } ->
                AppCheckFailureKind.PLAY_INTEGRITY_UNAVAILABLE
            message.contains("attestation", ignoreCase = true) ||
                message.contains("code: 403", ignoreCase = true) -> AppCheckFailureKind.ATTESTATION_REJECTED
            else -> AppCheckFailureKind.UNKNOWN
        }
    }
}
