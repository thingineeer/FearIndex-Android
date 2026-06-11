package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.data.datasource.CryptoFearIndexDataSource
import th1ngjin.fearindex.data.dto.CryptoFearIndexDTO
import th1ngjin.fearindex.data.dto.CryptoFearIndexResponse
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.domain.entity.FearIndex

class CryptoFearIndexRepositoryImplTest {

    private val dataSource = mockk<CryptoFearIndexDataSource>()
    private val repository = CryptoFearIndexRepositoryImpl(dataSource)

    @Test
    fun `fetchCurrent - Alternative me 응답을 FearIndex 엔티티로 변환`() = runTest {
        val response = createResponse(
            listOf(
                CryptoFearIndexDTO(value = "25", valueClassification = "Extreme Fear", timestamp = "1713168000"),
                CryptoFearIndexDTO(value = "30", valueClassification = "Fear", timestamp = "1713081600"),
            ),
        )
        coEvery { dataSource.fetchCurrent(days = 31, forceRefresh = false) } returns response
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns createResponse(emptyList())

        val result = repository.fetchCurrent()

        assertEquals(25.0, result.score, 0.01)
        assertEquals(FearIndex.Rating.FEAR, result.rating) // 25 → FEAR
        assertNotNull(result.timestamp)
    }

    @Test
    fun `fetchCurrent - previous1Year는 365일치 응답의 마지막 원소`() = runTest {
        // 31일 응답 (current 용)
        val short = (0..30).map {
            CryptoFearIndexDTO(value = "50", valueClassification = "Neutral", timestamp = "${1713168000 - it * 86400}")
        }
        // 365일 응답 — 마지막 원소가 "1년 전"
        val yearly = (0..364).map { i ->
            val value = if (i == 364) "22" else "55"
            CryptoFearIndexDTO(value = value, valueClassification = "Fear", timestamp = "${1713168000 - i * 86400}")
        }
        coEvery { dataSource.fetchCurrent(days = 31, forceRefresh = false) } returns createResponse(short)
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns createResponse(yearly)

        val result = repository.fetchCurrent()

        assertEquals(22.0, result.previous1Year!!, 0.01)
    }

    @Test
    fun `fetchCurrent - 365일치 fetch 실패 시 previous1Year는 null`() = runTest {
        val short = listOf(
            CryptoFearIndexDTO(value = "50", valueClassification = "Neutral", timestamp = "1713168000"),
            CryptoFearIndexDTO(value = "45", valueClassification = "Fear", timestamp = "1713081600"),
        )
        coEvery { dataSource.fetchCurrent(days = 31, forceRefresh = false) } returns createResponse(short)
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } throws RuntimeException("Network failure")

        val result = repository.fetchCurrent()

        // 예외가 발생해도 fetchCurrent 전체는 실패하지 않고 previous1Year만 null
        assertEquals(50.0, result.score, 0.01)
        assertNull(result.previous1Year)
    }

    @Test
    fun `fetchCurrent - previousClose는 data 1번 인덱스`() = runTest {
        val data = listOf(
            CryptoFearIndexDTO(value = "50", valueClassification = "Neutral", timestamp = "1713168000"),
            CryptoFearIndexDTO(value = "45", valueClassification = "Fear", timestamp = "1713081600"),
        )
        coEvery { dataSource.fetchCurrent(days = 31, forceRefresh = false) } returns createResponse(data)
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns createResponse(emptyList())

        val result = repository.fetchCurrent()

        assertEquals(45.0, result.previousClose!!, 0.01)
    }

    @Test
    fun `fetchCurrent - previous1Week은 data 7번 인덱스`() = runTest {
        val data = (0..10).map {
            CryptoFearIndexDTO(value = "${50 + it}", valueClassification = "Neutral", timestamp = "${1713168000 - it * 86400}")
        }
        coEvery { dataSource.fetchCurrent(days = 31, forceRefresh = false) } returns createResponse(data)
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns createResponse(emptyList())

        val result = repository.fetchCurrent()

        assertEquals(57.0, result.previous1Week!!, 0.01)
    }

    @Test
    fun `fetchCurrent - 데이터가 부족하면 previous 값은 null`() = runTest {
        val data = listOf(
            CryptoFearIndexDTO(value = "50", valueClassification = "Neutral", timestamp = "1713168000"),
        )
        coEvery { dataSource.fetchCurrent(days = 31, forceRefresh = false) } returns createResponse(data)
        coEvery { dataSource.fetchCurrent(days = 365, forceRefresh = false) } returns createResponse(emptyList())

        val result = repository.fetchCurrent()

        assertNull(result.previousClose)
        assertNull(result.previous1Week)
        assertNull(result.previous1Month)
        assertNull(result.previous1Year)
    }

    @Test
    fun `fetchHistory - 히스토리 변환 및 오름차순 정렬`() = runTest {
        val data = listOf(
            CryptoFearIndexDTO(value = "70", valueClassification = "Greed", timestamp = "1713340800"),
            CryptoFearIndexDTO(value = "30", valueClassification = "Fear", timestamp = "1713168000"),
            CryptoFearIndexDTO(value = "50", valueClassification = "Neutral", timestamp = "1713254400"),
        )
        coEvery { dataSource.fetchCurrent(days = 7, forceRefresh = false) } returns createResponse(data)

        val result = repository.fetchHistory(days = 7)

        assertEquals(3, result.size)
        // timestamp 오름차순 정렬
        assertEquals(30.0, result[0].score, 0.01) // 가장 이른 timestamp
        assertEquals(50.0, result[1].score, 0.01)
        assertEquals(70.0, result[2].score, 0.01) // 가장 늦은 timestamp
    }

    @Test
    fun `fetchHistory - 빈 데이터`() = runTest {
        coEvery { dataSource.fetchCurrent(days = 7, forceRefresh = false) } returns createResponse(emptyList())

        val result = repository.fetchHistory(days = 7)

        assertEquals(0, result.size)
    }

    @Test
    fun `screenshot mode - network 없이 crypto fixture를 반환한다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)
        try {
            val current = repository.fetchCurrent()
            val history = repository.fetchHistory(days = 5)

            assertEquals(39.0, current.score, 0.01)
            assertEquals(5, history.size)
            coVerify(exactly = 0) { dataSource.fetchCurrent(any(), any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    private fun createResponse(data: List<CryptoFearIndexDTO>) = CryptoFearIndexResponse(
        name = "Fear and Greed Index",
        data = data,
    )
}
