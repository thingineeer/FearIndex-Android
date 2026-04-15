package th1ngjin.fearindex.domain.entity

/**
 * 과거 유사 시점 이벤트 — iOS HistoricalEvent 1:1 매핑.
 */
data class HistoricalEvent(
    val date: String,
    val score: Int,
    val description: String,
    val returnAfter1M: Double?,
    val returnAfter3M: Double?,
    val returnAfter6M: Double?,
    val returnAfter1Y: Double?,
)
