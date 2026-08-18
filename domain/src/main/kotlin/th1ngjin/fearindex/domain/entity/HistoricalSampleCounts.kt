package th1ngjin.fearindex.domain.entity

/**
 * horizon 별 표본 수. iOS `HistoricalSampleCounts` 1:1.
 *
 * 긴 horizon 일수록 미래 가격이 아직 없어 표본이 줄어든다 (예: 1M 34 / 1Y 32).
 */
data class HistoricalSampleCounts(
    val oneMonth: Int,
    val threeMonth: Int,
    val sixMonth: Int,
    val oneYear: Int,
) {
    companion object {
        /** 레거시 데이터(horizon 별 표본 수 없음) 호환 — 모든 horizon 에 [count] 적용 */
        fun same(count: Int): HistoricalSampleCounts =
            HistoricalSampleCounts(oneMonth = count, threeMonth = count, sixMonth = count, oneYear = count)
    }
}
