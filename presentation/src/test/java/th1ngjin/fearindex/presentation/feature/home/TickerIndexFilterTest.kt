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
        MarketIndex("^KS11", "KOSPI", price = 2864.12, change = -1.42, changePercent = -1.42),
        MarketIndex("^KQ11", "KOSDAQ", price = 802.45, change = -0.86, changePercent = -0.86),
        MarketIndex("^IXIC", "Nasdaq", price = 17896.21, change = 0.74, changePercent = 0.74),
        MarketIndex("^GSPC", "S&P 500", price = 5421.03, change = 0.31, changePercent = 0.31),
        MarketIndex("^DJI", "Dow Jones", price = 38974.12, change = -0.18, changePercent = -0.18),
    )
}
