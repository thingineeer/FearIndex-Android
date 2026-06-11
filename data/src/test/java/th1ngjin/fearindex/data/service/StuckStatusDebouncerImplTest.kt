package th1ngjin.fearindex.data.service

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository

/**
 * StuckStatusDebouncerImpl 무결성 테스트 (실시간 기반).
 *
 * 내부 CoroutineScope(Dispatchers.IO)를 쓰므로 runTest 가상시간이 아닌
 * 실제 delay에 맞춰 `debounceMillis`를 짧게 세팅한 후 검증.
 *
 * 검증 포인트:
 * - 기본 debounceMillis 5초 (서버 호출 비용과 Functions rate limit 보호)
 * - 연타 병합: 마지막 상태만 서버 호출
 * - MARKET/CRYPTO 독립 디바운스
 * - flush 실패 시 재시도 큐 저장
 */
class StuckStatusDebouncerImplTest {

    private val repository = mockk<StuckCounterRepository>()
    private val storage = mockk<StuckCounterStorage>(relaxUnitFun = true)

    @Test
    fun `기본 debounceMillis는 5000ms`() {
        val debouncer = StuckStatusDebouncerImpl(repository, storage)
        assertEquals(
            "서버 호출 비용 보호 기준 — 기본 디바운스는 5초여야 함",
            5_000L,
            debouncer.debounceMillis,
        )
    }

    @Test
    fun `schedule - 디바운스 경과 후 submitStuckStatus 1회 호출`() = runBlocking {
        coEvery { repository.submitStuckStatus(any(), any()) } returns StuckCounterResult.EMPTY

        val debouncer = StuckStatusDebouncerImpl(repository, storage).apply {
            debounceMillis = 50L // 테스트용 단축
        }

        debouncer.schedule(FearIndexType.MARKET, StuckStatus.STUCK)
        delay(150L) // debounce + 여유

        coVerify(exactly = 1) {
            repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK)
        }
    }

    @Test
    fun `schedule - 연타 시 마지막 상태만 반영`() = runBlocking {
        coEvery { repository.submitStuckStatus(any(), any()) } returns StuckCounterResult.EMPTY

        val debouncer = StuckStatusDebouncerImpl(repository, storage).apply {
            debounceMillis = 80L
        }

        debouncer.schedule(FearIndexType.MARKET, StuckStatus.STUCK)
        delay(20L)
        debouncer.schedule(FearIndexType.MARKET, StuckStatus.SAFE)
        delay(20L)
        debouncer.schedule(FearIndexType.MARKET, StuckStatus.NONE)
        delay(200L)

        coVerify(exactly = 0) { repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK) }
        coVerify(exactly = 0) { repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.SAFE) }
        coVerify(exactly = 1) { repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.NONE) }
    }

    @Test
    fun `schedule - MARKET과 CRYPTO는 독립 디바운스`() = runBlocking {
        coEvery { repository.submitStuckStatus(any(), any()) } returns StuckCounterResult.EMPTY

        val debouncer = StuckStatusDebouncerImpl(repository, storage).apply {
            debounceMillis = 50L
        }

        debouncer.schedule(FearIndexType.MARKET, StuckStatus.STUCK)
        debouncer.schedule(FearIndexType.CRYPTO, StuckStatus.SAFE)
        delay(200L)

        coVerify(exactly = 1) { repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK) }
        coVerify(exactly = 1) { repository.submitStuckStatus(FearIndexType.CRYPTO, StuckStatus.SAFE) }
    }

    @Test
    fun `flush 실패 시 storage savePendingRetry 호출`() = runBlocking {
        coEvery { repository.submitStuckStatus(any(), any()) } throws RuntimeException("network")
        every { storage.savePendingRetry(any(), any()) } just Runs

        val debouncer = StuckStatusDebouncerImpl(repository, storage).apply {
            debounceMillis = 50L
        }

        debouncer.schedule(FearIndexType.MARKET, StuckStatus.STUCK)
        delay(200L)

        coVerify(exactly = 1) {
            storage.savePendingRetry(FearIndexType.MARKET, StuckStatus.STUCK)
        }
    }
}
