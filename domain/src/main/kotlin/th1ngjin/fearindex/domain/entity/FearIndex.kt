package th1ngjin.fearindex.domain.entity

import java.time.Instant

data class FearIndex(
    val score: Double,
    val rating: Rating,
    val timestamp: Instant,
    val previousClose: Double? = null,
    val previous1Week: Double? = null,
    val previous1Month: Double? = null,
    val previous1Year: Double? = null,
) {
    val roundedScore: Int get() = score.toInt()

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
                score <= 55 -> NEUTRAL
                score <= 75 -> GREED
                else -> EXTREME_GREED
            }
        }
    }
}

enum class FearIndexType { MARKET, CRYPTO }
