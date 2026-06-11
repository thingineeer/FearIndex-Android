package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import th1ngjin.fearindex.data.datasource.KospiFearIndexDataSource
import th1ngjin.fearindex.data.dto.KospiHistoryDTO
import th1ngjin.fearindex.data.dto.KospiLatestDTO
import th1ngjin.fearindex.data.dto.KospiPublicSnapshotResponse
import th1ngjin.fearindex.data.dto.KospiSignalScoreDTO
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiSnapshotType

class KospiFearIndexRepositoryImplTest {

    private val dataSource = mockk<KospiFearIndexDataSource>()
    private val repository = KospiFearIndexRepositoryImpl(dataSource)

    @Test
    fun `fetchCurrent - latest snapshot과 history anchor를 KOSPI domain으로 변환한다`() = runTest {
        coEvery { dataSource.fetchSnapshot(includeHistory = false, forceRefresh = true) } returns createResponse(
            latest = createLatest(score = 44.9, intScore = 45, dataDate = "2026-06-11"),
        )
        coEvery { dataSource.fetchSnapshot(includeHistory = true, forceRefresh = true) } returns createResponse(
            latest = createLatest(),
            history = listOf(
                createHistory("2025-06-11", 75.8, 76),
                createHistory("2026-05-12", 60.7, 61),
                createHistory("2026-06-04", 43.0, 43),
                createHistory("2026-06-10", 47.1, 47),
            ),
        )

        val result = repository.fetchCurrent(forceRefresh = true)

        assertEquals(44.9, result.fearIndex.score, 0.01)
        assertEquals(FearIndex.Rating.NEUTRAL, result.fearIndex.rating)
        assertEquals(47.1, result.fearIndex.previousClose!!, 0.01)
        assertEquals(43.0, result.fearIndex.previous1Week!!, 0.01)
        assertEquals(60.7, result.fearIndex.previous1Month!!, 0.01)
        assertEquals(75.8, result.fearIndex.previous1Year!!, 0.01)
        assertEquals(KospiSnapshotType.INTRADAY, result.snapshotType)
        assertFalse(result.isFinal)
        coVerify(exactly = 1) { dataSource.fetchSnapshot(includeHistory = false, forceRefresh = true) }
        coVerify(exactly = 1) { dataSource.fetchSnapshot(includeHistory = true, forceRefresh = true) }
    }

    @Test(expected = IllegalStateException::class)
    fun `fetchCurrent - latest가 없으면 예외`() = runTest {
        coEvery { dataSource.fetchSnapshot(includeHistory = false, forceRefresh = false) } returns createResponse(latest = null)

        repository.fetchCurrent()
    }

    @Test(expected = IllegalStateException::class)
    fun `fetchCurrent - stale latest면 예외`() = runTest {
        coEvery { dataSource.fetchSnapshot(includeHistory = false, forceRefresh = false) } returns createResponse(
            latest = createLatest(stale = true),
        )

        repository.fetchCurrent()
    }

    @Test
    fun `fetchHistory - chartHistory 우선 사용 후 days만큼 최신 행을 반환한다`() = runTest {
        coEvery { dataSource.fetchSnapshot(includeHistory = true, forceRefresh = false) } returns createResponse(
            latest = createLatest(),
            history = listOf(createHistory("2026-06-08", 70.0, 70)),
            chartHistory = listOf(
                createHistory("2026-06-09", 25.2, 25),
                createHistory("2026-06-10", 18.4, 18),
                createHistory("2026-06-11", 15.3, 15),
            ),
        )

        val result = repository.fetchHistory(days = 2)

        assertEquals(listOf(18.4, 15.3), result.map { it.score })
    }

    @Test
    fun `fetchHistory - days가 0 이하이면 빈 리스트`() = runTest {
        val result = repository.fetchHistory(days = 0)

        assertEquals(emptyList<FearIndex>(), result)
        coVerify(exactly = 0) { dataSource.fetchSnapshot(any(), any()) }
    }

    private fun createResponse(
        latest: KospiLatestDTO?,
        history: List<KospiHistoryDTO> = emptyList(),
        chartHistory: List<KospiHistoryDTO>? = null,
    ) = KospiPublicSnapshotResponse(
        version = 2,
        generatedAt = "2026-06-11T01:00:06.211Z",
        latest = latest,
        history = history,
        chartHistory = chartHistory,
        historyCount = history.size,
    )

    private fun createLatest(
        score: Double = 44.9,
        intScore: Int = 45,
        dataDate: String = "2026-06-11",
        stale: Boolean = false,
    ) = KospiLatestDTO(
        score = score,
        rating = "fear",
        confidence = "high",
        signals = listOf(KospiSignalScoreDTO("kospiMomentum", 55.5, 0.18, "price")),
        missingSignals = listOf("marketVolatility"),
        clusterScores = mapOf("price" to 44.0, "credit" to null),
        clusterDivergence = 6.7,
        updatedAt = 1_781_139_607_154.0,
        dataSource = "kis_ecos_v2",
        date = dataDate,
        dataDate = dataDate,
        intScore = intScore,
        snapshotType = "intraday",
        isFinal = false,
        stale = stale,
    )

    private fun createHistory(date: String, score: Double, intScore: Int) = KospiHistoryDTO(
        date = date,
        score = score,
        rating = "neutral",
        confidence = "high",
        clusterDivergence = 1.0,
        intScore = intScore,
    )
}
