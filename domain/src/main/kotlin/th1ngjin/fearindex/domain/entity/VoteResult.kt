package th1ngjin.fearindex.domain.entity

/**
 * Buy/Hold/Sell 투표 집계 결과
 */
data class VoteResult(
    val buyCount: Int,
    val holdCount: Int,
    val sellCount: Int,
    val totalCount: Int,
    val myVote: VoteChoice?,
) {
    val buyPercentage: Double
        get() = if (totalCount > 0) buyCount * 100.0 / totalCount else 0.0

    val holdPercentage: Double
        get() = if (totalCount > 0) holdCount * 100.0 / totalCount else 0.0

    val sellPercentage: Double
        get() = if (totalCount > 0) sellCount * 100.0 / totalCount else 0.0

    companion object {
        val EMPTY = VoteResult(
            buyCount = 0,
            holdCount = 0,
            sellCount = 0,
            totalCount = 0,
            myVote = null,
        )
    }
}
