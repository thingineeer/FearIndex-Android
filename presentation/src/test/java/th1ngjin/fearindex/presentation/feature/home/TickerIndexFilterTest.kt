package th1ngjin.fearindex.presentation.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketIndex

class TickerIndexFilterTest {

    @Test
    fun `KOSPI 선택 시 한국 지수만 ticker에 노출한다`() {
        val indices = sampleIndices()

        val result = tickerIndicesFor(FearIndexType.KOSPI, indices)

        assertEquals(listOf("KOSPI", "KOSDAQ"), result.map { it.name })
    }

    @Test
    fun `시장 선택 시 한국 지수는 제외하고 글로벌 지수를 노출한다`() {
        val indices = sampleIndices()

        val result = tickerIndicesFor(FearIndexType.MARKET, indices)

        assertEquals(listOf("Nasdaq", "S&P 500", "Dow Jones"), result.map { it.name })
    }

    private fun sampleIndices(): List<MarketIndex> = listOf(
        MarketIndex("^KS11", "KOSPI", 2864.12, -1.42, isPositive = false),
        MarketIndex("^KQ11", "KOSDAQ", 802.45, -0.86, isPositive = false),
        MarketIndex("^IXIC", "Nasdaq", 17896.21, 0.74, isPositive = true),
        MarketIndex("^GSPC", "S&P 500", 5421.03, 0.31, isPositive = true),
        MarketIndex("^DJI", "Dow Jones", 38974.12, -0.18, isPositive = false),
    )
}
