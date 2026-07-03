package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.FearRSI
import th1ngjin.fearindex.domain.repository.AssetPriceClosesRepository
import java.io.IOException

/**
 * iOS `FetchAssetRSIUseCaseTests.swift` 대응 — 종가 시계열 → RSI(14) 계산.
 */
class GetAssetRSIUseCaseTest {

    private val repository = mockk<AssetPriceClosesRepository>()
    private val useCase = GetAssetRSIUseCase(repository)

    @Test
    fun `단조 상승 종가면 RSI 100 overbought`() = runTest {
        coEvery { repository.dailyCloses(FearIndexType.MARKET) } returns
            List(20) { it * 2.0 + 10 }

        val rsi = useCase(FearIndexType.MARKET)

        assertNotNull(rsi)
        assertEquals(100.0, rsi!!.value, 0.0)
        assertEquals(FearRSI.RSISignal.OVERBOUGHT, rsi.signal)
    }

    @Test
    fun `종가 부족이면 null(카드 숨김)`() = runTest {
        coEvery { repository.dailyCloses(FearIndexType.CRYPTO) } returns emptyList()

        assertNull(useCase(FearIndexType.CRYPTO))
    }

    @Test
    fun `repo 에러는 전파`() = runTest {
        coEvery { repository.dailyCloses(FearIndexType.KOSPI) } throws IOException("boom")

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(FearIndexType.KOSPI) }
        }
    }
}
