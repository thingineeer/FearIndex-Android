package th1ngjin.fearindex.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import th1ngjin.fearindex.data.datasource.CryptoFearIndexDataSource
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoFearIndexRepositoryImpl @Inject constructor(
    private val dataSource: CryptoFearIndexDataSource,
) : CryptoFearIndexRepository {

    override suspend fun fetchCurrent(forceRefresh: Boolean): FearIndex = coroutineScope {
        // 31일 데이터 (current + previous1Week + previous1Month) + 365일 데이터 (previous1Year) 병렬 fetch.
        // iOS CryptoFearIndexRepository와 동일한 패턴 — previous1Year만 별도 호출로 네트워크 효율 유지.
        val shortDeferred = async { dataSource.fetchCurrent(days = 31, forceRefresh = forceRefresh) }
        val yearDeferred = async { runCatching { dataSource.fetchCurrent(days = 365, forceRefresh = forceRefresh) } }

        val response = shortDeferred.await()
        val dto = response.data.first()
        val score = dto.value.toDouble()

        val previous1Year = yearDeferred.await().getOrNull()?.let { yearResponse ->
            // Alternative.me는 최신 데이터가 index 0, 가장 오래된 게 끝에. 365일 요청 시 data.size == 365면 마지막 값이 "1년 전".
            yearResponse.data.getOrNull(yearResponse.data.size - 1)?.value?.toDoubleOrNull()
        } ?: run {
            Timber.w("[CryptoFearIndexRepositoryImpl] 1년치 fetch 실패 — previous1Year=null")
            null
        }

        FearIndex(
            score = score,
            rating = FearIndex.Rating.from(score),
            timestamp = Instant.ofEpochSecond(dto.timestamp.toLong()),
            previousClose = findPreviousScore(response.data, 1),
            previous1Week = findPreviousScore(response.data, 7),
            previous1Month = findPreviousScore(response.data, 30),
            previous1Year = previous1Year,
        )
    }

    override suspend fun fetchHistory(days: Int, forceRefresh: Boolean): List<FearIndex> {
        val response = dataSource.fetchCurrent(days = days, forceRefresh = forceRefresh)
        return response.data.map { dto ->
            val score = dto.value.toDouble()
            FearIndex(
                score = score,
                rating = FearIndex.Rating.from(score),
                timestamp = Instant.ofEpochSecond(dto.timestamp.toLong()),
            )
        }.sortedBy { it.timestamp }
    }

    private fun findPreviousScore(data: List<th1ngjin.fearindex.data.dto.CryptoFearIndexDTO>, daysAgo: Int): Double? {
        return data.getOrNull(daysAgo)?.value?.toDoubleOrNull()
    }
}
