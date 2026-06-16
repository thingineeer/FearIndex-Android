package th1ngjin.fearindex.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────── Yahoo Finance (chart v8) ───────────────────────────
// GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d
// iOS YahooFinanceStrategy 와 1:1.

@Serializable
data class YahooChartResponse(
    val chart: YahooChart = YahooChart(),
)

@Serializable
data class YahooChart(
    val result: List<YahooChartResult>? = null,
    val error: YahooChartError? = null,
)

@Serializable
data class YahooChartResult(
    val meta: YahooChartMeta? = null,
)

@Serializable
data class YahooChartMeta(
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
    val previousClose: Double? = null,
)

@Serializable
data class YahooChartError(
    val code: String? = null,
    val description: String? = null,
)

// ─────────────────────────── Naver Finance (KOSPI/KOSDAQ) ───────────────────────────
// GET https://m.stock.naver.com/api/index/{KOSPI|KOSDAQ}/basic
// 모든 필드 String. price=closePrice, change=compareToPreviousClosePrice, changePercent=fluctuationsRatio.

@Serializable
data class NaverIndexResponse(
    val itemCode: String = "",
    val stockName: String = "",
    val closePrice: String = "",
    val compareToPreviousClosePrice: String = "",
    val fluctuationsRatio: String = "",
    val marketStatus: String = "",
)

// ─────────────────────────── CoinGecko (simple/price) ───────────────────────────
// GET https://api.coingecko.com/api/v3/simple/price?ids=...&vs_currencies=usd&include_24hr_change=true
// 응답: { "bitcoin": { "usd": 67000.0, "usd_24h_change": -2.5 }, ... } — 동적 키 맵.

@Serializable
data class CoinGeckoCoinPrice(
    val usd: Double = 0.0,
    @SerialName("usd_24h_change") val usd24hChange: Double? = null,
)

// ─────────────────────────── currency-api (USD → KRW) ───────────────────────────
// GET https://latest.currency-api.pages.dev/v1/currencies/usd.json
// 응답: { "date": "2026-06-15", "usd": { "krw": 1380.5, ... } }

@Serializable
data class ExchangeRateDailyResponse(
    val date: String = "",
    val usd: Map<String, Double> = emptyMap(),
)
