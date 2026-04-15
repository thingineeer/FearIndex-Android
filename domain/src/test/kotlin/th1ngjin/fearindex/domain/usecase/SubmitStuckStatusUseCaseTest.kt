package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository

class SubmitStuckStatusUseCaseTest {

    private val repository = mockk<StuckCounterRepository>()
    private val useCase = SubmitStuckStatusUseCase(repository)

    @Test
    fun `invoke - MARKET STUCK 제출 성공`() = runTest {
        val expected = StuckCounterResult(
            stuckCount = 50,
            safeCount = 30,
            totalResponded = 80,
            stuckPercentage = 62.5,
            safePercentage = 37.5,
            myStatus = StuckStatus.STUCK,
        )
        coEvery { repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK) } returns expected

        val result = useCase(FearIndexType.MARKET, StuckStatus.STUCK)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.submitStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK) }
    }

    @Test
    fun `invoke - CRYPTO SAFE 제출 성공`() = runTest {
        val expected = StuckCounterResult(
            stuckCount = 20,
            safeCount = 80,
            totalResponded = 100,
            stuckPercentage = 20.0,
            safePercentage = 80.0,
            myStatus = StuckStatus.SAFE,
        )
        coEvery { repository.submitStuckStatus(FearIndexType.CRYPTO, StuckStatus.SAFE) } returns expected

        val result = useCase(FearIndexType.CRYPTO, StuckStatus.SAFE)

        assertEquals(expected, result)
    }

    @Test(expected = RuntimeException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.submitStuckStatus(any(), any()) } throws RuntimeException("Server error")

        useCase(FearIndexType.MARKET, StuckStatus.STUCK)
    }
}
