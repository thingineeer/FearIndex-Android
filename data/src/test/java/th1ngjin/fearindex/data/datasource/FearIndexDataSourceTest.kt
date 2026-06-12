package th1ngjin.fearindex.data.datasource

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import th1ngjin.fearindex.data.dto.CNNFearGreedResponse
import th1ngjin.fearindex.data.dto.FearAndGreedDTO
import th1ngjin.fearindex.data.dto.FearAndGreedHistoricalDTO
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FearIndexDataSourceTest {

    private val api = mockk<CNNFearGreedApi>()
    private val dataSource = FearIndexDataSource(api)

    @Test
    fun `fetchCurrent - 365일 이하는 365일 캐시를 재사용한다`() = runTest {
        val expected = createResponse()
        val startDate365 = LocalDate.now()
            .minusDays(365)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        coEvery { api.getFearAndGreed(startDate365) } returns expected

        val result90 = dataSource.fetchCurrent(days = 90)
        val result180 = dataSource.fetchCurrent(days = 180)
        val result365 = dataSource.fetchCurrent(days = 365)

        assertSame(expected, result90)
        assertSame(expected, result180)
        assertSame(expected, result365)
        coVerify(exactly = 1) { api.getFearAndGreed(startDate365) }
    }

    @Test
    fun `fetchCurrent - 365일 초과는 요청 기간별 startDate를 사용한다`() = runTest {
        val expected = createResponse()
        val startDate1825 = LocalDate.now()
            .minusDays(1825)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        coEvery { api.getFearAndGreed(startDate1825) } returns expected

        val result = dataSource.fetchCurrent(days = 1825)

        assertSame(expected, result)
        coVerify(exactly = 1) { api.getFearAndGreed(startDate1825) }
    }

    private fun createResponse() = CNNFearGreedResponse(
        fearAndGreed = FearAndGreedDTO(
            score = 30.0,
            rating = "Fear",
            timestamp = "1781222382000",
            previousClose = 31.0,
            previous1Week = 32.0,
            previous1Month = 33.0,
            previous1Year = 34.0,
        ),
        fearAndGreedHistorical = FearAndGreedHistoricalDTO(),
    )
}
