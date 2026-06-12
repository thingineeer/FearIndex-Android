package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.CNNFearGreedResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FearIndexDataSource @Inject constructor(
    private val api: CNNFearGreedApi,
) {
    private data class CacheEntry(
        val response: CNNFearGreedResponse,
        val timestamp: Long,
    )

    private val cache = mutableMapOf<Int, CacheEntry>()
    private val cacheTtl = 5 * 60 * 1000L
    private val cacheMutex = Mutex()

    /**
     * CNN Fear & Greed API는 startDate 이후 전체 history를 반환.
     * days에 따라 startDate를 다르게 설정하여 필요한 기간만큼의 데이터를 가져옴.
     */
    suspend fun fetchCurrent(days: Int = DEFAULT_HISTORY_DAYS, forceRefresh: Boolean = false): CNNFearGreedResponse =
        cacheMutex.withLock {
            val requestDays = normalizedRequestDays(days)
            val cached = cache[requestDays]
            if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtl) {
                return@withLock cached.response
            }

            val response = fetchFromApi(requestDays)
            cache[requestDays] = CacheEntry(response, System.currentTimeMillis())
            response
        }

    private suspend fun fetchFromApi(days: Int): CNNFearGreedResponse {
        val startDate = LocalDate.now().minusDays(days.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val response = api.getFearAndGreed(startDate)
        Timber.d("Fetched CNN Fear & Greed Index (days=$days): score=${response.fearAndGreed.score}, history=${response.fearAndGreedHistorical.data.size}")
        return response
    }

    private fun normalizedRequestDays(days: Int): Int =
        if (days <= DEFAULT_HISTORY_DAYS) DEFAULT_HISTORY_DAYS else days

    private companion object {
        const val DEFAULT_HISTORY_DAYS = 365
    }
}
