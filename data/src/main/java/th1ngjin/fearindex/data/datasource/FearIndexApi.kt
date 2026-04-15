package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.CNNFearGreedResponse
import th1ngjin.fearindex.data.dto.CryptoFearIndexResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CNNFearGreedApi {
    @GET("index/fearandgreed/graphdata/{startDate}")
    suspend fun getFearAndGreed(@Path("startDate") startDate: String): CNNFearGreedResponse
}

interface CryptoFearIndexApi {
    @GET("fng/")
    suspend fun getCryptoFearIndex(
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ): CryptoFearIndexResponse
}
