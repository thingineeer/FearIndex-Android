package th1ngjin.fearindex.domain.entity

/**
 * 물림 카운터 집계 결과
 *
 * **절대 수치(count)와 비율(percentage)을 분리** 저장한다.
 * 서버는 totalResponded < 100이면 stuckCount/safeCount를 0으로 마스킹해 내려보내지만
 * percentage는 항상 실제 값. 이 분리 덕에 초기 유저도 게이지는 정확히 보면서
 * "X명 응답" 숫자는 숨길 수 있다.
 */
data class StuckCounterResult(
    val stuckCount: Int,
    val safeCount: Int,
    val totalResponded: Int,
    val stuckPercentage: Double,   // 0.0 ~ 100.0
    val safePercentage: Double,    // 0.0 ~ 100.0
    val myStatus: StuckStatus,
) {
    /** 절대 수치 노출 여부 (초기 공허함 방지 — 100명 미만이면 숨김) */
    val shouldShowAbsoluteCount: Boolean get() = totalResponded >= 100

    companion object {
        val EMPTY = StuckCounterResult(
            stuckCount = 0,
            safeCount = 0,
            totalResponded = 0,
            stuckPercentage = 0.0,
            safePercentage = 0.0,
            myStatus = StuckStatus.NONE,
        )
    }
}
