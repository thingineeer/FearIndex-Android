package th1ngjin.fearindex.domain.entity

/**
 * 자산 가격 RSI(14) 보조 지표 — iOS `FearRSI.swift` 대칭.
 */
data class FearRSI(
    val value: Double,
    val signal: RSISignal,
) {
    enum class RSISignal {
        OVERSOLD,
        NEUTRAL,
        OVERBOUGHT,
    }
}
