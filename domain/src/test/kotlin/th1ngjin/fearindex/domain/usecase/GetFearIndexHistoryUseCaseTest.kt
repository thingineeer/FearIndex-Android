package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.FearIndexRepository
import java.time.Instant

class GetFearIndexHistoryUseCaseTest {

    private val repository = mockk<FearIndexRepository>()
    private val useCase = GetFearIndexHistoryUseCase(repository)

    @Test
    fun `invoke - 기본 days는 365`() = runTest {
        coEvery { repository.fetchHistory(365, false) } returns emptyList()

        useCase()

        coVerify { repository.fetchHistory(365, false) }
    }

    @Test
    fun `invoke - days 파라미터 전달`() = runTest {
        val expected = listOf(createFearIndex(50.0), createFearIndex(60.0))
        coEvery { repository.fetchHistory(90, false) } returns expected

        val result = useCase(days = 90)

        assertEquals(2, result.size)
        assertEquals(expected, result)
        coVerify { repository.fetchHistory(90, false) }
    }

    @Test
    fun `invoke - forceRefresh 전달`() = runTest {
        coEvery { repository.fetchHistory(180, true) } returns emptyList()

        useCase(days = 180, forceRefresh = true)

        coVerify { repository.fetchHistory(180, true) }
    }

    @Test(expected = RuntimeException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.fetchHistory(any(), any()) } throws RuntimeException("Server error")

        useCase()
    }

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.now(),
    )
}
