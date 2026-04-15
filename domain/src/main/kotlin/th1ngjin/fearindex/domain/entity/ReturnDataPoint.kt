package th1ngjin.fearindex.domain.entity

data class ReturnDataPoint(
    val score: Int,
    val returns: HistoricalReturns,
    val worstCase: HistoricalReturns,
    val bestCase: HistoricalReturns,
    val sampleCount: Int,
)
