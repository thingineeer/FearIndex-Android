package th1ngjin.fearindex.data.datasource

import th1ngjin.fearindex.data.dto.KospiPublicSnapshotResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KospiFearIndexDataSource @Inject constructor(
    private val api: KospiFearIndexApi,
) {
    private data class CacheEntry(
        val response: KospiPublicSnapshotResponse,
        val timestamp: Long,
    )

    private val cache = mutableMapOf<Boolean, CacheEntry>()
    private val cacheTtl = 5 * 60 * 1000L

    suspend fun fetchSnapshot(
        includeHistory: Boolean,
        forceRefresh: Boolean = false,
    ): KospiPublicSnapshotResponse {
        val cached = cache[includeHistory]
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtl) {
            return cached.response
        }

        val response = api.getKospiFearIndex(
            version = SNAPSHOT_VERSION,
            history = if (includeHistory) 1 else null,
        )
        cache[includeHistory] = CacheEntry(response, System.currentTimeMillis())
        Timber.d(
            "Fetched KOSPI Fear Index (history=$includeHistory): latest=${response.latest?.score}, history=${response.history.size}",
        )
        return response
    }

    private companion object {
        const val SNAPSHOT_VERSION = "20260610"
    }
}
