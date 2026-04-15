package th1ngjin.fearindex.domain.usecase

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository

class ObserveStuckCounterUseCaseTest {

    private val repository = mockk<StuckCounterRepository>()
    private val useCase = ObserveStuckCounterUseCase(repository)

    @Test
    fun `fetchOnce - 결과 반환`() = runTest {
        val expected = StuckCounterResult(
            stuckCount = 10,
            safeCount = 20,
            totalResponded = 30,
            stuckPercentage = 33.3,
            safePercentage = 66.7,
            myStatus = StuckStatus.NONE,
        )
        coEvery { repository.fetchResult(FearIndexType.MARKET) } returns expected

        val result = useCase.fetchOnce(FearIndexType.MARKET)

        assertEquals(expected, result)
    }

    @Test
    fun `stream - Flow 방출 확인`() = runTest {
        val result1 = StuckCounterResult.EMPTY
        val result2 = StuckCounterResult(
            stuckCount = 5,
            safeCount = 10,
            totalResponded = 15,
            stuckPercentage = 33.3,
            safePercentage = 66.7,
            myStatus = StuckStatus.STUCK,
        )
        every { repository.stuckCounterStream(FearIndexType.CRYPTO) } returns flowOf(result1, result2)

        useCase.stream(FearIndexType.CRYPTO).test {
            assertEquals(result1, awaitItem())
            assertEquals(result2, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `loadLocalStatus - 로컬 저장된 상태 반환`() {
        every { repository.loadLocalStatus(FearIndexType.MARKET) } returns StuckStatus.STUCK

        val result = useCase.loadLocalStatus(FearIndexType.MARKET)

        assertEquals(StuckStatus.STUCK, result)
    }

    @Test
    fun `loadLocalStatus - NONE 기본값`() {
        every { repository.loadLocalStatus(FearIndexType.CRYPTO) } returns StuckStatus.NONE

        val result = useCase.loadLocalStatus(FearIndexType.CRYPTO)

        assertEquals(StuckStatus.NONE, result)
    }
}
