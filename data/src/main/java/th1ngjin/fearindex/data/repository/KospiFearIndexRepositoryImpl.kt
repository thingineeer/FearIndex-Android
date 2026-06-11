package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.KospiFearIndexDataSource
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.repository.KospiFearIndexRepository
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KospiFearIndexRepositoryImpl @Inject constructor(
    private val dataSource: KospiFearIndexDataSource,
) : KospiFearIndexRepository {

    override suspend fun fetchCurrent(forceRefresh: Boolean): KospiFearIndex {
        val response = dataSource.fetchSnapshot(includeHistory = false, forceRefresh = forceRefresh)
        val latest = response.latest ?: throw IllegalStateException("KOSPI latest snapshot missing")
        if (latest.stale) throw IllegalStateException("KOSPI latest snapshot is stale")

        val current = latest.toDomain(response.generatedAtInstant)
        val history = fetchAnchorHistory(forceRefresh)
        return current.copy(fearIndex = enrich(current.fearIndex, current.dataDate, history))
    }

    override suspend fun fetchHistory(days: Int, forceRefresh: Boolean): List<FearIndex> {
        if (days <= 0) return emptyList()
        val response = dataSource.fetchSnapshot(includeHistory = true, forceRefresh = forceRefresh)
        return response.chartHistoryForDisplay
            .map { it.toDomain() }
            .sortedBy { it.timestamp }
            .takeLast(days)
    }

    private suspend fun fetchAnchorHistory(forceRefresh: Boolean): List<FearIndex> =
        runCatching {
            dataSource.fetchSnapshot(includeHistory = true, forceRefresh = forceRefresh)
                .history
                .map { it.toDomain() }
                .sortedBy { it.timestamp }
        }.getOrElse { error ->
            Timber.w(error, "[KospiFearIndexRepositoryImpl] KOSPI anchor history fetch failed")
            emptyList()
        }

    private fun enrich(current: FearIndex, dataDate: String, history: List<FearIndex>): FearIndex {
        val referenceDate = parseDay(dataDate) ?: current.timestamp.atZone(ZoneOffset.UTC).toLocalDate()
        return current.copy(
            previousClose = anchor(history, referenceDate, daysAgo = 1) ?: current.previousClose,
            previous1Week = anchor(history, referenceDate, daysAgo = 7) ?: current.previous1Week,
            previous1Month = anchor(history, referenceDate, daysAgo = 30) ?: current.previous1Month,
            previous1Year = anchor(history, referenceDate, daysAgo = 365) ?: current.previous1Year,
        )
    }

    private fun anchor(history: List<FearIndex>, referenceDate: LocalDate, daysAgo: Long): Double? {
        val target = referenceDate.minusDays(daysAgo)
        return history
            .filter { it.timestamp.atZone(ZoneOffset.UTC).toLocalDate() <= target }
            .maxByOrNull { it.timestamp }
            ?.score
    }

    private fun parseDay(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()
}
