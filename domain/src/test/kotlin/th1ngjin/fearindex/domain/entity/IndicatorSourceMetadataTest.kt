package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 지표 출처 메타데이터 라벨 포맷 — iOS sourceText/infoRow body 대응.
 * 카드: "source · basis · asOf", 시트: "source · asOf · methodology" (빈 값 제외).
 */
class IndicatorSourceMetadataTest {

    @Test
    fun `cardLabel - source basis asOf를 가운뎃점으로 연결`() {
        val metadata = IndicatorSourceMetadata(
            sourceName = "Binance USD-M Futures",
            basisLabel = "BTC",
            asOf = "2026-07-05",
            isOfficial = true,
            methodology = "14-day RSI from Binance BTCUSDT daily futures closes.",
        )

        assertEquals("Binance USD-M Futures · BTC · 2026-07-05", metadata.cardLabel)
    }

    @Test
    fun `cardLabel - asOf 없으면 제외`() {
        val metadata = IndicatorSourceMetadata(
            sourceName = "Yahoo Finance ^GSPC",
            basisLabel = "S&P 500",
            asOf = null,
            isOfficial = false,
            methodology = "14-day RSI from Yahoo Finance ^GSPC daily closes.",
        )

        assertEquals("Yahoo Finance ^GSPC · S&P 500", metadata.cardLabel)
    }

    @Test
    fun `cardLabel - 빈 문자열도 제외`() {
        val metadata = IndicatorSourceMetadata(
            sourceName = "",
            basisLabel = "KOSPI",
            asOf = "",
            isOfficial = true,
            methodology = "",
        )

        assertEquals("KOSPI", metadata.cardLabel)
    }

    @Test
    fun `sheetBody - source asOf methodology를 가운뎃점으로 연결`() {
        val metadata = IndicatorSourceMetadata(
            sourceName = "FINRA Daily Short Sale Volume",
            basisLabel = "SPY ETF",
            asOf = "2026-07-02",
            isOfficial = true,
            methodology = "SPY ShortVolume / TotalVolume from FINRA CNMS daily files.",
        )

        assertEquals(
            "FINRA Daily Short Sale Volume · 2026-07-02 · " +
                "SPY ShortVolume / TotalVolume from FINRA CNMS daily files.",
            metadata.sheetBody,
        )
    }
}
