package th1ngjin.fearindex.domain.entity

import java.time.Instant

data class ReturnEventEntry(
    val id: String,
    val date: Instant,
    val score: Int,
    val descriptionKey: String,
    val returnAfter: HistoricalReturns? = null,
)
