package th1ngjin.fearindex.data.datasource

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val cacheMutex = Mutex()

    suspend fun fetchSnapshot(
        includeHistory: Boolean,
        forceRefresh: Boolean = false,
    ): KospiPublicSnapshotResponse = cacheMutex.withLock {
        val cached = cache[includeHistory]
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtl) {
            return@withLock cached.response
        }

        val response = api.getKospiFearIndex(
            version = SNAPSHOT_VERSION,
            history = if (includeHistory) 1 else null,
        )
        cache[includeHistory] = CacheEntry(response, System.currentTimeMillis())
        Timber.d(
            "Fetched KOSPI Fear Index (history=$includeHistory): latest=${response.latest?.score}, history=${response.history.size}",
        )
        response
    }

    private companion object {
        const val SNAPSHOT_VERSION = "20260610"
    }
}
