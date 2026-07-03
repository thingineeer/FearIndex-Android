package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.ShortPressure

/**
 * 공매도 비중(%) 시계열 → 추세 기반 신호 — iOS `ShortPressureCalculator.swift` 1:1 포팅.
 *
 * 자산마다 정상 공매도 비중 스케일이 크게 달라(SPX/BTC ~30~40%, KOSPI 한 자릿수%)
 * 절대 %포인트가 아닌 "최신값 제외 최근 5개 평균 대비 상대 변화율"로 판정 → scale-invariant.
 */
class ShortPressureCalculator(
    /** 추세 판정 임계값(최근평균 대비 상대 변화율). ±15% 벗어나면 rising/falling. */
    private val trendThreshold: Double = 0.15,
) {

    /** dailyRatios: 시간순(오래된→최신) 공매도 비중(%). 최소 3개 필요, 부족 시 null(카드 숨김). */
    fun calculate(dailyRatios: List<Double>): ShortPressure? {
        if (dailyRatios.size < 3) return null
        val latest = dailyRatios.last()
        val recent = dailyRatios.dropLast(1).takeLast(5)
        if (recent.isEmpty()) return null
        val baseline = recent.sum() / recent.size
        // baseline 0(전 구간 무공매도)이면 상대비 불가 → 중립 처리.
        if (baseline <= 0) {
            return ShortPressure(
                ratioPercent = latest,
                trend = ShortPressure.Trend.FLAT,
                signal = ShortPressure.Signal.NEUTRAL,
            )
        }
        val relativeDelta = (latest - baseline) / baseline

        val trend = when {
            relativeDelta > trendThreshold -> ShortPressure.Trend.RISING
            relativeDelta < -trendThreshold -> ShortPressure.Trend.FALLING
            else -> ShortPressure.Trend.FLAT
        }
        val signal = when (trend) {
            ShortPressure.Trend.RISING -> ShortPressure.Signal.HEAVY_SHORTING
            ShortPressure.Trend.FALLING -> ShortPressure.Signal.SHORT_COVERING
            ShortPressure.Trend.FLAT -> ShortPressure.Signal.NEUTRAL
        }
        return ShortPressure(ratioPercent = latest, trend = trend, signal = signal)
    }
}
