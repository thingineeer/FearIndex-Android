package th1ngjin.fearindex.presentation.feature.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.presentation.feature.history.NotificationHistoryLayout.DayLabel
import th1ngjin.fearindex.presentation.feature.history.NotificationHistoryLayout.Row
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** iOS `NotificationHistoryLayoutTests` 1:1 포팅 (배너 위치 / interleave / 날짜 섹션 / 날짜 라벨). */
class NotificationHistoryLayoutTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    /** 2027-01-15 12:00 KST */
    private val now = ZonedDateTime.of(2027, 1, 15, 12, 0, 0, 0, zone).toInstant()

    private fun record(id: String, hoursAgo: Double) = NotificationRecord(
        id = id,
        kind = NotificationKind.MARKET,
        title = "t",
        body = "b",
        score = 20,
        receivedAt = now.minus(Duration.ofMinutes((hoursAgo * 60).toLong())),
    )

    private fun records(count: Int) = (0 until count).map { record("r$it", it.toDouble()) }

    // MARK: - Banner positions

    @Test
    fun `bannerPositions - 3번째 뒤, 이후 7개마다`() {
        assertEquals(emptyList<Int>(), NotificationHistoryLayout.bannerPositions(0))
        assertEquals(emptyList<Int>(), NotificationHistoryLayout.bannerPositions(2))
        assertEquals(listOf(3), NotificationHistoryLayout.bannerPositions(3))
        assertEquals(listOf(3), NotificationHistoryLayout.bannerPositions(9))
        assertEquals(listOf(3, 10), NotificationHistoryLayout.bannerPositions(10))
        assertEquals(listOf(3, 10, 17), NotificationHistoryLayout.bannerPositions(17))
        assertEquals(listOf(3, 10, 17), NotificationHistoryLayout.bannerPositions(20))
        assertEquals(listOf(3, 10, 17, 24), NotificationHistoryLayout.bannerPositions(24))
    }

    @Test
    fun `rows(includeAds) - 레코드 순서 유지 + 배너가 3, 10번째 레코드 뒤에 삽입`() {
        val rows = NotificationHistoryLayout.rows(records(12), includeAds = true)
        assertEquals(14, rows.size)
        assertEquals(Row.Banner(position = 3), rows[3])
        assertEquals(Row.Banner(position = 10), rows[11])
        val recordIds = rows.filterIsInstance<Row.Record>().map { it.record.id }
        assertEquals(records(12).map { it.id }, recordIds)
        assertEquals("history-banner-3", rows[3].id)
        assertEquals("r0", rows[0].id)
    }

    @Test
    fun `rows(includeAds=false) - 프리미엄은 배너 없이 레코드만`() {
        val rows = NotificationHistoryLayout.rows(records(12), includeAds = false)
        assertEquals(12, rows.size)
        assertTrue(rows.all { it is Row.Record })
    }

    // MARK: - Sections

    @Test
    fun `sections - 자정 기준 날짜별 그룹, 배너는 직전 레코드 섹션에 귀속`() {
        val input = listOf(
            record("a", 1.0),   // 오늘 11:00
            record("b", 2.0),   // 오늘 10:00
            record("c", 13.0),  // 어제 23:00
            record("d", 40.0),  // 그저께
        )
        val rows = NotificationHistoryLayout.rows(input, includeAds = true) // 배너는 c(3번째) 뒤
        val sections = NotificationHistoryLayout.sections(rows, zone)
        assertEquals(3, sections.size)
        assertEquals(listOf("a", "b"), sections[0].rows.map { it.id })
        assertEquals(listOf("c", "history-banner-3"), sections[1].rows.map { it.id })
        assertEquals(listOf("d"), sections[2].rows.map { it.id })
        assertEquals(LocalDate.of(2027, 1, 15), sections[0].day)
        assertEquals(LocalDate.of(2027, 1, 14), sections[1].day)
        assertEquals(LocalDate.of(2027, 1, 13), sections[2].day)
    }

    @Test
    fun `sections - 빈 입력은 빈 섹션, 레코드 없이는 배너 삽입 안 됨`() {
        assertTrue(NotificationHistoryLayout.sections(emptyList(), zone).isEmpty())
        assertTrue(NotificationHistoryLayout.rows(emptyList(), includeAds = true).isEmpty())
    }

    // MARK: - Day label

    @Test
    fun `dayLabel - 오늘, 어제, 그 외 날짜`() {
        val today = LocalDate.of(2027, 1, 15)
        val yesterday = today.minusDays(1)
        val older = today.minusDays(2)
        assertEquals(DayLabel.Today, NotificationHistoryLayout.dayLabel(today, today))
        assertEquals(DayLabel.Yesterday, NotificationHistoryLayout.dayLabel(yesterday, today))
        assertEquals(DayLabel.Date(older), NotificationHistoryLayout.dayLabel(older, today))
    }
}
