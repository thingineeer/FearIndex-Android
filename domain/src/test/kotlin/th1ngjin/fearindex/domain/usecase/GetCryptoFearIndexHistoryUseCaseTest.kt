package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import java.time.Instant

class GetCryptoFearIndexHistoryUseCaseTest {

    private val repository = mockk<CryptoFearIndexRepository>()
    private val useCase = GetCryptoFearIndexHistoryUseCase(repository)

    @Test
    fun `invoke - 기본 days는 31`() = runTest {
        coEvery { repository.fetchHistory(31, false) } returns emptyList()

        useCase()

        coVerify { repository.fetchHistory(31, false) }
    }

    @Test
    fun `invoke - days 파라미터 전달`() = runTest {
        val expected = listOf(createFearIndex(20.0), createFearIndex(80.0))
        coEvery { repository.fetchHistory(7, false) } returns expected

        val result = useCase(days = 7)

        assertEquals(2, result.size)
        coVerify { repository.fetchHistory(7, false) }
    }

    @Test
    fun `invoke - forceRefresh 전달`() = runTest {
        coEvery { repository.fetchHistory(31, true) } returns emptyList()

        useCase(forceRefresh = true)

        coVerify { repository.fetchHistory(31, true) }
    }

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.now(),
    )
}
