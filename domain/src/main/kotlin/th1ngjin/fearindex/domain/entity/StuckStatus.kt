package th1ngjin.fearindex.domain.entity

/**
 * 물림 상태 (영속 토글)
 *
 * - STUCK: "물렸어요"
 * - SAFE: "안 물렸어요"
 * - NONE: 무응답 (초기 상태 또는 응답 취소)
 *
 * 서버 전송 값(serverValue)은 stuck-counter Cloud Functions 스펙과 일치한다.
 */
enum class StuckStatus(val serverValue: String) {
    STUCK("stuck"),
    SAFE("safe"),
    NONE("none");

    companion object {
        fun fromServer(raw: String?): StuckStatus = when (raw) {
            "stuck" -> STUCK
            "safe" -> SAFE
            else -> NONE
        }
    }
}
