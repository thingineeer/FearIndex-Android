package th1ngjin.fearindex.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * 시장 상세 시세 표시 포맷 (순수 함수). en_US 강제(천단위 콤마, 마침표 소수점).
 * iOS MarketIndex/CryptoPrice/ExchangeRateQuote 의 priceText/changePercentText 와 1:1.
 */
object MarketQuoteFormat {

    private val symbols = DecimalFormatSymbols(Locale.US)

    private val indexPriceFormat = DecimalFormat("#,##0.00", symbols)
    private val cryptoHighFormat = DecimalFormat("#,##0.00", symbols)        // price >= 1: 소수 2자리
    private val cryptoLowFormat = DecimalFormat("#,##0.00##", symbols)       // price < 1: 소수 min2 max4
    private val exchangeFormat = DecimalFormat("#,##0.0", symbols)
    private val percentFormat = DecimalFormat("0.00", symbols)

    /** "▲ 1.25%" / "▼ 0.50%" / "0.00%" / "--"(null). iOS changePercentText. */
    fun changePercentText(changePercent: Double?): String {
        if (changePercent == null) return "--"
        if (changePercent == 0.0) return "0.00%"
        val arrow = if (changePercent > 0) "▲" else "▼"
        return "$arrow ${percentFormat.format(abs(changePercent))}%"
    }

    /** 지수 가격 "7,554.29". */
    fun indexPriceText(price: Double): String = indexPriceFormat.format(price)

    /** 암호화폐 가격 "$67,000.00" / "$0.5234" (가격대별 소수자리). */
    fun cryptoPriceText(price: Double): String {
        val body = if (price >= 1.0) cryptoHighFormat.format(price) else cryptoLowFormat.format(price)
        return "$$body"
    }

    /** 환율 가격 "1,512.6" (소수 1자리). */
    fun exchangePriceText(rate: Double): String = exchangeFormat.format(rate)
}
