package th1ngjin.fearindex.data.dto

import kotlinx.serialization.Serializable
import th1ngjin.fearindex.domain.entity.IndicatorSourceMetadata

/**
 * RSI/공매도 보조 지표용 DTO — iOS `OfficialIndicatorsDTO`/`AssetShortRatioDataSource` 응답 모델 대칭.
 */

/** Yahoo chart v8 — 종가 시계열용 (기존 YahooChartResponse는 meta 전용이라 별도 선언). */
@Serializable
data class YahooCloseChartResponse(val chart: Chart) {
    @Serializable
    data class Chart(val result: List<Result>? = null)

    @Serializable
    data class Result(val indicators: Indicators = Indicators())

    @Serializable
    data class Indicators(val quote: List<Quote> = emptyList())

    @Serializable
    data class Quote(val close: List<Double?> = emptyList())
}

/**
 * 서버 `cryptoOfficialIndicatorsV1` — Binance 공식 소스 기반 BTC 지표 시계열.
 * rsi.closes(일봉 종가 180개, 클라에서 Wilder 14 RSI 계산) + short.ratios(shortAccount %, 14일).
 */
@Serializable
data class OfficialIndicatorsResponse(
    val version: Int = 1,
    val generatedAt: String? = null,
    val rsi: OfficialIndicatorSeriesDTO = OfficialIndicatorSeriesDTO(),
    val short: OfficialIndicatorSeriesDTO = OfficialIndicatorSeriesDTO(),
)

@Serializable
data class OfficialIndicatorSeriesDTO(
    val available: Boolean = false,
    val values: List<Double>? = null,
    val closes: List<Double>? = null,
    val ratios: List<Double>? = null,
    val dates: List<String> = emptyList(),
    val source: String? = null,
    val basis: String? = null,
    val asOf: String? = null,
    val official: Boolean = false,
    val methodology: String? = null,
) {
    /** iOS: `decoded.rsi.closes ?? decoded.rsi.values`. */
    val closesOrValues: List<Double> get() = closes ?: values ?: emptyList()

    /** iOS: `decoded.short.ratios ?? decoded.short.values`. */
    val ratiosOrValues: List<Double> get() = ratios ?: values ?: emptyList()

    fun toSourceMetadata(): IndicatorSourceMetadata = IndicatorSourceMetadata(
        sourceName = source.orEmpty(),
        basisLabel = basis.orEmpty(),
        asOf = asOf,
        isOfficial = official,
        methodology = methodology.orEmpty(),
    )
}

/**
 * 서버 `/api/kospi/short` — KIS 시총상위 합산 공매도 비중(%) 시계열.
 * KRX 공식 소스 확정 전까지 서버가 `available=false` + 빈 배열을 반환 (구버전 응답엔 필드 없음 → true).
 */
@Serializable
data class KospiShortResponse(
    val available: Boolean = true,
    val shortRatios: List<Double> = emptyList(),
    val dates: List<String> = emptyList(),
    val source: String? = null,
    val basis: String? = null,
    val official: Boolean = false,
    val methodology: String? = null,
) {
    fun toSourceMetadata(): IndicatorSourceMetadata = IndicatorSourceMetadata(
        sourceName = source.orEmpty(),
        basisLabel = basis.orEmpty(),
        asOf = dates.lastOrNull(),
        isOfficial = official,
        methodology = methodology.orEmpty(),
    )
}
