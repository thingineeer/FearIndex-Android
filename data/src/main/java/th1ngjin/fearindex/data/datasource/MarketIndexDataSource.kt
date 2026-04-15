package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.YahooSparkResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketIndexDataSource @Inject constructor(
    private val api: MarketIndexApi,
) {
    private data class CacheEntry(
        val response: YahooSparkResponse,
        val timestamp: Long,
    )

    private var cache: CacheEntry? = null
    private val cacheTtl = 5 * 60 * 1000L // 5분

    companion object {
        const val SYMBOLS = "^KS11,^KQ11,^IXIC,^GSPC,^DJI"
    }

    suspend fun fetch(forceRefresh: Boolean = false): YahooSparkResponse {
        val cached = cache
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtl) {
            return cached.response
        }
        val response = api.getSpark(symbols = SYMBOLS)
        cache = CacheEntry(response, System.currentTimeMillis())
        Timber.d("Fetched Yahoo Spark: ${response.spark?.result?.size ?: 0} symbols")
        return response
    }
}
