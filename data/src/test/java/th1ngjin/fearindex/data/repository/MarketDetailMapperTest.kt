package th1ngjin.fearindex.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.data.dto.NaverIndexResponse
import th1ngjin.fearindex.data.dto.YahooChartMeta
import th1ngjin.fearindex.domain.entity.MarketIndexType

/**
 * 시장 상세 데이터 변환 로직 검증 (iOS YahooFinanceStrategy/NaverFinanceStrategy/currency-api 와 1:1).
 */
class MarketDetailMapperTest {

    // ── Yahoo ──

    @Test
    fun `yahoo - chartPreviousClose 우선으로 change 계산`() {
        val meta = YahooChartMeta(regularMarketPrice = 110.0, chartPreviousClose = 100.0, previousClose = 90.0)
        val idx = MarketDetailMapper.yahooToIndex(MarketIndexType.SP500, meta)!!
        assertEquals(110.0, idx.price, 0.0)
        assertEquals(10.0, idx.change, 0.0)          // 110 - 100 (chartPreviousClose 우선)
        assertEquals(10.0, idx.changePercent, 0.001) // 10/100*100
    }

    @Test
    fun `yahoo - chartPreviousClose 없으면 previousClose fallback`() {
        val meta = YahooChartMeta(regularMarketPrice = 110.0, chartPreviousClose = null, previousClose = 88.0)
        val idx = MarketDetailMapper.yahooToIndex(MarketIndexType.VIX, meta)!!
        assertEquals(22.0, idx.change, 0.0) // 110 - 88
    }

    @Test
    fun `yahoo - 둘 다 없으면 price 자신 (무변동)`() {
        val meta = YahooChartMeta(regularMarketPrice = 110.0, chartPreviousClose = null, previousClose = null)
        val idx = MarketDetailMapper.yahooToIndex(MarketIndexType.NASDAQ, meta)!!
        assertEquals(0.0, idx.change, 0.0)
        assertEquals(0.0, idx.changePercent, 0.0)
    }

    @Test
    fun `yahoo - regularMarketPrice 없으면 null`() {
        val meta = YahooChartMeta(regularMarketPrice = null)
        assertNull(MarketDetailMapper.yahooToIndex(MarketIndexType.NASDAQ, meta))
    }

    @Test
    fun `yahoo - previousClose 0 이면 changePercent 0 (0 나눗셈 방어)`() {
        val meta = YahooChartMeta(regularMarketPrice = 5.0, chartPreviousClose = 0.0)
        val idx = MarketDetailMapper.yahooToIndex(MarketIndexType.VIX, meta)!!
        assertEquals(0.0, idx.changePercent, 0.0)
    }

    // ── Naver ──

    @Test
    fun `naver - 콤마 제거 후 파싱`() {
        val dto = NaverIndexResponse(
            closePrice = "2,864.12",
            compareToPreviousClosePrice = "-41.20",
            fluctuationsRatio = "-1.42",
        )
        val idx = MarketDetailMapper.naverToIndex(MarketIndexType.KOSPI, dto)
        assertEquals(2864.12, idx.price, 0.001)
        assertEquals(-41.2, idx.change, 0.001)
        assertEquals(-1.42, idx.changePercent, 0.001)
        assertEquals("^KS11", idx.symbol) // symbol 은 Yahoo 심볼 유지
    }

    @Test
    fun `naver - 파싱 실패 시 0`() {
        val dto = NaverIndexResponse(closePrice = "abc", compareToPreviousClosePrice = "", fluctuationsRatio = "x")
        val idx = MarketDetailMapper.naverToIndex(MarketIndexType.KOSDAQ, dto)
        assertEquals(0.0, idx.price, 0.0)
        assertEquals(0.0, idx.change, 0.0)
    }

    // ── 환율 이전일 계산 ──

    @Test
    fun `previousDateString - UTC 하루 전`() {
        assertEquals("2026-06-14", MarketDetailMapper.previousDateString("2026-06-15"))
        assertEquals("2026-02-28", MarketDetailMapper.previousDateString("2026-03-01"))
        assertEquals("2025-12-31", MarketDetailMapper.previousDateString("2026-01-01"))
    }

    @Test
    fun `previousDateString - 잘못된 형식이면 null`() {
        assertNull(MarketDetailMapper.previousDateString("bad-date"))
    }

    // ── 환율 quote ──

    @Test
    fun `exchange - change 와 changePercent 계산`() {
        val quote = MarketDetailMapper.exchangeQuote(rate = 1380.0, previousRate = 1360.0, date = "2026-06-15")!!
        assertEquals(1380.0, quote.rate, 0.0)
        assertEquals(20.0, quote.change!!, 0.001)
        assertEquals(20.0 / 1360.0 * 100, quote.changePercent!!, 0.001)
        assertEquals("USD", quote.baseCode)
        assertEquals("KRW", quote.targetCode)
    }

    @Test
    fun `exchange - previousRate 없으면 change null`() {
        val quote = MarketDetailMapper.exchangeQuote(rate = 1380.0, previousRate = null, date = "2026-06-15")!!
        assertNull(quote.change)
        assertNull(quote.changePercent)
    }
}
