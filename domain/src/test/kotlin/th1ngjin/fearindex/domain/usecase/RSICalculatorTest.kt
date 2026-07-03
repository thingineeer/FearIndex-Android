package th1ngjin.fearindex.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearRSI
import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random

/**
 * iOS `RSICalculatorTests.swift` 와 1:1 대응하는 테스트.
 * Wilder's smoothing RSI(14) — 시간순(오래된→최신) 종가/점수 시계열.
 */
class RSICalculatorTest {

    private val calculator = RSICalculator(period = 14)

    /** iOS makeHistory 대응 — scores[0]이 가장 최신(과거로 갈수록 idx 증가). */
    private fun makeHistory(scores: List<Double>): List<FearIndex> {
        val now = Instant.ofEpochSecond(1_700_000_000)
        return scores.mapIndexed { idx, score ->
            FearIndex(
                score = score,
                rating = FearIndex.Rating.from(score),
                timestamp = now.minusSeconds(idx.toLong() * 86_400),
                previousClose = score,
                previous1Week = score,
                previous1Month = score,
            )
        }
    }

    // MARK: - history 입력

    @Test
    fun `데이터 부족이면 null`() {
        val history = makeHistory(listOf(50.0, 48.0, 46.0))
        assertNull(calculator.calculateFromHistory(history))
    }

    @Test
    fun `RSI 값은 0~100 범위`() {
        val random = Random(42)
        val scores = List(30) { random.nextDouble(20.0, 80.0) }
        val result = calculator.calculateFromHistory(makeHistory(scores))
        assertNotNull(result)
        assertTrue(result!!.value >= 0)
        assertTrue(result.value <= 100)
    }

    @Test
    fun `연속 하락이면 RSI 30 미만 시 oversold`() {
        val scores = mutableListOf(80.0)
        for (i in 1 until 20) scores.add(80.0 - i * 3.5)
        val result = calculator.calculateFromHistory(makeHistory(scores.reversed()))
        assertNotNull(result)
        if (result!!.value < 30) {
            assertEquals(FearRSI.RSISignal.OVERSOLD, result.signal)
        }
    }

    @Test
    fun `연속 상승이면 RSI 70 초과 시 overbought`() {
        val scores = mutableListOf(20.0)
        for (i in 1 until 20) scores.add(20.0 + i * 3.5)
        val result = calculator.calculateFromHistory(makeHistory(scores.reversed()))
        assertNotNull(result)
        if (result!!.value > 70) {
            assertEquals(FearRSI.RSISignal.OVERBOUGHT, result.signal)
        }
    }

    @Test
    fun `전부 상승(손실 0)이면 RSI 100 overbought`() {
        val scores = List(20) { it * 2.0 + 10 }
        val result = calculator.calculateFromHistory(makeHistory(scores.reversed()))
        assertNotNull(result)
        assertEquals(100.0, result!!.value, 0.0)
        assertEquals(FearRSI.RSISignal.OVERBOUGHT, result.signal)
    }

    @Test
    fun `혼합 변동이면 neutral`() {
        val scores = List(20) { i -> 50.0 + if (i % 2 == 0) 5.0 else -5.0 }
        val result = calculator.calculateFromHistory(makeHistory(scores.reversed()))
        assertNotNull(result)
        assertEquals(FearRSI.RSISignal.NEUTRAL, result!!.signal)
    }

    // MARK: - closes(가격 종가 배열) 직접 입력

    @Test
    fun `closes 입력 - period 이하면 null`() {
        // 변화 개수가 period 미만 (14개 종가 → 13개 변화) → null
        val closes = List(14) { it.toDouble() }
        assertNull(calculator.calculate(closes = closes))
    }

    @Test
    fun `closes 입력 - 단조 상승이면 RSI 100 overbought`() {
        val closes = List(20) { it * 2.0 + 10 }
        val result = calculator.calculate(closes = closes)
        assertNotNull(result)
        assertEquals(100.0, result!!.value, 0.0)
        assertEquals(FearRSI.RSISignal.OVERBOUGHT, result.signal)
    }

    @Test
    fun `closes 입력 - 동일 가격(분모 0)도 안전 처리`() {
        val closes = List(20) { 2700.0 }
        val result = calculator.calculate(closes = closes)
        assertNotNull(result)
        assertEquals(100.0, result!!.value, 0.0)
    }

    @Test
    fun `closes 입력 - 값이 0~100 범위`() {
        val random = Random(7)
        val closes = List(30) { random.nextDouble(2500.0, 2900.0) }
        val result = calculator.calculate(closes = closes)
        assertNotNull(result)
        assertTrue(result!!.value >= 0)
        assertTrue(result.value <= 100)
    }

    @Test
    fun `closes 입력은 동일 시계열의 history 경로 결과와 일치`() {
        val series = listOf(
            2700.0, 2710.0, 2695.0, 2720.0, 2730.0, 2715.0, 2740.0, 2750.0,
            2735.0, 2760.0, 2745.0, 2770.0, 2780.0, 2765.0, 2790.0, 2800.0,
        )
        // history 경로는 timestamp 오름차순 정렬 후 점수 시계열을 본다.
        val fromHistory = calculator.calculateFromHistory(makeHistory(series.reversed()))
        val fromCloses = calculator.calculate(closes = series)
        assertNotNull(fromHistory)
        assertNotNull(fromCloses)
        assertTrue(abs(fromHistory!!.value - fromCloses!!.value) < 0.0001)
        assertEquals(fromHistory.signal, fromCloses.signal)
    }
}
