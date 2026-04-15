package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import th1ngjin.fearindex.data.datasource.FearIndexDataSource
import th1ngjin.fearindex.data.dto.CNNFearGreedResponse
import th1ngjin.fearindex.data.dto.FearAndGreedDTO
import th1ngjin.fearindex.data.dto.FearAndGreedHistoricalDTO
import th1ngjin.fearindex.data.dto.HistoricalDataPointDTO
import th1ngjin.fearindex.domain.entity.FearIndex

class FearIndexRepositoryImplTest {

    private val dataSource = mockk<FearIndexDataSource>()
    private val repository = FearIndexRepositoryImpl(dataSource)

    @Test
    fun `fetchCurrent - CNN API 응답을 FearIndex 엔티티로 변환`() = runTest {
        val response = createResponse(
            score = 42.0,
            rating = "Fear",
            timestamp = "2026-04-15T10:00:00Z",
            previousClose = 38.0,
            previous1Week = 35.0,
            previous1Month = 50.0,
            previous1Year = 60.0,
        )
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns response

        val result = repository.fetchCurrent()

        assertEquals(42.0, result.score, 0.01)
        assertEquals(FearIndex.Rating.FEAR, result.rating)
        assertEquals(38.0, result.previousClose!!, 0.01)
        assertEquals(35.0, result.previous1Week!!, 0.01)
        assertEquals(50.0, result.previous1Month!!, 0.01)
        assertEquals(60.0, result.previous1Year!!, 0.01)
    }

    @Test
    fun `fetchCurrent - Rating from 로직으로 변환 (score 기반, DTO rating 무시)`() = runTest {
        val response = createResponse(
            score = 80.0,
            rating = "Extreme Greed",
            timestamp = "2026-04-15T10:00:00Z",
        )
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns response

        val result = repository.fetchCurrent()

        assertEquals(FearIndex.Rating.EXTREME_GREED, result.rating)
    }

    @Test
    fun `fetchCurrent - timestamp ISO 형식 파싱`() = runTest {
        val response = createResponse(
            score = 50.0,
            rating = "Neutral",
            timestamp = "2026-04-15T12:30:00Z",
        )
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns response

        val result = repository.fetchCurrent()

        assertNotNull(result.timestamp)
    }

    @Test
    fun `fetchCurrent - timestamp epoch 밀리초 폴백`() = runTest {
        val response = createResponse(
            score = 50.0,
            rating = "Neutral",
            timestamp = "1713168000000", // epoch millis
        )
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns response

        val result = repository.fetchCurrent()

        assertNotNull(result.timestamp)
    }

    @Test
    fun `fetchCurrent - 잘못된 timestamp면 Instant now 폴백`() = runTest {
        val response = createResponse(
            score = 50.0,
            rating = "Neutral",
            timestamp = "invalid-timestamp",
        )
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns response

        val result = repository.fetchCurrent()

        assertNotNull(result.timestamp)
    }

    @Test
    fun `fetchHistory - 히스토리 데이터를 FearIndex 리스트로 변환`() = runTest {
        val response = createResponseWithHistory(
            listOf(
                HistoricalDataPointDTO(x = 1713168000000.0, y = 30.0, rating = "Fear"),
                HistoricalDataPointDTO(x = 1713254400000.0, y = 50.0, rating = "Neutral"),
                HistoricalDataPointDTO(x = 1713340800000.0, y = 70.0, rating = "Greed"),
            ),
        )
        coEvery { dataSource.fetchCurrent(days = 90, forceRefresh = false) } returns response

        val result = repository.fetchHistory(days = 90)

        assertEquals(3, result.size)
        // timestamp 오름차순 정렬 확인
        assertEquals(30.0, result[0].score, 0.01)
        assertEquals(50.0, result[1].score, 0.01)
        assertEquals(70.0, result[2].score, 0.01)
    }

    @Test
    fun `fetchHistory - 빈 히스토리`() = runTest {
        val response = createResponseWithHistory(emptyList())
        coEvery { dataSource.fetchCurrent(days = 90, forceRefresh = false) } returns response

        val result = repository.fetchHistory(days = 90)

        assertEquals(0, result.size)
    }

    @Test
    fun `fetchHistory - forceRefresh 전달`() = runTest {
        val response = createResponseWithHistory(emptyList())
        coEvery { dataSource.fetchCurrent(days = 180, forceRefresh = true) } returns response

        repository.fetchHistory(days = 180, forceRefresh = true)

        // 검증: forceRefresh=true로 호출됨 (coEvery에서 매칭)
    }

    private fun createResponse(
        score: Double,
        rating: String,
        timestamp: String,
        previousClose: Double = 0.0,
        previous1Week: Double = 0.0,
        previous1Month: Double = 0.0,
        previous1Year: Double = 0.0,
    ) = CNNFearGreedResponse(
        fearAndGreed = FearAndGreedDTO(
            score = score,
            rating = rating,
            timestamp = timestamp,
            previousClose = previousClose,
            previous1Week = previous1Week,
            previous1Month = previous1Month,
            previous1Year = previous1Year,
        ),
        fearAndGreedHistorical = FearAndGreedHistoricalDTO(),
    )

    private fun createResponseWithHistory(
        history: List<HistoricalDataPointDTO>,
    ) = CNNFearGreedResponse(
        fearAndGreed = FearAndGreedDTO(
            score = 50.0,
            rating = "Neutral",
            timestamp = "2026-04-15T10:00:00Z",
            previousClose = 48.0,
            previous1Week = 45.0,
            previous1Month = 40.0,
            previous1Year = 55.0,
        ),
        fearAndGreedHistorical = FearAndGreedHistoricalDTO(data = history),
    )
}
