package th1ngjin.fearindex.data.repository

import kotlinx.coroutines.withTimeout
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.CoinGeckoApi
import th1ngjin.fearindex.data.datasource.KospiFearIndexDataSource
import th1ngjin.fearindex.data.datasource.YahooChartApi
import th1ngjin.fearindex.data.mapper.CoinGeckoCloseParser
import th1ngjin.fearindex.data.mapper.KospiCloseParser
import th1ngjin.fearindex.data.mapper.YahooCloseParser
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.repository.AssetPriceClosesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자산 일봉 종가 라우팅 — iOS `AssetPriceClosesRepository` 대응.
 * MARKET=Yahoo ^GSPC(6mo), CRYPTO=CoinGecko(180d), KOSPI=기존 스냅샷의 kospiClose(추가 API 없음).
 * 외부 호출은 8초 strict timeout (SDK hang 방어).
 */
@Singleton
class AssetPriceClosesRepositoryImpl @Inject constructor(
    private val yahooApi: YahooChartApi,
    private val coinGeckoApi: CoinGeckoApi,
    private val kospiDataSource: KospiFearIndexDataSource,
) : AssetPriceClosesRepository {

    override suspend fun dailyCloses(type: FearIndexType): List<Double> {
        if (ScreenshotMode.isEnabled()) return emptyList()
        return when (type) {
            FearIndexType.MARKET -> withTimeout(TIMEOUT_MS) {
                YahooCloseParser.closes(yahooApi.getCloseChart(SPX_SYMBOL, "1d", "6mo"))
            }
            FearIndexType.CRYPTO -> withTimeout(TIMEOUT_MS) {
                CoinGeckoCloseParser.closes(coinGeckoApi.getMarketChart())
            }
            FearIndexType.KOSPI -> KospiCloseParser.closes(
                kospiDataSource.fetchSnapshot(includeHistory = true, forceRefresh = false)
                    .chartHistoryForDisplay,
            )
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000L
        const val SPX_SYMBOL = "^GSPC"
    }
}
