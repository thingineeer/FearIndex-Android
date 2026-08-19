package th1ngjin.fearindex.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.repository.NotificationHistoryRepository
import th1ngjin.fearindex.domain.util.NotificationHistoryPolicy
import java.time.Instant

/** iOS `NotificationHistoryUseCaseTests` 포팅. 인메모리 fake 저장소로 prune 영속/markSeen/unread 흐름 검증. */
class NotificationHistoryUseCaseTest {

    private val now: Instant = Instant.ofEpochSecond(1_800_000_000)

    private class FakeRepository(initial: List<NotificationRecord>) : NotificationHistoryRepository {
        var records: List<NotificationRecord> = initial
        var seenAt: Instant? = null
        var replaceAllCalls = 0
        private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val updates: Flow<Unit> = _updates

        override suspend fun fetchAll(): List<NotificationRecord> = records
        override suspend fun append(record: NotificationRecord) {
            if (records.none { it.id == record.id }) records = records + record
            _updates.tryEmit(Unit)
        }
        override suspend fun replaceAll(records: List<NotificationRecord>) {
            replaceAllCalls++
            this.records = records
        }
        override suspend fun lastSeenAt(): Instant? = seenAt
        override suspend fun setLastSeenAt(instant: Instant) { seenAt = instant }
    }

    private fun record(id: String, daysAgo: Double) = NotificationRecord(
        id = id, kind = NotificationKind.MARKET, title = "t", body = "b", score = 30,
        receivedAt = now.minusMillis((daysAgo * 86_400_000).toLong()),
    )

    private fun makeSut(records: List<NotificationRecord> = emptyList()): Pair<NotificationHistoryUseCase, FakeRepository> {
        val repository = FakeRepository(records)
        return NotificationHistoryUseCase(repository, now = { now }) to repository
    }

    @Test
    fun `fetch(무료) - 30일 초과분은 화면에서만 숨기고 저장소에는 그대로 남긴다`() = runTest {
        val (useCase, repository) = makeSut(
            listOf(record("old", 45.0), record("keep", 5.0), record("newest", 0.2)),
        )
        val result = useCase.fetch(isPremium = false)
        assertEquals(listOf("newest", "keep"), result.map { it.id })
        // 저장소는 무손실 — 기간 필터는 표시 단계에서만 적용된다(순서는 저장소 원본 유지)
        assertEquals(setOf("newest", "keep", "old"), repository.records.map { it.id }.toSet())
        assertEquals(0, repository.replaceAllCalls)
    }

    @Test
    fun `fetch - 무료로 숨겨졌던 30일 이전 내역이 프리미엄 구매 즉시 복원된다`() = runTest {
        val (useCase, repository) = makeSut(
            listOf(record("old", 45.0), record("newest", 0.2)),
        )
        // 무료: 숨김
        assertEquals(listOf("newest"), useCase.fetch(isPremium = false).map { it.id })
        // 구매 직후: 같은 저장소에서 과거 내역이 그대로 돌아온다 (잠금 카피가 성립하는 조건)
        assertEquals(listOf("newest", "old"), useCase.fetch(isPremium = true).map { it.id })
        assertEquals(0, repository.replaceAllCalls)
    }

    @Test
    fun `fetch - 기기 시계가 미래로 튀어도 레코드는 삭제되지 않고 시계 복귀 시 복원된다`() = runTest {
        val repository = FakeRepository(listOf(record("a", 5.0), record("b", 1.0)))
        var clock = now
        val useCase = NotificationHistoryUseCase(repository) { clock }

        // 시계가 1년 미래로 튄 상태: 전부 기간 초과로 보이지만 삭제하지 않는다
        clock = now.plusMillis(365L * 86_400_000)
        assertEquals(emptyList<String>(), useCase.fetch(isPremium = false).map { it.id })
        assertEquals(setOf("a", "b"), repository.records.map { it.id }.toSet())
        assertEquals(0, repository.replaceAllCalls)

        // 시계 복귀 → 자동 복원
        clock = now
        assertEquals(listOf("b", "a"), useCase.fetch(isPremium = false).map { it.id })
    }

    @Test
    fun `fetch - 하드캡 초과분만 저장소에서 실제로 제거한다`() = runTest {
        val over = (0 until NotificationHistoryPolicy.HARD_CAP + 3).map { record("r$it", it * 0.001) }
        val (useCase, repository) = makeSut(over)
        val result = useCase.fetch(isPremium = true)
        assertEquals(NotificationHistoryPolicy.HARD_CAP, result.size)
        assertEquals(NotificationHistoryPolicy.HARD_CAP, repository.records.size)
        assertEquals(1, repository.replaceAllCalls)
    }

    @Test
    fun `fetch(프리미엄) - 기간 숨김 없음, 하드캡 이하면 replaceAll 호출 안 함`() = runTest {
        val (useCase, repository) = makeSut(listOf(record("newest", 0.2), record("old", 400.0)))
        val result = useCase.fetch(isPremium = true)
        assertEquals(listOf("newest", "old"), result.map { it.id })
        assertEquals(0, repository.replaceAllCalls)
    }

    @Test
    fun `fetch - 저장소 순서가 최신순이 아니어도 하드캡 이하면 replaceAll 호출 안 함`() = runTest {
        val (useCase, repository) = makeSut(listOf(record("old", 20.0), record("newest", 0.2)))
        val result = useCase.fetch(isPremium = false)
        assertEquals(listOf("newest", "old"), result.map { it.id })
        assertEquals(0, repository.replaceAllCalls)
    }

    @Test
    fun `record - 저장소에 append (같은 id 재기록은 저장소가 dedup)`() = runTest {
        val (useCase, repository) = makeSut()
        useCase.record(record("a", 0.0))
        useCase.record(record("a", 0.0))
        assertEquals(1, repository.records.size)
    }

    @Test
    fun `hasUnread - markSeen 전 true, markSeen 후 false, 새 수신 후 다시 true`() = runTest {
        val (useCase, _) = makeSut(listOf(record("a", 0.5)))
        assertTrue(useCase.hasUnread(isPremium = false))
        useCase.markSeen(now)
        assertFalse(useCase.hasUnread(isPremium = false))
        useCase.record(record("b", -0.01))
        assertTrue(useCase.hasUnread(isPremium = false))
    }

    @Test
    fun `markSeen - 인자 생략 시 주입된 now 사용`() = runTest {
        val (useCase, repository) = makeSut()
        useCase.markSeen()
        assertEquals(now, repository.seenAt)
    }

    @Test
    fun `hasUnread(무료) - 30일 초과분만 있으면 배지 없음 (프리미엄이면 있음)`() = runTest {
        val (useCase, _) = makeSut(listOf(record("old", 45.0)))
        assertFalse(useCase.hasUnread(isPremium = false))
        assertTrue(useCase.hasUnread(isPremium = true))
    }

    @Test
    fun `unreadCount - 보관 기간 안의 미확인 건수`() = runTest {
        val (useCase, _) = makeSut(listOf(record("old", 45.0), record("a", 1.0), record("b", 0.1)))
        assertEquals(2, useCase.unreadCount(isPremium = false))
        assertEquals(3, useCase.unreadCount(isPremium = true))
        useCase.markSeen(now.minusSeconds(12 * 3_600))
        assertEquals(1, useCase.unreadCount(isPremium = false))
    }

    @Test
    fun `빈 저장소 - fetch 빈 배열, hasUnread false`() = runTest {
        val (useCase, _) = makeSut()
        assertTrue(useCase.fetch(isPremium = false).isEmpty())
        assertFalse(useCase.hasUnread(isPremium = true))
    }

    @Test
    fun `updates - 저장소 updates 스트림을 그대로 노출`() = runTest {
        val (useCase, _) = makeSut()
        useCase.updates.test {
            useCase.record(record("a", 0.0))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
