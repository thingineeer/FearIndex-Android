package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.FearIndexRepository
import java.io.IOException
import java.time.Instant

class GetFearIndexUseCaseTest {

    private val repository = mockk<FearIndexRepository>()
    private val useCase = GetFearIndexUseCase(repository)

    @Test
    fun `invoke - 성공 시 FearIndex 반환`() = runTest {
        val expected = createFearIndex(score = 42.0)
        coEvery { repository.fetchCurrent(false) } returns expected

        val result = useCase()

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.fetchCurrent(false) }
    }

    @Test
    fun `invoke - forceRefresh true 전달`() = runTest {
        val expected = createFearIndex(score = 70.0)
        coEvery { repository.fetchCurrent(true) } returns expected

        val result = useCase(forceRefresh = true)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.fetchCurrent(true) }
    }

    @Test(expected = IOException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.fetchCurrent(any()) } throws IOException("Network error")

        useCase()
    }

    @Test
    fun `invoke - 기본 forceRefresh는 false`() = runTest {
        val expected = createFearIndex(score = 50.0)
        coEvery { repository.fetchCurrent(false) } returns expected

        useCase()

        coVerify { repository.fetchCurrent(false) }
    }

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.now(),
    )
}
