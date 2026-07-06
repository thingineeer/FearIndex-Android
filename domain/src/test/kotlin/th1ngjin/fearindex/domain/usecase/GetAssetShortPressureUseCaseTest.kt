package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import th1ngjin.fearindex.domain.entity.AssetShortRatioSeries
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.IndicatorSourceMetadata
import th1ngjin.fearindex.domain.entity.ShortPressure
import th1ngjin.fearindex.domain.repository.AssetShortPressureRepository
import java.io.IOException

/**
 * iOS `FetchAssetShortPressureUseCaseTests` 대응 — 공매도 비중 시계열 → 신호 산출.
 */
class GetAssetShortPressureUseCaseTest {

    private val repository = mockk<AssetShortPressureRepository>()
    private val useCase = GetAssetShortPressureUseCase(repository)

    @Test
    fun `상승 시리즈면 heavyShorting`() = runTest {
        coEvery { repository.dailyShortRatios(FearIndexType.CRYPTO) } returns
            AssetShortRatioSeries(ratios = listOf(30.0, 31.0, 32.0, 40.0))

        val sp = useCase(FearIndexType.CRYPTO)

        assertNotNull(sp)
        assertEquals(ShortPressure.Signal.HEAVY_SHORTING, sp!!.signal)
    }

    @Test
    fun `출처 메타데이터를 ShortPressure에 부착`() = runTest {
        val metadata = IndicatorSourceMetadata(
            sourceName = "FINRA Daily Short Sale Volume",
            basisLabel = "SPY ETF",
            asOf = "2026-07-02",
            isOfficial = true,
            methodology = "SPY ShortVolume / TotalVolume from FINRA CNMS daily files.",
        )
        coEvery { repository.dailyShortRatios(FearIndexType.MARKET) } returns
            AssetShortRatioSeries(
                ratios = listOf(30.0, 31.0, 32.0, 40.0),
                sourceMetadata = metadata,
            )

        val sp = useCase(FearIndexType.MARKET)

        assertEquals(metadata, sp!!.sourceMetadata)
    }

    @Test
    fun `빈 시리즈면 null(카드 숨김)`() = runTest {
        coEvery { repository.dailyShortRatios(FearIndexType.MARKET) } returns
            AssetShortRatioSeries.EMPTY

        assertNull(useCase(FearIndexType.MARKET))
    }

    @Test
    fun `repo 에러는 전파`() = runTest {
        coEvery { repository.dailyShortRatios(FearIndexType.MARKET) } throws IOException("boom")

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(FearIndexType.MARKET) }
        }
    }
}
