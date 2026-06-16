package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 환율 시세. iOS `ExchangeRateQuote` 와 1:1 대응 (currency-api).
 */
data class ExchangeRateQuote(
    val baseCode: String,      // "USD"
    val targetCode: String,    // "KRW"
    val rate: Double,
    val change: Double? = null,
    val changePercent: Double? = null,
    val lastUpdated: Instant = Instant.EPOCH,
    val provider: String = "currency-api",
) {
    val isPositive: Boolean get() = (change ?: 0.0) >= 0
}
