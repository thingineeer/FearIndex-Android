package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 닫힌 시각 구간 (start <= end). iOS `DateInterval` 대응.
 */
data class DateRange(
    val start: Instant,
    val end: Instant,
) {
    init {
        require(!start.isAfter(end)) { "DateRange start($start) must be <= end($end)" }
    }
}
