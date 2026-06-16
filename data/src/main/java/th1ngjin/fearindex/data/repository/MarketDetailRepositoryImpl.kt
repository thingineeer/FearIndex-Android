package th1ngjin.fearindex.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import th1ngjin.fearindex.data.datasource.CoinGeckoApi
import th1ngjin.fearindex.data.datasource.ExchangeRateApi
import th1ngjin.fearindex.data.datasource.NaverFinanceApi
import th1ngjin.fearindex.data.datasource.YahooChartApi
import th1ngjin.fearindex.domain.entity.CryptoCoinType
import th1ngjin.fearindex.domain.entity.CryptoPrice
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.entity.MarketIndexType
import th1ngjin.fearindex.domain.repository.MarketDetailRepository
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketDetailRepositoryImpl @Inject constructor(
    private val yahooApi: YahooChartApi,
    private val naverApi: NaverFinanceApi,
    private val coinGeckoApi: CoinGeckoApi,
    private val exchangeApi: ExchangeRateApi,
) : MarketDetailRepository {

    override suspend fun getIndices(forceRefresh: Boolean): List<MarketIndex> = coroutineScope {
        val now = Instant.now()
        // 지수 탭(글로벌 7 + 한국 2) + 환율 탭용 DXY(달러지수) 병렬 조회.
        // DXY 는 indices 탭엔 안 나오고 FX 탭에서 사용 (iOS exchangeIndexCases parity).
        // 실패 심볼은 스킵(iOS withTaskGroup parity).
        val types = MarketIndexType.indicesTabCases + MarketIndexType.exchangeIndexCases
        val deferred = types.map { type ->
            async { runCatching { fetchIndex(type, now) }.getOrNull() }
        }
        deferred.mapNotNull { it.await() }
    }

    private suspend fun fetchIndex(type: MarketIndexType, now: Instant): MarketIndex? {
        return if (type.usesYahooFinance) {
            val response = yahooApi.getChart(symbol = type.symbol)
            val meta = response.chart.result?.firstOrNull()?.meta ?: return null
            MarketDetailMapper.yahooToIndex(type, meta, now)
        } else {
            // 한국 지수: rawValue(^KS11)가 아니라 공식 심볼(KOSPI/KOSDAQ)로 호출
            val dto = naverApi.getIndexBasic(symbolCode = type.officialSymbol)
            MarketDetailMapper.naverToIndex(type, dto, now)
        }
    }

    override suspend fun getCryptoPrices(forceRefresh: Boolean): List<CryptoPrice> {
        val now = Instant.now()
        val prices = coinGeckoApi.getSimplePrice(ids = CryptoCoinType.allIds)
        // enum 선언 순서(BTC/ETH/XRP/SOL/BNB) 유지, 응답에 없으면 스킵
        return CryptoCoinType.entries.mapNotNull { coin ->
            val data = prices[coin.id] ?: return@mapNotNull null
            CryptoPrice(
                id = coin.id,
                symbol = coin.symbol,
                name = coin.displayName,
                price = data.usd,
                change24h = data.usd24hChange ?: 0.0,
                timestamp = now,
            )
        }
    }

    override suspend fun getUsdKrwRate(forceRefresh: Boolean): ExchangeRateQuote? {
        val latest = runCatching { exchangeApi.getDaily(LATEST_URL) }.getOrElse {
            Timber.w(it, "[MarketDetailRepositoryImpl] 환율 최신 fetch 실패")
            return null
        }
        val rate = latest.usd["krw"] ?: return null
        // 이전일 환율 (실패해도 change 만 null, 전체 실패 아님)
        val previousRate = MarketDetailMapper.previousDateString(latest.date)?.let { prevDate ->
            runCatching { exchangeApi.getDaily(dailyUrl(prevDate)).usd["krw"] }.getOrNull()
        }
        return MarketDetailMapper.exchangeQuote(rate = rate, previousRate = previousRate, date = latest.date)
    }

    private fun dailyUrl(date: String): String =
        "https://$date.currency-api.pages.dev/v1/currencies/usd.json"

    private companion object {
        const val LATEST_URL = "https://latest.currency-api.pages.dev/v1/currencies/usd.json"
    }
}
