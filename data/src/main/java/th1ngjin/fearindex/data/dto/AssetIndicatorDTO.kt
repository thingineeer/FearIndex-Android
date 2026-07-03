package th1ngjin.fearindex.data.dto

import kotlinx.serialization.Serializable

/**
 * RSI/공매도 보조 지표용 DTO — iOS `AssetPriceCloseParser`/`AssetShortRatioParser`의 응답 모델 대칭.
 */

/** Yahoo chart v8 — 종가 시계열용 (기존 YahooChartResponse는 meta 전용이라 별도 선언). */
@Serializable
data class YahooCloseChartResponse(val chart: Chart) {
    @Serializable
    data class Chart(val result: List<Result>? = null)

    @Serializable
    data class Result(val indicators: Indicators = Indicators())

    @Serializable
    data class Indicators(val quote: List<Quote> = emptyList())

    @Serializable
    data class Quote(val close: List<Double?> = emptyList())
}

/** CoinGecko `/coins/{id}/market_chart` — days>=91이면 자동 일봉, prices[][1]=종가. */
@Serializable
data class CoinGeckoMarketChartResponse(
    val prices: List<List<Double>> = emptyList(),
)

/** Binance Futures `globalLongShortAccountRatio` 행 — 응답 수치는 전부 String. */
@Serializable
data class BinanceLongShortRatioDTO(
    val shortAccount: String,
    val timestamp: Long,
)

/** 서버 `/api/kospi/short` — KIS 시총상위 합산 공매도 비중(%) 시계열. */
@Serializable
data class KospiShortResponse(
    val shortRatios: List<Double> = emptyList(),
)
