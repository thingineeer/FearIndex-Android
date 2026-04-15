package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.CryptoFearIndexResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoFearIndexDataSource @Inject constructor(
    private val api: CryptoFearIndexApi,
) {
    private data class CacheEntry(
        val response: CryptoFearIndexResponse,
        val timestamp: Long,
    )

    private val cache = mutableMapOf<Int, CacheEntry>()
    private val cacheTtl = 60 * 60 * 1000L

    /**
     * Alternative.me Crypto Fear Index API는 limit 파라미터로 최근 N일 데이터를 반환.
     */
    suspend fun fetchCurrent(days: Int = 31, forceRefresh: Boolean = false): CryptoFearIndexResponse {
        val cached = cache[days]
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtl) {
            return cached.response
        }
        val response = api.getCryptoFearIndex(limit = days)
        cache[days] = CacheEntry(response, System.currentTimeMillis())
        Timber.d("Fetched Crypto Fear Index (days=$days): size=${response.data.size}, latest=${response.data.firstOrNull()?.value}")
        return response
    }
}
