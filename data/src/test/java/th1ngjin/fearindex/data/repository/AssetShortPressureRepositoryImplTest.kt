package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.data.datasource.BinanceFuturesApi
import th1ngjin.fearindex.data.datasource.FinraShortVolumeApi
import th1ngjin.fearindex.data.datasource.KospiFearIndexApi
import th1ngjin.fearindex.data.dto.BinanceLongShortRatioDTO
import th1ngjin.fearindex.data.dto.KospiShortResponse
import th1ngjin.fearindex.domain.entity.FearIndexType
import java.io.IOException
import java.time.Instant

/**
 * 공매도 비중 라우팅 + FINRA 병렬 fetch(부분 실패 허용) + TTL 캐시.
 * 고정 clock: 2026-07-03T12:00:00Z → NY 2026-07-03(금) → 후보일 = 0702,0701,0630,0629,0626.
 */
class AssetShortPressureRepositoryImplTest {

    private val finraApi = mockk<FinraShortVolumeApi>()
    private val binanceApi = mockk<BinanceFuturesApi>()
    private val kospiApi = mockk<KospiFearIndexApi>()
    private val fixedNow = Instant.parse("2026-07-03T12:00:00Z")
    private val repository = AssetShortPressureRepositoryImpl(
        finraApi = finraApi,
        binanceApi = binanceApi,
        kospiApi = kospiApi,
        now = { fixedNow },
    )

    private fun finraText(ratio: Double): okhttp3.ResponseBody {
        val short = (ratio * 1000).toLong()
        return "Date|Symbol|ShortVolume|ShortExemptVolume|TotalVolume|Market\n20260702|SPY|$short|0|100000|Q"
            .toResponseBody()
    }

    @Test
    fun `MARKET은 FINRA 최근 거래일 병렬 fetch - 실패일 스킵하고 오름차순 최근 3일`() = runTest {
        coEvery { finraApi.getDailyShortVolume("20260702") } throws IOException("404 미발행")
        coEvery { finraApi.getDailyShortVolume("20260701") } returns finraText(34.0)
        coEvery { finraApi.getDailyShortVolume("20260630") } returns finraText(33.0)
        coEvery { finraApi.getDailyShortVolume("20260629") } returns finraText(32.0)
        coEvery { finraApi.getDailyShortVolume("20260626") } returns finraText(31.0)

        val ratios = repository.dailyShortRatios(FearIndexType.MARKET)

        // 성공한 4일(0626,0629,0630,0701) 중 최근 3일, 시간순 오름차순
        assertEquals(listOf(32.0, 33.0, 34.0), ratios)
    }

    @Test
    fun `CRYPTO는 Binance 순숏 계정 비율 - 캐시로 재호출 방지`() = runTest {
        coEvery { binanceApi.getGlobalLongShortAccountRatio() } returns listOf(
            BinanceLongShortRatioDTO(shortAccount = "0.45", timestamp = 2L),
            BinanceLongShortRatioDTO(shortAccount = "0.42", timestamp = 1L),
            BinanceLongShortRatioDTO(shortAccount = "0.48", timestamp = 3L),
        )

        val first = repository.dailyShortRatios(FearIndexType.CRYPTO)
        val second = repository.dailyShortRatios(FearIndexType.CRYPTO)

        assertEquals(listOf(42.0, 45.0, 48.0), first)
        assertEquals(first, second)
        coVerify(exactly = 1) { binanceApi.getGlobalLongShortAccountRatio() }
    }

    @Test
    fun `KOSPI는 서버 shortRatios - 캐시로 재호출 방지`() = runTest {
        coEvery { kospiApi.getKospiShort() } returns KospiShortResponse(
            shortRatios = listOf(4.2, 4.5, 3.9),
        )

        val first = repository.dailyShortRatios(FearIndexType.KOSPI)
        val second = repository.dailyShortRatios(FearIndexType.KOSPI)

        assertEquals(listOf(4.2, 4.5, 3.9), first)
        assertEquals(first, second)
        coVerify(exactly = 1) { kospiApi.getKospiShort() }
    }

    @Test
    fun `3개 미만이면 캐시하지 않음`() = runTest {
        coEvery { kospiApi.getKospiShort() } returns KospiShortResponse(shortRatios = listOf(4.2))

        repository.dailyShortRatios(FearIndexType.KOSPI)
        repository.dailyShortRatios(FearIndexType.KOSPI)

        coVerify(exactly = 2) { kospiApi.getKospiShort() }
    }
}
