package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.data.datasource.FinraShortVolumeApi
import th1ngjin.fearindex.data.datasource.KospiFearIndexApi
import th1ngjin.fearindex.data.datasource.OfficialIndicatorsApi
import th1ngjin.fearindex.data.dto.KospiShortResponse
import th1ngjin.fearindex.data.dto.OfficialIndicatorSeriesDTO
import th1ngjin.fearindex.data.dto.OfficialIndicatorsResponse
import th1ngjin.fearindex.domain.entity.FearIndexType
import java.io.IOException
import java.time.Instant

/**
 * 공매도 비중 라우팅 + FINRA 병렬 fetch(부분 실패 허용) + TTL 캐시 + 출처 메타데이터.
 * 고정 clock: 2026-07-03T12:00:00Z → NY 2026-07-03(금) → 후보일 = 0702,0701,0630,0629,0626.
 */
class AssetShortPressureRepositoryImplTest {

    private val finraApi = mockk<FinraShortVolumeApi>()
    private val officialApi = mockk<OfficialIndicatorsApi>()
    private val kospiApi = mockk<KospiFearIndexApi>()
    private val fixedNow = Instant.parse("2026-07-03T12:00:00Z")
    private val repository = AssetShortPressureRepositoryImpl(
        finraApi = finraApi,
        officialApi = officialApi,
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

        val series = repository.dailyShortRatios(FearIndexType.MARKET)

        // 성공한 4일(0626,0629,0630,0701) 중 최근 3일, 시간순 오름차순
        assertEquals(listOf(32.0, 33.0, 34.0), series.ratios)
        val metadata = series.sourceMetadata!!
        assertEquals("FINRA Daily Short Sale Volume", metadata.sourceName)
        assertEquals("SPY ETF", metadata.basisLabel)
        assertEquals("2026-07-01", metadata.asOf) // 마지막 성공 파일 날짜
        assertTrue(metadata.isOfficial)
    }

    @Test
    fun `CRYPTO는 서버 official endpoint의 short ratios - 캐시로 재호출 방지`() = runTest {
        coEvery { officialApi.getCryptoOfficialIndicators() } returns OfficialIndicatorsResponse(
            short = OfficialIndicatorSeriesDTO(
                available = true,
                ratios = listOf(42.0, 45.0, 48.0),
                source = "Binance USD-M Futures",
                basis = "BTCUSDT Futures · BTC",
                asOf = "2026-07-05",
                official = true,
                methodology = "shortAccount percentage from Binance BTCUSDT global long/short account ratio.",
            ),
        )

        val first = repository.dailyShortRatios(FearIndexType.CRYPTO)
        val second = repository.dailyShortRatios(FearIndexType.CRYPTO)

        assertEquals(listOf(42.0, 45.0, 48.0), first.ratios)
        assertEquals("BTCUSDT Futures · BTC", first.sourceMetadata!!.basisLabel)
        assertEquals(first, second)
        coVerify(exactly = 1) { officialApi.getCryptoOfficialIndicators() }
    }

    @Test
    fun `CRYPTO - available=false면 빈 시계열(카드 숨김)`() = runTest {
        coEvery { officialApi.getCryptoOfficialIndicators() } returns OfficialIndicatorsResponse(
            short = OfficialIndicatorSeriesDTO(available = false, ratios = listOf(42.0)),
        )

        assertEquals(emptyList<Double>(), repository.dailyShortRatios(FearIndexType.CRYPTO).ratios)
    }

    @Test
    fun `KOSPI는 서버 shortRatios + 응답 메타데이터 - 캐시로 재호출 방지`() = runTest {
        coEvery { kospiApi.getKospiShort() } returns KospiShortResponse(
            shortRatios = listOf(4.2, 4.5, 3.9),
            source = "KRX Data Marketplace",
            basis = "KOSPI official short-sale statistics",
            official = true,
        )

        val first = repository.dailyShortRatios(FearIndexType.KOSPI)
        val second = repository.dailyShortRatios(FearIndexType.KOSPI)

        assertEquals(listOf(4.2, 4.5, 3.9), first.ratios)
        assertEquals("KRX Data Marketplace", first.sourceMetadata!!.sourceName)
        assertEquals(first, second)
        coVerify(exactly = 1) { kospiApi.getKospiShort() }
    }

    @Test
    fun `KOSPI available=false면 shortRatios가 있어도 빈 배열 - 카드 숨김`() = runTest {
        coEvery { kospiApi.getKospiShort() } returns KospiShortResponse(
            available = false,
            shortRatios = listOf(4.2, 4.5, 3.9),
        )

        val series = repository.dailyShortRatios(FearIndexType.KOSPI)

        assertEquals(emptyList<Double>(), series.ratios)
    }

    @Test
    fun `KOSPI unavailable 응답은 캐시하지 않음 - 소스 복구 시 즉시 반영`() = runTest {
        coEvery { kospiApi.getKospiShort() } returns KospiShortResponse(
            available = false,
            shortRatios = emptyList(),
        )

        repository.dailyShortRatios(FearIndexType.KOSPI)
        repository.dailyShortRatios(FearIndexType.KOSPI)

        coVerify(exactly = 2) { kospiApi.getKospiShort() }
    }

    @Test
    fun `3개 미만이면 캐시하지 않음`() = runTest {
        coEvery { kospiApi.getKospiShort() } returns KospiShortResponse(shortRatios = listOf(4.2))

        repository.dailyShortRatios(FearIndexType.KOSPI)
        repository.dailyShortRatios(FearIndexType.KOSPI)

        coVerify(exactly = 2) { kospiApi.getKospiShort() }
    }
}
