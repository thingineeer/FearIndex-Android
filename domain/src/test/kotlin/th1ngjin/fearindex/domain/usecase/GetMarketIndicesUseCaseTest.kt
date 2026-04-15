package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.repository.MarketIndexRepository

class GetMarketIndicesUseCaseTest {

    private val repository = mockk<MarketIndexRepository>()
    private val useCase = GetMarketIndicesUseCase(repository)

    @Test
    fun `invoke - 시장 지수 리스트 반환`() = runTest {
        val expected = listOf(
            MarketIndex(symbol = "^GSPC", name = "S&P 500", price = 5200.0, changePercent = 1.2, isPositive = true),
            MarketIndex(symbol = "^KS11", name = "KOSPI", price = 2650.0, changePercent = -0.5, isPositive = false),
        )
        coEvery { repository.getMarketIndices() } returns expected

        val result = useCase()

        assertEquals(2, result.size)
        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getMarketIndices() }
    }

    @Test
    fun `invoke - 빈 리스트 반환`() = runTest {
        coEvery { repository.getMarketIndices() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test(expected = RuntimeException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.getMarketIndices() } throws RuntimeException("API error")

        useCase()
    }
}
