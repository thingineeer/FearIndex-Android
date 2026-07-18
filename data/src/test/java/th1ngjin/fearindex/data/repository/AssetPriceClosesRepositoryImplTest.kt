package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.data.datasource.KospiFearIndexDataSource
import th1ngjin.fearindex.data.datasource.OfficialIndicatorsApi
import th1ngjin.fearindex.data.datasource.YahooChartApi
import th1ngjin.fearindex.data.dto.KospiPublicSnapshotResponse
import th1ngjin.fearindex.data.dto.OfficialIndicatorSeriesDTO
import th1ngjin.fearindex.data.dto.OfficialIndicatorsResponse
import th1ngjin.fearindex.data.dto.YahooCloseChartResponse
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 자산 일봉 종가 라우팅 — MARKET=Yahoo ^GSPC(기기 직접), CRYPTO=서버 official endpoint,
 * KOSPI=스냅샷 kospiClose. 각 시계열에 출처 메타데이터 부착 (iOS v1.8.8 parity).
 */
class AssetPriceClosesRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val yahooApi = mockk<YahooChartApi>()
    private val officialApi = mockk<OfficialIndicatorsApi>()
    private val kospiDataSource = mockk<KospiFearIndexDataSource>()
    private val repository = AssetPriceClosesRepositoryImpl(yahooApi, officialApi, kospiDataSource)

    @Test
    fun `MARKET은 Yahoo GSPC 6mo 일봉 종가 + 비공식 하드코딩 메타데이터`() = runTest {
        val response = json.decodeFromString<YahooCloseChartResponse>(
            """{"chart":{"result":[{"indicators":{"quote":[{"close":[6100.0,null,6120.0]}]}}]}}""",
        )
        coEvery { yahooApi.getCloseChart("^GSPC", "1d", "6mo") } returns response

        val series = repository.dailyCloses(FearIndexType.MARKET)

        assertEquals(listOf(6100.0, 6120.0), series.closes)
        val metadata = series.sourceMetadata!!
        assertEquals("Yahoo Finance ^GSPC", metadata.sourceName)
        assertEquals("S&P 500", metadata.basisLabel)
        assertEquals(null, metadata.asOf)
        assertFalse(metadata.isOfficial)
        coVerify(exactly = 1) { yahooApi.getCloseChart("^GSPC", "1d", "6mo") }
    }

    @Test
    fun `CRYPTO는 서버 official endpoint의 rsi closes + 서버 메타데이터`() = runTest {
        coEvery { officialApi.getCryptoOfficialIndicators() } returns OfficialIndicatorsResponse(
            rsi = OfficialIndicatorSeriesDTO(
                available = true,
                closes = listOf(66000.0, 66500.0),
                source = "Binance USD-M Futures",
                basis = "BTC",
                asOf = "2026-07-05",
                official = true,
                methodology = "14-day RSI from Binance BTCUSDT daily futures closes.",
            ),
        )

        val series = repository.dailyCloses(FearIndexType.CRYPTO)

        assertEquals(listOf(66000.0, 66500.0), series.closes)
        val metadata = series.sourceMetadata!!
        assertEquals("Binance USD-M Futures", metadata.sourceName)
        assertEquals("BTC", metadata.basisLabel)
        assertEquals("2026-07-05", metadata.asOf)
        assertTrue(metadata.isOfficial)
    }

    @Test
    fun `CRYPTO - closes 없으면 values 폴백`() = runTest {
        coEvery { officialApi.getCryptoOfficialIndicators() } returns OfficialIndicatorsResponse(
            rsi = OfficialIndicatorSeriesDTO(available = true, values = listOf(1.0, 2.0)),
        )

        assertEquals(listOf(1.0, 2.0), repository.dailyCloses(FearIndexType.CRYPTO).closes)
    }

    @Test
    fun `CRYPTO - available=false면 빈 시계열(카드 숨김)`() = runTest {
        coEvery { officialApi.getCryptoOfficialIndicators() } returns OfficialIndicatorsResponse(
            rsi = OfficialIndicatorSeriesDTO(available = false, closes = listOf(66000.0)),
        )

        assertEquals(emptyList<Double>(), repository.dailyCloses(FearIndexType.CRYPTO).closes)
    }

    @Test
    fun `KOSPI는 스냅샷 kospiClose(날짜 오름차순) + 마지막 종가일 asOf 메타데이터`() = runTest {
        val response = json.decodeFromString<KospiPublicSnapshotResponse>(
            """
            {"version":1,"generatedAt":"2026-07-03T00:00:00Z","history":[
              {"date":"2026-07-02","score":40.0,"rating":"fear","confidence":"high","kospiClose":8730.5},
              {"date":"2026-06-30","score":38.0,"rating":"fear","confidence":"high","kospiClose":8650.0},
              {"date":"2026-07-01","score":39.0,"rating":"fear","confidence":"high"}
            ]}
            """.trimIndent(),
        )
        coEvery { kospiDataSource.fetchSnapshot(includeHistory = true, forceRefresh = false) } returns response

        val series = repository.dailyCloses(FearIndexType.KOSPI)

        assertEquals(listOf(8650.0, 8730.5), series.closes)
        val metadata = series.sourceMetadata!!
        assertEquals("KIS/KRX KOSPI", metadata.sourceName)
        assertEquals("KOSPI", metadata.basisLabel)
        assertEquals("2026-07-02", metadata.asOf)
        assertTrue(metadata.isOfficial)
    }
}
