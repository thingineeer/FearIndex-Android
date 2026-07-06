package th1ngjin.fearindex.data.mapper

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.data.dto.KospiHistoryDTO
import th1ngjin.fearindex.data.dto.KospiShortResponse
import th1ngjin.fearindex.data.dto.YahooCloseChartResponse

/**
 * iOS `AssetPriceCloseParserTests` + `AssetShortRatioParserTests` 대응.
 * 파서는 순수 함수 — 네트워크 없이 페이로드 → 시계열 변환만 검증.
 */
class AssetIndicatorParsersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // MARK: - Yahoo (SPX ^GSPC closes)

    @Test
    fun `Yahoo chart 응답에서 종가 추출 - 휴장일 null 제거, 순서 보존`() {
        val payload = """
            {"chart":{"result":[{"meta":{"symbol":"^GSPC"},
              "indicators":{"quote":[{"close":[6100.5,null,6120.25,6090.0,null]}]}}]}}
        """.trimIndent()
        val response = json.decodeFromString<YahooCloseChartResponse>(payload)
        assertEquals(listOf(6100.5, 6120.25, 6090.0), YahooCloseParser.closes(response))
    }

    @Test
    fun `Yahoo chart result 없으면 빈 배열`() {
        val response = json.decodeFromString<YahooCloseChartResponse>("""{"chart":{"result":null}}""")
        assertEquals(emptyList<Double>(), YahooCloseParser.closes(response))
    }

    // MARK: - FINRA (SPX = SPY proxy, 파이프 구분 텍스트)

    private val finraSample = """
        Date|Symbol|ShortVolume|ShortExemptVolume|TotalVolume|Market
        20260702|AAPL|1000|10|4000|B,Q,N
        20260702|SPY|30000|100|100000|B,Q,N
        20260702|TSLA|500|5|1000|B,Q,N
    """.trimIndent()

    @Test
    fun `FINRA 텍스트에서 SPY 공매도 비중 추출 - Short over Total 곱하기 100`() {
        assertEquals(30.0, FinraShortVolumeParser.shortRatioPercent(finraSample, "SPY")!!, 0.0001)
    }

    @Test
    fun `FINRA 심볼 없으면 null`() {
        assertNull(FinraShortVolumeParser.shortRatioPercent(finraSample, "QQQ"))
    }

    @Test
    fun `FINRA TotalVolume 0이면 null(0 나눗셈 방지)`() {
        val text = "Date|Symbol|ShortVolume|ShortExemptVolume|TotalVolume|Market\n20260702|SPY|100|0|0|Q"
        assertNull(FinraShortVolumeParser.shortRatioPercent(text, "SPY"))
    }

    @Test
    fun `FINRA 필드 부족 행은 스킵`() {
        val text = "broken|line\n20260702|SPY|20000|50|80000|Q"
        assertEquals(25.0, FinraShortVolumeParser.shortRatioPercent(text, "SPY")!!, 0.0001)
    }

    // MARK: - KOSPI 서버 공매도 (/api/kospi/short)

    @Test
    fun `KOSPI short 응답 디코딩`() {
        val payload = """{"shortRatios":[4.2,4.5,3.9],"dates":["2026-06-30","2026-07-01","2026-07-02"],"updatedAt":"2026-07-03T00:00:00Z"}"""
        val response = json.decodeFromString<KospiShortResponse>(payload)
        assertEquals(listOf(4.2, 4.5, 3.9), response.shortRatios)
    }

    // MARK: - KOSPI 종가 (스냅샷 chartHistoryForDisplay의 kospiClose)

    @Test
    fun `KOSPI history에서 kospiClose 추출 - 날짜 오름차순, null 제거`() {
        val history = listOf(
            KospiHistoryDTO(date = "2026-07-02", score = 40.0, rating = "fear", confidence = "high", kospiClose = 8730.5),
            KospiHistoryDTO(date = "2026-06-30", score = 38.0, rating = "fear", confidence = "high", kospiClose = 8650.0),
            KospiHistoryDTO(date = "2026-07-01", score = 39.0, rating = "fear", confidence = "high", kospiClose = null),
        )
        assertEquals(listOf(8650.0, 8730.5), KospiCloseParser.closes(history))
    }

    @Test
    fun `KOSPI 종가가 존재하는 마지막 날짜가 asOf`() {
        val history = listOf(
            KospiHistoryDTO(date = "2026-07-02", score = 40.0, rating = "fear", confidence = "high", kospiClose = 8730.5),
            KospiHistoryDTO(date = "2026-07-03", score = 41.0, rating = "fear", confidence = "high", kospiClose = null),
            KospiHistoryDTO(date = "2026-06-30", score = 38.0, rating = "fear", confidence = "high", kospiClose = 8650.0),
        )
        assertEquals("2026-07-02", KospiCloseParser.lastCloseDate(history))
        assertNull(KospiCloseParser.lastCloseDate(emptyList()))
    }
}
