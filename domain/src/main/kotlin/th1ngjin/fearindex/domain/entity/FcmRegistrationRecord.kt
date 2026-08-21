package th1ngjin.fearindex.domain.entity

/** 마지막으로 서버에 성공적으로 등록한 FCM 토큰/설정 스냅샷 (토큰은 해시만 보관). */
data class FcmRegistrationRecord(
    val tokenHash: String,
    val settingsHash: Int,
    val buildNumber: String,
    val registeredAtMillis: Long,
)
