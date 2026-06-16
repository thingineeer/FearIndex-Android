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
import java.time.LocalDate
import java.time.ZoneOffset

class CryptoFearIndexRepositoryImplTest {

    private val dataSource = mockk<CryptoFearIndexDataSource>()
    private val repository = CryptoFearIndexRepositoryImpl(dataSource)

    /** UTC 자정 기준 epoch 초. Alternative.me 는 일 단위 UTC timestamp. */
    private fun utcTs(day: String): String =
        LocalDate.parse(day).atStartOfDay(ZoneOffset.UTC).toEpochSecond().toString()

    @Test
    fun `fetchCurrent - Alternative me 응답을 FearIndex 엔티티로 변환`() = runTest {
        // 최신(index 0)이 current
        val response = createResponse(
            listOf(
                CryptoFearIndexDTO(value = "25", valueClassification = "Extreme Fear", timestamp = utcTs("2024-03-04")),
                CryptoFearIndexDTO(value = "30", valueClassification = "Fear", timestamp = utcTs("2024-03-03")),
            ),
        )
        coEvery { dataSource.fetchCurrent(days = 367, forceRefresh = false) } returns response

        val result = repository.fetchCurrent()

        assertEquals(25.0, result.score, 0.01)
        assertEquals(FearIndex.Rating.FEAR, result.rating) // 25 → FEAR
        assertNotNull(result.timestamp)
    }

    @Test
    fun `fetchCurrent - 앵커는 날짜 기반 (배열 인덱스 아님)`() = runTest {
        // iOS HistoricalAnchorResolver 와 동일 케이스. current=2024-03-04.
        // 일부 날짜가 빠져 있어 배열 인덱스로는 틀린 값이 나오는 데이터셋.
        val data = listOf(
            CryptoFearIndexDTO(value = "10", valueClassification = "x", timestamp = utcTs("2024-03-04")), // current
            CryptoFearIndexDTO(value = "15", valueClassification = "x", timestamp = utcTs("2024-03-03")), // 전일
            CryptoFearIndexDTO(value = "45", valueClassification = "x", timestamp = utcTs("2024-02-26")), // 1주전 앵커(02-26 이하 최신)
            CryptoFearIndexDTO(value = "47", valueClassification = "x", timestamp = utcTs("2024-02-02")), // 1개월전 앵커(02-04 이하 최신)
            CryptoFearIndexDTO(value = "88", valueClassification = "x", timestamp = utcTs("2023-03-04")), // 1년전 앵커
        )
        coEvery { dataSource.fetchCurrent(days = 367, forceRefresh = false) } returns createResponse(data)

        val result = repository.fetchCurrent()

        assertEquals(15.0, result.previousClose!!, 0.01)
        assertEquals(45.0, result.previous1Week!!, 0.01)
        assertEquals(47.0, result.previous1Month!!, 0.01)
        assertEquals(88.0, result.previous1Year!!, 0.01)
    }

    @Test
    fun `fetchCurrent - 1년치가 없으면 previous1Year는 null`() = runTest {
        // 최근 데이터만 있고 1년 전 데이터 없음
        val data = listOf(
            CryptoFearIndexDTO(value = "50", valueClassification = "x", timestamp = utcTs("2024-03-04")),
            CryptoFearIndexDTO(value = "45", valueClassification = "x", timestamp = utcTs("2024-03-03")),
        )
        coEvery { dataSource.fetchCurrent(days = 367, forceRefresh = false) } returns createResponse(data)

        val result = repository.fetchCurrent()

        assertEquals(50.0, result.score, 0.01)
        assertNull(result.previous1Year) // 1년전 데이터 없음 → null
    }

    @Test
    fun `fetchCurrent - 전일 앵커는 기준일 직전 데이터`() = runTest {
        val data = listOf(
            CryptoFearIndexDTO(value = "50", valueClassification = "x", timestamp = utcTs("2024-03-04")),
            CryptoFearIndexDTO(value = "45", valueClassification = "x", timestamp = utcTs("2024-03-03")),
        )
        coEvery { dataSource.fetchCurrent(days = 367, forceRefresh = false) } returns createResponse(data)

        val result = repository.fetchCurrent()

        assertEquals(45.0, result.previousClose!!, 0.01)
    }

    @Test
    fun `fetchCurrent - 1주전 앵커는 7일 전 날짜`() = runTest {
        // 연속 8일치: index 7 == 7일 전. 날짜 기반이라도 연속이면 인덱스7과 일치.
        val data = (0..10).map {
            CryptoFearIndexDTO(
                value = "${50 + it}",
                valueClassification = "x",
                timestamp = utcTs(LocalDate.parse("2024-03-04").minusDays(it.toLong()).toString()),
            )
        }
        coEvery { dataSource.fetchCurrent(days = 367, forceRefresh = false) } returns createResponse(data)

        val result = repository.fetchCurrent()

        // 7일 전(2024-02-26) 값 = "57"
        assertEquals(57.0, result.previous1Week!!, 0.01)
    }

    @Test
    fun `fetchCurrent - 데이터가 1개뿐이면 앵커는 현재 점수 fallback (1년전 null)`() = runTest {
        val data = listOf(
            CryptoFearIndexDTO(value = "50", valueClassification = "x", timestamp = utcTs("2024-03-04")),
        )
        coEvery { dataSource.fetchCurrent(days = 367, forceRefresh = false) } returns createResponse(data)

        val result = repository.fetchCurrent()

        // iOS 와 동일: previousClose/1Week/1Month 는 current.score fallback, 1Year 만 null
        assertEquals(50.0, result.previousClose!!, 0.01)
        assertEquals(50.0, result.previous1Week!!, 0.01)
        assertEquals(50.0, result.previous1Month!!, 0.01)
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
        assertEquals(30.0, result[0].score, 0.01)
        assertEquals(50.0, result[1].score, 0.01)
        assertEquals(70.0, result[2].score, 0.01)
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
