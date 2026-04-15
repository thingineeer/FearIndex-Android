package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import java.io.IOException
import java.time.Instant

class GetCryptoFearIndexUseCaseTest {

    private val repository = mockk<CryptoFearIndexRepository>()
    private val useCase = GetCryptoFearIndexUseCase(repository)

    @Test
    fun `invoke - 성공 시 FearIndex 반환`() = runTest {
        val expected = createFearIndex(score = 30.0)
        coEvery { repository.fetchCurrent(false) } returns expected

        val result = useCase()

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.fetchCurrent(false) }
    }

    @Test
    fun `invoke - forceRefresh true 전달`() = runTest {
        val expected = createFearIndex(score = 85.0)
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

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.now(),
    )
}
