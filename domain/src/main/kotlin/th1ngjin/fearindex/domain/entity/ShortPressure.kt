package th1ngjin.fearindex.domain.entity

/**
 * 공매도/숏커버링 보조 지표 — iOS `ShortPressure.swift` 대칭.
 * 최근 공매도 비중(%) + 추세 + 신호.
 */
data class ShortPressure(
    /** 최신 공매도 비중(%). 자산별 의미: SPX/KOSPI=숏거래량/총거래량, BTC=순숏 계정 비율. */
    val ratioPercent: Double,
    val trend: Trend,
    val signal: Signal,
) {
    enum class Trend {
        RISING,
        FALLING,
        FLAT,
    }

    enum class Signal {
        /** 공매도 비중 상승 = 하락 베팅 증가. */
        HEAVY_SHORTING,
        NEUTRAL,

        /** 공매도 비중 하락 = 숏 청산(숏커버링) = 매수 압력. */
        SHORT_COVERING,
    }
}
