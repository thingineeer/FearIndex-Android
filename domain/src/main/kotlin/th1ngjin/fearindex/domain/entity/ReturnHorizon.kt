package th1ngjin.fearindex.domain.entity

/**
 * 수익률 horizon 키 (1M / 3M / 6M / 1Y). iOS `ReturnHorizon` 1:1.
 *
 * `HistoricalReturns` / `HistoricalSampleCounts` 의 4개 필드를 enum 으로 선택하는 SSOT.
 * market/kospi 는 거래일 21/63/126/252, crypto 는 달력일 30/90/180/365 에 대응한다.
 * [analyticsKey] 는 iOS rawValue 와 동일 (analytics 파라미터 공유).
 */
enum class ReturnHorizon(val analyticsKey: String) {
    ONE_MONTH("oneMonth"),
    THREE_MONTH("threeMonth"),
    SIX_MONTH("sixMonth"),
    ONE_YEAR("oneYear"),
    ;

    /** 해당 horizon 의 수익률 값 */
    fun value(returns: HistoricalReturns): Double = when (this) {
        ONE_MONTH -> returns.oneMonth
        THREE_MONTH -> returns.threeMonth
        SIX_MONTH -> returns.sixMonth
        ONE_YEAR -> returns.oneYear
    }

    /** 해당 horizon 의 표본 수 */
    fun count(counts: HistoricalSampleCounts): Int = when (this) {
        ONE_MONTH -> counts.oneMonth
        THREE_MONTH -> counts.threeMonth
        SIX_MONTH -> counts.sixMonth
        ONE_YEAR -> counts.oneYear
    }
}
