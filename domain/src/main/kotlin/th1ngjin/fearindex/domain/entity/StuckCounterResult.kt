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

    /**
     * 디바운스(서버 호출 1.5초 지연) 동안 UI 일관성 유지용 낙관적 갱신.
     * - 이전 myStatus → 새 newStatus 로의 delta만큼 stuck/safe 카운트 ±1
     * - totalResponded는 NONE↔STUCK/SAFE 변환 시에만 ±1 (STUCK↔SAFE 전환은 유지)
     * - percentage는 새 카운트로 재계산
     */
    fun withOptimisticToggle(newStatus: StuckStatus): StuckCounterResult {
        val prev = myStatus
        if (prev == newStatus) return this
        var newStuck = stuckCount
        var newSafe = safeCount
        var newTotal = totalResponded

        when (prev) {
            StuckStatus.STUCK -> newStuck -= 1
            StuckStatus.SAFE -> newSafe -= 1
            StuckStatus.NONE -> newTotal += 1
        }
        when (newStatus) {
            StuckStatus.STUCK -> newStuck += 1
            StuckStatus.SAFE -> newSafe += 1
            StuckStatus.NONE -> newTotal -= 1
        }
        newStuck = newStuck.coerceAtLeast(0)
        newSafe = newSafe.coerceAtLeast(0)
        newTotal = newTotal.coerceAtLeast(0)
        val totalForPct = (newStuck + newSafe).coerceAtLeast(0)
        val newStuckPct = if (totalForPct > 0) newStuck * 100.0 / totalForPct else 0.0
        val newSafePct = if (totalForPct > 0) newSafe * 100.0 / totalForPct else 0.0

        return copy(
            stuckCount = newStuck,
            safeCount = newSafe,
            totalResponded = newTotal,
            stuckPercentage = newStuckPct.coerceIn(0.0, 100.0),
            safePercentage = newSafePct.coerceIn(0.0, 100.0),
            myStatus = newStatus,
        )
    }

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
