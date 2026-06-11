package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.KospiFearIndexRepository
import java.time.Instant

class GetKospiFearIndexHistoryUseCaseTest {

    private val repository = mockk<KospiFearIndexRepository>()
    private val useCase = GetKospiFearIndexHistoryUseCase(repository)

    @Test
    fun `invoke - 기본 days는 365`() = runTest {
        coEvery { repository.fetchHistory(365, false) } returns emptyList()

        useCase()

        coVerify { repository.fetchHistory(365, false) }
    }

    @Test
    fun `invoke - days 파라미터 전달`() = runTest {
        val expected = listOf(createFearIndex(40.0), createFearIndex(55.0))
        coEvery { repository.fetchHistory(90, false) } returns expected

        val result = useCase(days = 90)

        assertEquals(expected, result)
        coVerify { repository.fetchHistory(90, false) }
    }

    @Test
    fun `invoke - forceRefresh 전달`() = runTest {
        coEvery { repository.fetchHistory(30, true) } returns emptyList()

        useCase(days = 30, forceRefresh = true)

        coVerify { repository.fetchHistory(30, true) }
    }

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.parse("2026-06-10T06:00:00Z"),
    )
}
