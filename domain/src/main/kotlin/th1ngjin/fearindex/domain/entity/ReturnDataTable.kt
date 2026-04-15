package th1ngjin.fearindex.domain.entity

import java.time.Instant

data class ReturnDataTable(
    val version: Int,
    val updatedAt: Instant,
    val dataPoints: List<ReturnDataPoint>,
    val historicalEvents: List<ReturnEventEntry>,
)
