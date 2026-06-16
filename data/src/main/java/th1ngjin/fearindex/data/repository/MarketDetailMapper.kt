package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.dto.NaverIndexResponse
import th1ngjin.fearindex.data.dto.YahooChartMeta
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.entity.MarketIndexType
import java.time.Instant
import java.time.LocalDate

/**
 * 시장 상세 API 응답 → 도메인 엔티티 변환 (순수 함수).
 * iOS YahooFinanceStrategy / NaverFinanceStrategy / ExchangeRateDataSource 와 1:1.
 */
object MarketDetailMapper {

    /** Yahoo chart meta → MarketIndex. price 없으면 null. */
    fun yahooToIndex(type: MarketIndexType, meta: YahooChartMeta, now: Instant = Instant.EPOCH): MarketIndex? {
        val price = meta.regularMarketPrice ?: return null
        // iOS: chartPreviousClose ?? previousClose ?? price
        val previousClose = meta.chartPreviousClose ?: meta.previousClose ?: price
        val change = price - previousClose
        val changePercent = if (previousClose > 0.0) (change / previousClose) * 100.0 else 0.0
        return MarketIndex(
            symbol = type.symbol,
            name = type.officialSymbol,
            price = price,
            change = change,
            changePercent = changePercent,
            timestamp = now,
        )
    }

    /** Naver 응답 → MarketIndex. 콤마 제거 후 파싱, 실패 시 0. */
    fun naverToIndex(type: MarketIndexType, dto: NaverIndexResponse, now: Instant = Instant.EPOCH): MarketIndex {
        val price = parseNumber(dto.closePrice)
        val change = parseNumber(dto.compareToPreviousClosePrice)
        val changePercent = parseNumber(dto.fluctuationsRatio)
        return MarketIndex(
            symbol = type.symbol, // Yahoo 심볼(^KS11) 유지 — type 매핑용
            name = type.officialSymbol,
            price = price,
            change = change,
            changePercent = changePercent,
            timestamp = now,
        )
    }

    /** currency-api 응답 → ExchangeRateQuote (USD/KRW). rate 없으면 null. */
    fun exchangeQuote(rate: Double, previousRate: Double?, date: String, now: Instant = Instant.EPOCH): ExchangeRateQuote? {
        val lastUpdated = runCatching {
            LocalDate.parse(date).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        }.getOrDefault(now)
        val change = previousRate?.let { rate - it }
        val changePercent = previousRate?.takeIf { it > 0.0 }?.let { ((rate - it) / it) * 100.0 }
        return ExchangeRateQuote(
            baseCode = "USD",
            targetCode = "KRW",
            rate = rate,
            change = change,
            changePercent = changePercent,
            lastUpdated = lastUpdated,
            provider = "currency-api.pages.dev",
        )
    }

    /** "yyyy-MM-dd" → 하루 전 "yyyy-MM-dd" (UTC). 형식 오류면 null. */
    fun previousDateString(date: String): String? =
        runCatching { LocalDate.parse(date).minusDays(1).toString() }.getOrNull()

    /** Naver 의 "2,864.12" 같은 콤마 포함 문자열 → Double. 실패 0.0. */
    private fun parseNumber(value: String): Double =
        value.replace(",", "").trim().toDoubleOrNull() ?: 0.0
}
