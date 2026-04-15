package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.YahooSparkResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MarketIndexApi {

    @GET("v8/finance/spark")
    suspend fun getSpark(
        @Query("symbols") symbols: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "1d",
    ): YahooSparkResponse
}
