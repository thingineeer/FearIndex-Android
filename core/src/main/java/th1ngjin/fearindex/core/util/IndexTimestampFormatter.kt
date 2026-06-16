package th1ngjin.fearindex.core.util

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.dateContext
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.util.Locale

/**
 * 현재 지수 업데이트 시각 포맷터.
 *
 * iOS `ChartDateFormatter.timestamp(for:dateContext:)` 와 1:1 대응:
 * - 포맷 `yyyy.MM.dd HH:mm`
 * - 타임존은 indexType별 (market=America/New_York, kospi=Asia/Seoul, crypto=UTC)
 * - 서양 숫자 강제 (금융 앱 표준, Locale.US)
 */
object IndexTimestampFormatter {

    private val pattern: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.US)

    fun format(instant: Instant, type: FearIndexType): String =
        pattern.withZone(type.dateContext.zoneId).format(instant)
}
