package th1ngjin.fearindex.data.repository

import android.content.SharedPreferences
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import th1ngjin.fearindex.data.storage.JsonlFileStore
import th1ngjin.fearindex.data.storage.NotificationRecordCodec
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import java.io.File
import java.time.Instant

/** iOS `NotificationHistoryRepositoryTests` 포팅 (spool 은 Android 미사용) + upsert 승격 + updates 발행. */
class NotificationHistoryRepositoryImplTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val now: Instant = Instant.ofEpochSecond(1_800_000_000)

    private class InMemoryLastSeenStore : NotificationHistoryLastSeenStore {
        var value: Instant? = null
        override fun get(): Instant? = value
        override fun set(instant: Instant) { value = instant }
    }

    private class Fixture(
        val repository: NotificationHistoryRepositoryImpl,
        val store: JsonlFileStore,
        val file: File,
        val lastSeen: InMemoryLastSeenStore,
    )

    private fun makeFixture(): Fixture {
        val file = File(temp.root, "notification_history/notification-history.jsonl")
        val store = JsonlFileStore(file)
        val lastSeen = InMemoryLastSeenStore()
        return Fixture(NotificationHistoryRepositoryImpl(store, lastSeen), store, file, lastSeen)
    }

    private fun record(
        id: String,
        kind: NotificationKind = NotificationKind.KOSPI,
        daysAgo: Double,
        title: String = "t",
        body: String = "b",
    ) = NotificationRecord(
        id = id, kind = kind, title = title, body = body, score = 19,
        receivedAt = now.minusMillis((daysAgo * 86_400_000).toLong()),
    )

    private suspend fun storedIds(f: Fixture): List<String> =
        f.store.readAllLines().mapNotNull { NotificationRecordCodec.decode(it)?.id }

    @Test
    fun `append 후 fetchAll 로 왕복 (JSONL 파일 영속, 최신순)`() = runTest {
        val f = makeFixture()
        f.repository.append(record("a", daysAgo = 1.0))
        f.repository.append(record("b", daysAgo = 0.5))
        assertEquals(listOf("b", "a"), f.repository.fetchAll().map { it.id })
        assertEquals(2, f.store.readAllLines().size)
    }

    @Test
    fun `append 는 같은 id 를 dedup - 새 인스턴스(재시작 후)에서도 파일 기준으로 dedup`() = runTest {
        val f = makeFixture()
        f.repository.append(record("dup", daysAgo = 1.0))
        f.repository.append(record("dup", daysAgo = 1.0))
        assertEquals(1, f.store.readAllLines().size)

        val restarted = NotificationHistoryRepositoryImpl(f.store, f.lastSeen)
        restarted.append(record("dup", daysAgo = 1.0))
        restarted.append(record("fresh", daysAgo = 0.1))
        assertEquals(listOf("fresh", "dup"), storedIds(f))
    }

    @Test
    fun `append - fallback id 레코드는 같은 제목·본문의 실제 message id 로 승격되어 1건만 남는다`() = runTest {
        val f = makeFixture()
        val fallbackAt = now.minusSeconds(30)
        val fallback = NotificationRecord(
            NotificationRecord.fallbackId(NotificationKind.KOSPI, fallbackAt),
            NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, fallbackAt,
        )
        val real = NotificationRecord("0:msg%real", NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, now)
        f.repository.append(fallback)
        f.repository.append(real)
        assertEquals(listOf("0:msg%real"), storedIds(f))
        assertEquals(listOf("0:msg%real"), f.repository.fetchAll().map { it.id })
    }

    @Test
    fun `replaceAll 은 파일을 통째로 교체하고 이후 append dedup 도 파일 기준`() = runTest {
        val f = makeFixture()
        f.repository.append(record("old", daysAgo = 45.0))
        f.repository.append(record("keep", daysAgo = 5.0))
        f.repository.replaceAll(listOf(record("keep", daysAgo = 5.0)))
        assertEquals(listOf("keep"), f.repository.fetchAll().map { it.id })
        f.repository.append(record("old", daysAgo = 45.0))
        assertEquals(listOf("keep", "old"), f.repository.fetchAll().map { it.id })
    }

    @Test
    fun `lastSeenAt 왕복`() = runTest {
        val f = makeFixture()
        assertNull(f.repository.lastSeenAt())
        f.repository.setLastSeenAt(now)
        assertEquals(now, f.repository.lastSeenAt())
        assertEquals(now, f.lastSeen.value)
    }

    @Test
    fun `손상 라인이 섞여 있어도 나머지 레코드는 읽힌다 (최신순)`() = runTest {
        val f = makeFixture()
        f.repository.append(record("ok", daysAgo = 1.0))
        f.store.appendLine("{broken")
        f.repository.append(record("ok2", daysAgo = 0.5))
        assertEquals(listOf("ok2", "ok"), f.repository.fetchAll().map { it.id })
    }

    @Test
    fun `파일에 중복 줄이 있어도 fetchAll 은 dedup 해서 돌려준다`() = runTest {
        val f = makeFixture()
        f.store.appendLine(NotificationRecordCodec.encode(record("dup", daysAgo = 1.0)))
        f.store.appendLine(NotificationRecordCodec.encode(record("dup", daysAgo = 1.0)))
        f.store.appendLine(NotificationRecordCodec.encode(record("solo", daysAgo = 0.5)))
        assertEquals(listOf("solo", "dup"), f.repository.fetchAll().map { it.id })
    }

    @Test
    fun `동시 append 경합(같은 id 를 여러 경로가 동시에 기록)에도 파일에는 1줄만 남는다`() = runTest {
        val f = makeFixture()
        val same = record("race", daysAgo = 0.1)
        (0 until 20).map { async { f.repository.append(same) } }.awaitAll()
        assertEquals(1, f.store.readAllLines().size)
        assertEquals(listOf("race"), f.repository.fetchAll().map { it.id })
    }

    @Test
    fun `updates - append, replaceAll, setLastSeenAt 이후 발행되고 중복 append(변경 없음)는 발행하지 않는다`() = runTest {
        val f = makeFixture()
        f.repository.updates.test {
            f.repository.append(record("a", daysAgo = 1.0))
            awaitItem()
            f.repository.append(record("a", daysAgo = 1.0))
            expectNoEvents()
            f.repository.replaceAll(emptyList())
            awaitItem()
            f.repository.setLastSeenAt(now)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `빈 저장소 - fetchAll 빈 리스트, 파일 미생성`() = runTest {
        val f = makeFixture()
        assertTrue(f.repository.fetchAll().isEmpty())
        assertFalse(f.file.exists())
    }

    @Test
    fun `UseCase 결합 - 무료 fetch 는 30일 초과분을 숨기되 파일에는 보존, 프리미엄이면 복원`() = runTest {
        val f = makeFixture()
        f.repository.append(record("old", daysAgo = 45.0))
        f.repository.append(record("keep", daysAgo = 5.0))
        val useCase = NotificationHistoryUseCase(f.repository, now = { now })

        // 무료: 화면에서만 숨김
        assertEquals(listOf("keep"), useCase.fetch(isPremium = false).map { it.id })
        // 파일은 무손실 — 잠금 카피("30일 이전 내역은 프리미엄에서")가 성립하는 조건
        assertEquals(setOf("keep", "old"), storedIds(f).toSet())
        // 프리미엄 구매 즉시 과거 내역 복원
        assertEquals(listOf("keep", "old"), useCase.fetch(isPremium = true).map { it.id })

        assertTrue(useCase.hasUnread(isPremium = false))
        useCase.markSeen(now)
        assertFalse(useCase.hasUnread(isPremium = false))
    }

    @Test
    fun `SharedPreferencesLastSeenStore - epoch 초 키로 왕복, 미설정이면 null`() {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val prefs = mockk<SharedPreferences>()
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { prefs.contains(SharedPreferencesLastSeenStore.KEY_LAST_SEEN) } returns false
        val store = SharedPreferencesLastSeenStore(prefs)
        assertNull(store.get())

        store.set(now)
        verify { editor.putLong(SharedPreferencesLastSeenStore.KEY_LAST_SEEN, now.epochSecond) }
        verify { editor.apply() }

        every { prefs.contains(SharedPreferencesLastSeenStore.KEY_LAST_SEEN) } returns true
        every { prefs.getLong(SharedPreferencesLastSeenStore.KEY_LAST_SEEN, any()) } returns now.epochSecond
        assertEquals(now, store.get())
    }
}
