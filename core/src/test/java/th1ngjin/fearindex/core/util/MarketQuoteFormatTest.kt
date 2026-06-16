package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 시장 상세 시세 표시 포맷 검증 (iOS MarketIndex/CryptoPrice/ExchangeRateQuote 의 *Text computed 와 1:1).
 */
class MarketQuoteFormatTest {

    // ── changePercentText (▲/▼ + abs %.2f%%) ──

    @Test
    fun `percent - 양수는 위 화살표 빨강의미`() {
        assertEquals("▲ 1.25%", MarketQuoteFormat.changePercentText(1.25))
    }

    @Test
    fun `percent - 음수는 아래 화살표`() {
        assertEquals("▼ 0.50%", MarketQuoteFormat.changePercentText(-0.5))
    }

    @Test
    fun `percent - 0 은 화살표 없이 0_00 퍼센트`() {
        assertEquals("0.00%", MarketQuoteFormat.changePercentText(0.0))
    }

    @Test
    fun `percent - null 은 대시`() {
        assertEquals("--", MarketQuoteFormat.changePercentText(null))
    }

    @Test
    fun `percent - 소수 둘째자리 반올림`() {
        assertEquals("▲ 2.13%", MarketQuoteFormat.changePercentText(2.126))
    }

    // ── 지수 priceText (천단위 콤마, 소수 2자리) ──

    @Test
    fun `indexPrice - 천단위 콤마 + 소수 2자리`() {
        assertEquals("7,554.29", MarketQuoteFormat.indexPriceText(7554.29))
        assertEquals("16.20", MarketQuoteFormat.indexPriceText(16.2))
        assertEquals("26,683.94", MarketQuoteFormat.indexPriceText(26683.94))
    }

    // ── crypto priceText (USD, 가격대별 소수) ──

    @Test
    fun `cryptoPrice - 1 이상은 소수 2자리 + 달러`() {
        assertEquals("$67,000.00", MarketQuoteFormat.cryptoPriceText(67000.0))
        assertEquals("$1,774.06", MarketQuoteFormat.cryptoPriceText(1774.06))
    }

    @Test
    fun `cryptoPrice - 1 미만은 소수 최대 4자리`() {
        assertEquals("$0.52", MarketQuoteFormat.cryptoPriceText(0.52))
        assertEquals("$0.5234", MarketQuoteFormat.cryptoPriceText(0.5234))
    }

    // ── 환율 priceText (소수 1자리) ──

    @Test
    fun `exchangePrice - 소수 1자리`() {
        assertEquals("1,512.6", MarketQuoteFormat.exchangePriceText(1512.63))
    }
}
