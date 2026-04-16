package th1ngjin.fearindex.core.util

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearVelocity
import th1ngjin.fearindex.domain.entity.VelocityTrend
import kotlin.math.min

/**
 * 공포지수 변화 속도 계산기 — iOS FearVelocityCalculator 1:1 포팅 (v1.7.9).
 *
 * v1.7.9 수정: classifyTrend에 weekly 체크 추가 (daily OR weekly).
 * 임계값: daily >= 5.0 OR weekly >= 10.0 이면 rally/crash로 분류.
 */
object FearVelocityCalculator {

    private const val MIN_HISTORY_SIZE = 8
    private const val VELOCITY_THRESHOLD = 5.0
    private const val VELOCITY_WEEKLY_THRESHOLD = 10.0
    private const val INFLECTION_MIN_DAYS = 3

    /**
     * 최소 8일치 히스토리가 필요. 부족하면 null.
     * history는 최신 → 과거 순서 (index 0이 최신).
     */
    fun calculate(history: List<FearIndex>): FearVelocity? {
        if (history.size < MIN_HISTORY_SIZE) return null

        val sorted = history.sortedByDescending { it.timestamp }
        val daily = computeDaily(sorted)
        val weekly = computeWeekly(sorted)
        val acceleration = computeAcceleration(sorted)
        val trend = classifyTrend(daily, weekly, acceleration)
        val inflection = detectInflection(sorted)

        return FearVelocity(
            daily = daily,
            weekly = weekly,
            acceleration = acceleration,
            trend = trend,
            isInflectionPoint = inflection,
        )
    }

    private fun computeDaily(sorted: List<FearIndex>): Double =
        sorted[0].score - sorted[1].score

    private fun computeWeekly(sorted: List<FearIndex>): Double {
        if (sorted.size < 8) return 0.0
        return sorted[0].score - sorted[7].score
    }

    private fun computeAcceleration(sorted: List<FearIndex>): Double {
        if (sorted.size < 3) return 0.0
        val todayV = sorted[0].score - sorted[1].score
        val yesterdayV = sorted[1].score - sorted[2].score
        return todayV - yesterdayV
    }

    /**
     * iOS v1.7.9 수정: daily OR weekly 중 하나라도 임계값 넘으면 rally/crash.
     * 양쪽이 반대 방향이면 recent(daily) 우선.
     */
    internal fun classifyTrend(
        daily: Double,
        weekly: Double,
        acceleration: Double,
    ): VelocityTrend {
        val isDailyCrash = daily < -VELOCITY_THRESHOLD
        val isDailyRally = daily > VELOCITY_THRESHOLD
        val isWeeklyCrash = weekly < -VELOCITY_WEEKLY_THRESHOLD
        val isWeeklyRally = weekly > VELOCITY_WEEKLY_THRESHOLD

        val isCrash = isDailyCrash || isWeeklyCrash
        val isRally = isDailyRally || isWeeklyRally

        if (isCrash && isRally) {
            return if (isDailyCrash) {
                if (acceleration < 0) VelocityTrend.CRASH_ACCELERATING else VelocityTrend.CRASH_DECELERATING
            } else {
                if (acceleration > 0) VelocityTrend.RALLY_ACCELERATING else VelocityTrend.RALLY_DECELERATING
            }
        }
        if (isCrash) return if (acceleration < 0) VelocityTrend.CRASH_ACCELERATING else VelocityTrend.CRASH_DECELERATING
        if (isRally) return if (acceleration > 0) VelocityTrend.RALLY_ACCELERATING else VelocityTrend.RALLY_DECELERATING
        return VelocityTrend.STABLE
    }

    /**
     * iOS detectInflection 포팅: 최근 daily가 이전 INFLECTION_MIN_DAYS 일과
     * 반대 부호이면 변곡점.
     */
    private fun detectInflection(sorted: List<FearIndex>): Boolean {
        if (sorted.size < INFLECTION_MIN_DAYS + 1) return false
        return detectSignReversal(sorted)
    }

    private fun detectSignReversal(sorted: List<FearIndex>): Boolean {
        val latestDelta = sorted[0].score - sorted[1].score
        var consecutiveOpposite = 0
        for (i in 1 until min(sorted.size - 1, INFLECTION_MIN_DAYS + 1)) {
            val delta = sorted[i].score - sorted[i + 1].score
            if ((latestDelta > 0 && delta < 0) || (latestDelta < 0 && delta > 0)) {
                consecutiveOpposite++
            }
        }
        return consecutiveOpposite >= INFLECTION_MIN_DAYS
    }
}
