package th1ngjin.fearindex.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import java.time.Instant

/** iOS `NotificationHistoryPolicyTests` 포팅 + Android 전용 `upsert`(fallback id 승격) 규칙. */
class NotificationHistoryPolicyTest {

    private val now: Instant = Instant.ofEpochSecond(1_800_000_000)

    private fun record(
        id: String,
        kind: NotificationKind = NotificationKind.KOSPI,
        daysAgo: Double,
        title: String = "t",
        body: String = "b",
    ) = NotificationRecord(
        id = id,
        kind = kind,
        title = title,
        body = body,
        score = 20,
        receivedAt = now.minusMillis((daysAgo * 86_400_000).toLong()),
    )

    // MARK: retention

    @Test
    fun `retentionDays - 무료 30일, 프리미엄 무제한(null)`() {
        assertEquals(30, NotificationHistoryPolicy.retentionDays(isPremium = false))
        assertNull(NotificationHistoryPolicy.retentionDays(isPremium = true))
        assertEquals(30, NotificationHistoryPolicy.FREE_RETENTION_DAYS)
        assertEquals(5000, NotificationHistoryPolicy.HARD_CAP)
    }

    // MARK: prune

    @Test
    fun `prune - 무료는 30일 초과분 제거 + 최신순`() {
        val records = listOf(
            record(id = "b", daysAgo = 29.9),
            record(id = "d", daysAgo = 180.0),
            record(id = "a", daysAgo = 1.0),
            record(id = "c", daysAgo = 30.1),
        )
        val kept = NotificationHistoryPolicy.prune(records, isPremium = false, now = now)
        assertEquals(listOf("a", "b"), kept.map { it.id })
    }

    @Test
    fun `prune - 프리미엄은 기간 제거 없음 + 최신순 정렬`() {
        val records = listOf(record(id = "old", daysAgo = 300.0), record(id = "new", daysAgo = 0.5))
        val kept = NotificationHistoryPolicy.prune(records, isPremium = true, now = now)
        assertEquals(listOf("new", "old"), kept.map { it.id })
    }

    @Test
    fun `prune - 하드캡 5000 초과 시 최신순으로 자름 (프리미엄 포함)`() {
        val records = (0 until NotificationHistoryPolicy.HARD_CAP + 10).map {
            record(id = "r$it", daysAgo = it / 1_000.0)
        }
        val kept = NotificationHistoryPolicy.prune(records, isPremium = true, now = now)
        assertEquals(NotificationHistoryPolicy.HARD_CAP, kept.size)
        assertEquals("r0", kept.first().id)
        assertEquals("r${NotificationHistoryPolicy.HARD_CAP - 1}", kept.last().id)
    }

    @Test
    fun `prune - 정확히 cutoff 시각은 보관 (경계 포함)`() {
        val records = listOf(record(id = "edge", daysAgo = 30.0))
        val kept = NotificationHistoryPolicy.prune(records, isPremium = false, now = now)
        assertEquals(listOf("edge"), kept.map { it.id })
    }

    @Test
    fun `prune - 빈 입력은 빈 출력`() {
        assertTrue(NotificationHistoryPolicy.prune(emptyList(), isPremium = false, now = now).isEmpty())
    }

    // MARK: merge

    @Test
    fun `merge - id dedup은 base가 incoming을 이기고 최신순 정렬`() {
        val base = listOf(record(id = "x", daysAgo = 1.0))
        val incoming = listOf(
            NotificationRecord("x", NotificationKind.MARKET, "dup", "dup", 99, now),
            record(id = "y", daysAgo = 0.1),
        )
        val merged = NotificationHistoryPolicy.merge(base, incoming)
        assertEquals(listOf("y", "x"), merged.map { it.id })
        assertEquals(NotificationKind.KOSPI, merged.first { it.id == "x" }.kind)
    }

    @Test
    fun `merge - base 내부 중복 줄도 1건으로 정리`() {
        val base = listOf(record(id = "dup", daysAgo = 1.0), record(id = "dup", daysAgo = 1.0), record(id = "solo", daysAgo = 0.5))
        val merged = NotificationHistoryPolicy.merge(base, emptyList())
        assertEquals(listOf("solo", "dup"), merged.map { it.id })
    }

    // MARK: upsert

    @Test
    fun `upsert - 새 id는 추가되고 결과는 최신순`() {
        val existing = listOf(record(id = "old", daysAgo = 2.0))
        val result = NotificationHistoryPolicy.upsert(existing, record(id = "new", daysAgo = 0.5))
        assertEquals(listOf("new", "old"), result.map { it.id })
    }

    @Test
    fun `upsert - 같은 id가 이미 있으면 그대로 반환 (dedup, 변경 없음)`() {
        val existing = listOf(record(id = "a", daysAgo = 1.0), record(id = "b", daysAgo = 0.5))
        val result = NotificationHistoryPolicy.upsert(existing, record(id = "a", daysAgo = 0.0, title = "changed"))
        assertSame(existing, result)
    }

    @Test
    fun `upsert - fallback id 레코드는 같은 제목 본문의 실제 message id 레코드(120초 이내)로 승격된다`() {
        val fallbackAt = now.minusSeconds(60)
        val fallback = NotificationRecord(
            id = NotificationRecord.fallbackId(NotificationKind.KOSPI, fallbackAt),
            kind = NotificationKind.KOSPI, title = "코스피 공포", body = "지수 18", score = 18, receivedAt = fallbackAt,
        )
        val other = record(id = "other", daysAgo = 3.0)
        val real = NotificationRecord("0:msg%real", NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, now)

        val result = NotificationHistoryPolicy.upsert(listOf(other, fallback), real)

        assertEquals(listOf("0:msg%real", "other"), result.map { it.id })
        assertEquals(now, result.first().receivedAt)
    }

    @Test
    fun `upsert - 120초를 넘거나 제목 본문이 다르면 승격 없이 별도 추가`() {
        val fallbackAt = now.minusSeconds(121)
        val fallback = NotificationRecord(
            id = NotificationRecord.fallbackId(NotificationKind.KOSPI, fallbackAt),
            kind = NotificationKind.KOSPI, title = "코스피 공포", body = "지수 18", score = 18, receivedAt = fallbackAt,
        )
        val tooLate = NotificationRecord("late", NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, now)
        assertEquals(2, NotificationHistoryPolicy.upsert(listOf(fallback), tooLate).size)

        val nearFallback = fallback.copy(
            id = NotificationRecord.fallbackId(NotificationKind.KOSPI, now.minusSeconds(10)),
            receivedAt = now.minusSeconds(10),
        )
        val differentBody = NotificationRecord("diff", NotificationKind.KOSPI, "코스피 공포", "지수 19", 19, now)
        assertEquals(2, NotificationHistoryPolicy.upsert(listOf(nearFallback), differentBody).size)
    }

    @Test
    fun `upsert - incoming 자체가 fallback id면 승격 대상이 아니라 그냥 추가`() {
        val fallbackAt = now.minusSeconds(10)
        val fallback = NotificationRecord(
            NotificationRecord.fallbackId(NotificationKind.KOSPI, fallbackAt),
            NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, fallbackAt,
        )
        val anotherFallback = NotificationRecord(
            NotificationRecord.fallbackId(NotificationKind.KOSPI, now),
            NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, now,
        )
        val result = NotificationHistoryPolicy.upsert(listOf(fallback), anotherFallback)
        assertEquals(2, result.size)
    }

    // MARK: unread

    @Test
    fun `hasUnread - 마지막 확인 이후 수신이 있으면 true, 기록 없으면 false`() {
        val records = listOf(record(id = "a", daysAgo = 1.0))
        assertTrue(NotificationHistoryPolicy.hasUnread(records, lastSeenAt = null))
        assertTrue(NotificationHistoryPolicy.hasUnread(records, lastSeenAt = now.minusSeconds(2 * 86_400)))
        assertFalse(NotificationHistoryPolicy.hasUnread(records, lastSeenAt = now))
        assertFalse(NotificationHistoryPolicy.hasUnread(emptyList(), lastSeenAt = null))
    }

    @Test
    fun `unreadCount - lastSeenAt 이후 수신 건수 (null이면 전체, 같은 시각은 제외)`() {
        val records = listOf(
            record(id = "a", daysAgo = 0.1),
            record(id = "b", daysAgo = 1.0),
            record(id = "c", daysAgo = 3.0),
        )
        assertEquals(3, NotificationHistoryPolicy.unreadCount(records, lastSeenAt = null))
        assertEquals(2, NotificationHistoryPolicy.unreadCount(records, lastSeenAt = now.minusSeconds(2 * 86_400)))
        assertEquals(0, NotificationHistoryPolicy.unreadCount(records, lastSeenAt = now))
        assertEquals(2, NotificationHistoryPolicy.unreadCount(records, lastSeenAt = records[2].receivedAt))
    }

    @Test
    fun `persistablePrune - 기간과 무관하게 하드캡만 적용(시계 의존 삭제 금지)`() {
        val old = record("veryOld", daysAgo = 4000.0)
        val recent = record("recent", daysAgo = 1.0)
        val kept = NotificationHistoryPolicy.persistablePrune(listOf(old, recent))
        assertEquals(listOf("recent", "veryOld"), kept.map { it.id })
    }

    @Test
    fun `persistablePrune - 하드캡 초과분은 오래된 것부터 잘린다`() {
        val records = (0 until NotificationHistoryPolicy.HARD_CAP + 2).map { record("r$it", daysAgo = it * 0.001) }
        val kept = NotificationHistoryPolicy.persistablePrune(records)
        assertEquals(NotificationHistoryPolicy.HARD_CAP, kept.size)
        assertEquals("r0", kept.first().id)
    }
}
