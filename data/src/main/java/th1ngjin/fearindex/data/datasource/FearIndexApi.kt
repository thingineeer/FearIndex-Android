package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.CNNFearGreedResponse
import th1ngjin.fearindex.data.dto.CryptoFearIndexResponse
import th1ngjin.fearindex.data.dto.KospiPublicSnapshotResponse
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

interface KospiFearIndexApi {
    @GET("api/kospi/v2")
    suspend fun getKospiFearIndex(
        @Query("v") version: String = "20260610",
        @Query("history") history: Int? = null,
    ): KospiPublicSnapshotResponse
}
