package th1ngjin.fearindex.domain.entity

data class ReturnDataPoint(
    val score: Int,
    val returns: HistoricalReturns,
    val worstCase: HistoricalReturns,
    val bestCase: HistoricalReturns,
    val sampleCount: Int,
    /**
     * horizon 별 표본 수 (v1.9.4). 서버 `horizonCounts` 가 없으면 [sampleCount] 를 모든 horizon 에 적용.
     */
    val horizonSampleCounts: HistoricalSampleCounts = HistoricalSampleCounts.same(sampleCount),
)
