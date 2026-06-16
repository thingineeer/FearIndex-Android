package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.CryptoFearIndexDataSource
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexDateContext
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import th1ngjin.fearindex.domain.usecase.HistoricalAnchorResolver
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoFearIndexRepositoryImpl @Inject constructor(
    private val dataSource: CryptoFearIndexDataSource,
) : CryptoFearIndexRepository {

    override suspend fun fetchCurrent(forceRefresh: Boolean): FearIndex {
        if (ScreenshotMode.isEnabled()) return ScreenshotFixtures.cryptoCurrent()

        // 367일치 단일 fetch (current + 전일/1주전/1개월전/1년전 앵커 전부 커버).
        // iOS CryptoFearIndexRepository(limit:367) + HistoricalAnchorResolver 와 동일.
        val response = dataSource.fetchCurrent(days = ANCHOR_HISTORY_DAYS, forceRefresh = forceRefresh)
        val dto = response.data.first()
        val score = dto.value.toDouble()

        val current = FearIndex(
            score = score,
            rating = FearIndex.Rating.from(score),
            timestamp = Instant.ofEpochSecond(dto.timestamp.toLong()),
        )
        // 비교 카드 앵커를 **날짜 기반**으로 계산 (배열 인덱스 오차 제거). crypto 는 UTC.
        val history = response.data.map { item ->
            FearIndex(
                score = item.value.toDouble(),
                rating = FearIndex.Rating.from(item.value.toDouble()),
                timestamp = Instant.ofEpochSecond(item.timestamp.toLong()),
            )
        }
        return HistoricalAnchorResolver.enrich(
            current = current,
            history = history,
            context = FearIndexDateContext.CRYPTO,
        )
    }

    override suspend fun fetchHistory(days: Int, forceRefresh: Boolean): List<FearIndex> {
        if (ScreenshotMode.isEnabled()) {
            return ScreenshotFixtures.history(days = days, center = 48.0, amplitude = 18.0)
        }

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

    private companion object {
        /** iOS limit:367 과 동일 — 1년 앵커 + 며칠 여유. */
        const val ANCHOR_HISTORY_DAYS = 367
    }
}
