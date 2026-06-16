package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import java.time.Instant

/**
 * 현재 지수 업데이트 시각 포맷 검증.
 *
 * iOS `ChartDateFormatter.timestamp(for:dateContext:)` 와 1:1 대응:
 * - 포맷 `yyyy.MM.dd HH:mm`
 * - 타임존은 indexType별 (market=뉴욕, kospi=서울, crypto=UTC)
 */
class IndexTimestampFormatterTest {

    // 2026-06-16 06:30:00 UTC 고정 시각
    private val instant: Instant = Instant.parse("2026-06-16T06:30:00Z")

    @Test
    fun `crypto 는 UTC 로 포맷`() {
        assertEquals(
            "2026.06.16 06:30",
            IndexTimestampFormatter.format(instant, FearIndexType.CRYPTO),
        )
    }

    @Test
    fun `kospi 는 서울 시각으로 포맷 (UTC+9)`() {
        // 06:30 UTC → 15:30 KST
        assertEquals(
            "2026.06.16 15:30",
            IndexTimestampFormatter.format(instant, FearIndexType.KOSPI),
        )
    }

    @Test
    fun `market 은 뉴욕 시각으로 포맷 (여름 EDT UTC-4)`() {
        // 2026-06-16 06:30 UTC → 02:30 EDT (서머타임)
        assertEquals(
            "2026.06.16 02:30",
            IndexTimestampFormatter.format(instant, FearIndexType.MARKET),
        )
    }

    @Test
    fun `자정 경계 - market 뉴욕은 전날로 넘어감`() {
        // 2026-06-16 02:00 UTC → 2026-06-15 22:00 EDT
        val i = Instant.parse("2026-06-16T02:00:00Z")
        assertEquals(
            "2026.06.15 22:00",
            IndexTimestampFormatter.format(i, FearIndexType.MARKET),
        )
    }

    @Test
    fun `서양 숫자 포맷 고정 (Locale 무관)`() {
        // 어떤 시스템 locale 에서도 아라비아 숫자 + '.' 구분자
        val result = IndexTimestampFormatter.format(instant, FearIndexType.KOSPI)
        assertEquals("2026.06.16 15:30", result)
    }
}
