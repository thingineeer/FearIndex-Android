package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearRSI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Wilder's smoothing RSI(14) — iOS `RSICalculator.swift` 1:1 포팅 (계산 SSOT).
 *
 * - 최초 period 개 변화량은 단순평균, 이후 지수평활:
 *   avgGain = (prevAvgGain * (period-1) + gain) / period
 * - avgLoss 0(전 구간 상승)이면 RSI=100(overbought)으로 0 나눗셈 방지.
 * - 입력은 시간순(오래된→최신) 종가 배열이어야 한다.
 */
class RSICalculator(private val period: Int = 14) {

    /** 공포지수 히스토리에서 RSI 계산 — timestamp 오름차순 정렬 후 점수 시계열 사용. */
    fun calculateFromHistory(history: List<FearIndex>): FearRSI? =
        calculate(closes = history.sortedBy { it.timestamp }.map { it.score })

    /** 가격/값 종가 배열(시간순 오름차순)에서 RSI 계산. */
    fun calculate(closes: List<Double>): FearRSI? {
        if (closes.size <= period) return null
        val changes = closes.zipWithNext { prev, next -> next - prev }
        val (avgGain, avgLoss) = averageGainLoss(changes)
        if (avgLoss <= 0) return FearRSI(value = 100.0, signal = FearRSI.RSISignal.OVERBOUGHT)
        val rs = avgGain / avgLoss
        val rsi = 100 - (100 / (1 + rs))
        return FearRSI(value = rsi, signal = classify(rsi))
    }

    private fun averageGainLoss(changes: List<Double>): Pair<Double, Double> {
        val window = changes.take(period)
        var avgGain = window.sumOf { max(it, 0.0) } / period
        var avgLoss = window.sumOf { abs(min(it, 0.0)) } / period
        val mult = (period - 1).toDouble()
        for (i in period until changes.size) {
            val gain = max(changes[i], 0.0)
            val loss = abs(min(changes[i], 0.0))
            avgGain = (avgGain * mult + gain) / period
            avgLoss = (avgLoss * mult + loss) / period
        }
        return avgGain to avgLoss
    }

    private fun classify(rsi: Double): FearRSI.RSISignal = when {
        rsi < 30 -> FearRSI.RSISignal.OVERSOLD
        rsi > 70 -> FearRSI.RSISignal.OVERBOUGHT
        else -> FearRSI.RSISignal.NEUTRAL
    }
}
