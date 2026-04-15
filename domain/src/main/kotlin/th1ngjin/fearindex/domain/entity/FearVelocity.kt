package th1ngjin.fearindex.domain.entity

/**
 * 공포지수 변화 속도 — iOS FearVelocity 1:1 매핑.
 */
data class FearVelocity(
    val daily: Double,
    val weekly: Double,
    val acceleration: Double,
    val trend: VelocityTrend,
    val isInflectionPoint: Boolean,
)

enum class VelocityTrend {
    CRASH_ACCELERATING,
    CRASH_DECELERATING,
    STABLE,
    RALLY_ACCELERATING,
    RALLY_DECELERATING,
}
