package th1ngjin.fearindex.core.util

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearVelocity
import th1ngjin.fearindex.domain.entity.VelocityTrend
import kotlin.math.abs

/**
 * 공포지수 변화 속도 계산기 — iOS FearVelocityCalculator 1:1 포팅.
 *
 * - daily: 오늘 - 어제
 * - weekly: 오늘 - 7일 전
 * - acceleration: daily 변화량의 변화량
 * - trend: 방향 + 가속/감속 판정
 * - isInflectionPoint: daily와 acceleration 부호가 다르면 true
 */
object FearVelocityCalculator {

    private const val MIN_HISTORY_SIZE = 8

    /**
     * 최소 8일치 히스토리가 필요. 부족하면 null.
     * history는 최신 → 과거 순서 (index 0이 최신).
     */
    fun calculate(history: List<FearIndex>): FearVelocity? {
        if (history.size < MIN_HISTORY_SIZE) return null

        val today = history[0].score
        val yesterday = history[1].score
        val weekAgo = history[minOf(6, history.lastIndex)].score

        val daily = today - yesterday
        val weekly = today - weekAgo

        // acceleration: 오늘의 일일 변화 vs 어제의 일일 변화
        val previousDaily = if (history.size >= 3) {
            history[1].score - history[2].score
        } else {
            0.0
        }
        val acceleration = daily - previousDaily

        val trend = determineTrend(daily, acceleration)
        val isInflection = isInflectionPoint(daily, acceleration)

        return FearVelocity(
            daily = daily,
            weekly = weekly,
            acceleration = acceleration,
            trend = trend,
            isInflectionPoint = isInflection,
        )
    }

    private fun determineTrend(
        daily: Double,
        acceleration: Double,
    ): VelocityTrend {
        val threshold = 0.5
        return when {
            daily < -threshold && acceleration < 0 -> VelocityTrend.CRASH_ACCELERATING
            daily < -threshold && acceleration >= 0 -> VelocityTrend.CRASH_DECELERATING
            daily > threshold && acceleration > 0 -> VelocityTrend.RALLY_ACCELERATING
            daily > threshold && acceleration <= 0 -> VelocityTrend.RALLY_DECELERATING
            else -> VelocityTrend.STABLE
        }
    }

    private fun isInflectionPoint(
        daily: Double,
        acceleration: Double,
    ): Boolean {
        if (abs(daily) < 0.1 || abs(acceleration) < 0.1) return false
        return (daily > 0 && acceleration < 0) || (daily < 0 && acceleration > 0)
    }
}
