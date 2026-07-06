package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.CoinGeckoCoinPrice
import th1ngjin.fearindex.data.dto.ExchangeRateDailyResponse
import th1ngjin.fearindex.data.dto.NaverIndexResponse
import th1ngjin.fearindex.data.dto.YahooChartResponse
import th1ngjin.fearindex.data.dto.YahooCloseChartResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/** Yahoo Finance chart v8 — 개별 심볼 시세 (iOS YahooFinanceStrategy). */
interface YahooChartApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol", encoded = false) symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1d",
    ): YahooChartResponse

    /** 같은 엔드포인트의 종가 시계열 뷰 — RSI(14)용 6개월 일봉 (iOS SPXPriceCloseDataSource). */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getCloseChart(
        @Path("symbol", encoded = false) symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "6mo",
    ): YahooCloseChartResponse
}

/** Naver Finance — 한국 지수(KOSPI/KOSDAQ). symbolCode 는 "KOSPI"/"KOSDAQ". */
interface NaverFinanceApi {
    @GET("api/index/{symbolCode}/basic")
    suspend fun getIndexBasic(
        @Path("symbolCode") symbolCode: String,
    ): NaverIndexResponse
}

/** CoinGecko simple/price — 암호화폐 시세 (동적 키 맵 응답). */
interface CoinGeckoApi {
    @GET("api/v3/simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_24hr_change") include24hChange: Boolean = true,
    ): Map<String, CoinGeckoCoinPrice>
}

/** currency-api — USD 기준 일별 환율. base URL 이 날짜 서브도메인으로 가변이라 @Url 사용. */
interface ExchangeRateApi {
    @GET
    suspend fun getDaily(@Url url: String): ExchangeRateDailyResponse
}
