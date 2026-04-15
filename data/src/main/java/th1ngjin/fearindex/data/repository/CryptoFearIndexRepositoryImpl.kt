package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.CryptoFearIndexDataSource
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoFearIndexRepositoryImpl @Inject constructor(
    private val dataSource: CryptoFearIndexDataSource,
) : CryptoFearIndexRepository {

    override suspend fun fetchCurrent(forceRefresh: Boolean): FearIndex {
        // Current score는 31일 캐시 재사용 (previous1Month 계산용)
        val response = dataSource.fetchCurrent(days = 31, forceRefresh = forceRefresh)
        val dto = response.data.first()
        val score = dto.value.toDouble()
        return FearIndex(
            score = score,
            rating = FearIndex.Rating.from(score),
            timestamp = Instant.ofEpochSecond(dto.timestamp.toLong()),
            previousClose = findPreviousScore(response.data, 1),
            previous1Week = findPreviousScore(response.data, 7),
            previous1Month = findPreviousScore(response.data, 30),
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
