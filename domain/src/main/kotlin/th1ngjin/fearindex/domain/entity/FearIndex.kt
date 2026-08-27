package th1ngjin.fearindex.domain.entity

import java.time.Instant
import kotlin.math.roundToInt

data class FearIndex(
    val score: Double,
    val rating: Rating,
    val timestamp: Instant,
    val previousClose: Double? = null,
    val previous1Week: Double? = null,
    val previous1Month: Double? = null,
    val previous1Year: Double? = null,
) {
    val roundedScore: Int get() = score.roundToInt()

    enum class Rating {
        EXTREME_FEAR,
        FEAR,
        NEUTRAL,
        GREED,
        EXTREME_GREED;

        companion object {
            fun from(score: Double): Rating = when {
                score < 25 -> EXTREME_FEAR
                score < 45 -> FEAR
                // 경계값은 윗 밴드 귀속 — iOS(..<55)·서버(<55)와 동일 (55.0=GREED, 75.0=EXTREME_GREED)
                score < 55 -> NEUTRAL
                score < 75 -> GREED
                else -> EXTREME_GREED
            }
        }
    }
}

enum class FearIndexType(val serverName: String) {
    MARKET("market"),
    KOSPI("kospi"),
    CRYPTO("crypto"),
}
