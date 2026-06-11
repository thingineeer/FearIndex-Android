package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.ReturnDataSource
import th1ngjin.fearindex.data.datasource.ReturnDataSourceException
import th1ngjin.fearindex.data.dto.HistoricalReturnsDTO
import th1ngjin.fearindex.data.dto.ReturnDataDTO
import th1ngjin.fearindex.data.dto.ReturnDataPointDTO
import th1ngjin.fearindex.data.dto.ReturnEventEntryDTO
import th1ngjin.fearindex.domain.defaults.DefaultReturnData
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * ReturnDataRepositoryImpl 단위 테스트.
 *
 * 전략:
 * 1. Firestore 성공 시 DTO를 Entity로 변환해 반환
 * 2. 각종 예외(NoData / SchemaMismatch / Network) 발생 시 DefaultReturnData fallback
 * 3. 같은 indexType 재호출 시 DataSource 호출 안 함 (캐시)
 */
class ReturnDataRepositoryImplTest {

    private val dataSource = mockk<ReturnDataSource>()
    private val repository = ReturnDataRepositoryImpl(dataSource)

    @Test
    fun `fetch market - Firestore 성공 시 서버 데이터 반환`() = runTest {
        val dto = buildValidDTO(eventId = "iran-war-2025", eventScore = 8)
        coEvery { dataSource.fetch("market") } returns dto

        val result = repository.fetch(FearIndexType.MARKET)

        assertEquals(2, result.version)
        assertEquals(101, result.dataPoints.size)
        assertEquals(1, result.historicalEvents.size)
        assertEquals("iran-war-2025", result.historicalEvents.first().id)
    }

    @Test
    fun `fetch market - NoData 예외 시 DefaultReturnData 반환`() = runTest {
        coEvery { dataSource.fetch("market") } throws ReturnDataSourceException.NoData

        val result = repository.fetch(FearIndexType.MARKET)

        assertSame(DefaultReturnData.market, result)
    }

    @Test
    fun `fetch market - SchemaMismatch 예외 시 DefaultReturnData 반환`() = runTest {
        coEvery { dataSource.fetch("market") } throws
            ReturnDataSourceException.SchemaMismatch(listOf("version"))

        val result = repository.fetch(FearIndexType.MARKET)

        assertSame(DefaultReturnData.market, result)
    }

    @Test
    fun `fetch market - Network 예외 시 DefaultReturnData 반환`() = runTest {
        coEvery { dataSource.fetch("market") } throws
            ReturnDataSourceException.Network(RuntimeException("timeout"))

        val result = repository.fetch(FearIndexType.MARKET)

        assertSame(DefaultReturnData.market, result)
    }

    @Test
    fun `fetch crypto - 실패 시 Crypto fallback`() = runTest {
        coEvery { dataSource.fetch("crypto") } throws ReturnDataSourceException.NoData

        val result = repository.fetch(FearIndexType.CRYPTO)

        assertSame(DefaultReturnData.crypto, result)
    }

    @Test
    fun `fetch kospi - Firestore 성공 시 kospi 문서 데이터 반환`() = runTest {
        val dto = buildValidDTO(eventId = "kospi-stress", eventScore = 22)
        coEvery { dataSource.fetch("kospi") } returns dto

        val result = repository.fetch(FearIndexType.KOSPI)

        assertEquals("kospi-stress", result.historicalEvents.first().id)
        coVerify(exactly = 1) { dataSource.fetch("kospi") }
    }

    @Test
    fun `fetch kospi - 실패 시 Kospi fallback`() = runTest {
        coEvery { dataSource.fetch("kospi") } throws ReturnDataSourceException.NoData

        val result = repository.fetch(FearIndexType.KOSPI)

        assertSame(DefaultReturnData.kospi, result)
        assertEquals(101, result.dataPoints.size)
        assertEquals(7, result.historicalEvents.size)
        assertEquals(22, result.dataPoints.first { it.score == 50 }.sampleCount)
        assertEquals("kospi-tariff-2025", result.historicalEvents[1].id)
    }

    @Test
    fun `screenshot mode - Firestore 없이 DefaultReturnData를 반환한다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            val result = repository.fetch(FearIndexType.KOSPI)

            assertSame(DefaultReturnData.kospi, result)
            coVerify(exactly = 0) { dataSource.fetch(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `fetch market - 2회 호출 시 DataSource는 1번만 호출 (캐시)`() = runTest {
        coEvery { dataSource.fetch("market") } returns buildValidDTO()

        val first = repository.fetch(FearIndexType.MARKET)
        val second = repository.fetch(FearIndexType.MARKET)

        assertNotNull(first)
        assertNotNull(second)
        coVerify(exactly = 1) { dataSource.fetch("market") }
    }

    @Test
    fun `fetch market fallback 후 재호출 시 DefaultReturnData도 캐싱`() = runTest {
        coEvery { dataSource.fetch("market") } throws ReturnDataSourceException.NoData

        val first = repository.fetch(FearIndexType.MARKET)
        val second = repository.fetch(FearIndexType.MARKET)

        assertSame(DefaultReturnData.market, first)
        assertSame(DefaultReturnData.market, second)
        coVerify(exactly = 1) { dataSource.fetch("market") }
    }

    // ---- helpers ----

    /**
     * 101개 dataPoints + 1개 event 를 가진 유효한 DTO 생성.
     * version=2, updatedAt=now.
     */
    private fun buildValidDTO(
        eventId: String = "test-event",
        eventScore: Int = 50,
    ): ReturnDataDTO {
        val zeroReturns = HistoricalReturnsDTO(0.0, 0.0, 0.0, 0.0)
        val points = (0..100).map { score ->
            ReturnDataPointDTO(
                score = score,
                returns = zeroReturns,
                worstCase = zeroReturns,
                bestCase = zeroReturns,
                sampleCount = 1,
            )
        }
        val event = ReturnEventEntryDTO(
            id = eventId,
            date = "2025-06-13",
            score = eventScore,
            descriptionKey = "insight.event.iranWar",
            returnAfter = HistoricalReturnsDTO(5.2, 12.3, 0.0, 0.0),
        )
        return ReturnDataDTO(
            version = 2,
            updatedAt = 1_713_225_600.0,
            dataPoints = points,
            historicalEvents = listOf(event),
        )
    }
}
