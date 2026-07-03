package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.data.datasource.CoinGeckoApi
import th1ngjin.fearindex.data.datasource.KospiFearIndexDataSource
import th1ngjin.fearindex.data.datasource.YahooChartApi
import th1ngjin.fearindex.data.dto.CoinGeckoMarketChartResponse
import th1ngjin.fearindex.data.dto.KospiPublicSnapshotResponse
import th1ngjin.fearindex.data.dto.YahooCloseChartResponse
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 자산 일봉 종가 라우팅 — MARKET=Yahoo ^GSPC, CRYPTO=CoinGecko, KOSPI=스냅샷 kospiClose.
 */
class AssetPriceClosesRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val yahooApi = mockk<YahooChartApi>()
    private val coinGeckoApi = mockk<CoinGeckoApi>()
    private val kospiDataSource = mockk<KospiFearIndexDataSource>()
    private val repository = AssetPriceClosesRepositoryImpl(yahooApi, coinGeckoApi, kospiDataSource)

    @Test
    fun `MARKET은 Yahoo GSPC 6mo 일봉 종가`() = runTest {
        val response = json.decodeFromString<YahooCloseChartResponse>(
            """{"chart":{"result":[{"indicators":{"quote":[{"close":[6100.0,null,6120.0]}]}}]}}""",
        )
        coEvery { yahooApi.getCloseChart("^GSPC", "1d", "6mo") } returns response

        val closes = repository.dailyCloses(FearIndexType.MARKET)

        assertEquals(listOf(6100.0, 6120.0), closes)
        coVerify(exactly = 1) { yahooApi.getCloseChart("^GSPC", "1d", "6mo") }
    }

    @Test
    fun `CRYPTO는 CoinGecko market_chart 180일 일봉 종가`() = runTest {
        coEvery { coinGeckoApi.getMarketChart("bitcoin", "usd", 180) } returns
            CoinGeckoMarketChartResponse(
                prices = listOf(listOf(1.0, 66000.0), listOf(2.0, 66500.0)),
            )

        val closes = repository.dailyCloses(FearIndexType.CRYPTO)

        assertEquals(listOf(66000.0, 66500.0), closes)
    }

    @Test
    fun `KOSPI는 스냅샷 chartHistoryForDisplay의 kospiClose(날짜 오름차순)`() = runTest {
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

        val closes = repository.dailyCloses(FearIndexType.KOSPI)

        assertEquals(listOf(8650.0, 8730.5), closes)
    }
}
