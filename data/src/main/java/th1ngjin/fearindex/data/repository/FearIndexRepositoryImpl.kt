package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.FearIndexDataSource
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.FearIndexRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FearIndexRepositoryImpl @Inject constructor(
    private val dataSource: FearIndexDataSource,
) : FearIndexRepository {

    override suspend fun fetchCurrent(forceRefresh: Boolean): FearIndex {
        // Current score는 짧은 기간(365일) 캐시 재사용
        val response = dataSource.fetchCurrent(days = 365, forceRefresh = forceRefresh)
        val dto = response.fearAndGreed
        return FearIndex(
            score = dto.score,
            rating = FearIndex.Rating.from(dto.score),
            timestamp = parseTimestamp(dto.timestamp),
            previousClose = dto.previousClose,
            previous1Week = dto.previous1Week,
            previous1Month = dto.previous1Month,
            previous1Year = dto.previous1Year,
        )
    }

    override suspend fun fetchHistory(days: Int, forceRefresh: Boolean): List<FearIndex> {
        val response = dataSource.fetchCurrent(days = days, forceRefresh = forceRefresh)
        return response.fearAndGreedHistorical.data.map { point ->
            FearIndex(
                score = point.y,
                rating = FearIndex.Rating.from(point.y),
                timestamp = Instant.ofEpochMilli(point.x.toLong()),
            )
        }.sortedBy { it.timestamp }
    }

    private fun parseTimestamp(ts: String): Instant = try {
        Instant.parse(ts)
    } catch (e: Exception) {
        try {
            Instant.ofEpochMilli(ts.toDouble().toLong())
        } catch (e2: Exception) {
            Instant.now()
        }
    }
}
