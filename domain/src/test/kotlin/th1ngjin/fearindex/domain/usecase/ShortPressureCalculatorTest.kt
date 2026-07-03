package th1ngjin.fearindex.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.domain.entity.ShortPressure

/**
 * iOS `ShortPressureTests.swift` 와 1:1 대응하는 테스트.
 * 일별 공매도 비중(%) 시계열 → 최근 평균 대비 상대 변화율(±15%)로 추세 판정 (scale-invariant).
 */
class ShortPressureCalculatorTest {

    private val calc = ShortPressureCalculator() // 기본 상대 임계값 0.15(±15%)

    @Test
    fun `공매도 비중 상승(+22퍼센트)이면 heavyShorting + ratio는 최신값`() {
        val ratios = listOf(30.0, 31.0, 32.0, 38.0) // 38 vs 최근평균 31 = +22% > 15%
        val sp = calc.calculate(ratios)
        assertNotNull(sp)
        assertEquals(ShortPressure.Signal.HEAVY_SHORTING, sp!!.signal)
        assertEquals(ShortPressure.Trend.RISING, sp.trend)
        assertEquals(38.0, sp.ratioPercent, 0.0)
    }

    @Test
    fun `공매도 비중 하락(-20퍼센트)이면 shortCovering`() {
        val ratios = listOf(45.0, 44.0, 43.0, 35.0) // 35 vs 최근평균 44 = -20% < -15%
        val sp = calc.calculate(ratios)
        assertNotNull(sp)
        assertEquals(ShortPressure.Signal.SHORT_COVERING, sp!!.signal)
        assertEquals(ShortPressure.Trend.FALLING, sp.trend)
    }

    @Test
    fun `평탄(±15퍼센트 이내)이면 neutral`() {
        val ratios = listOf(40.0, 40.2, 39.9, 40.1)
        val sp = calc.calculate(ratios)
        assertNotNull(sp)
        assertEquals(ShortPressure.Signal.NEUTRAL, sp!!.signal)
        assertEquals(ShortPressure.Trend.FLAT, sp.trend)
    }

    @Test
    fun `KOSPI 미세 스케일에서도 상대 추세 동작(scale-invariant)`() {
        // 최근평균(0.04,0.09,0.03,0.05)=0.0525, 최신 0.10 → +90% → 상승
        val sp = calc.calculate(listOf(0.04, 0.09, 0.03, 0.05, 0.10))
        assertNotNull(sp)
        assertEquals(ShortPressure.Signal.HEAVY_SHORTING, sp!!.signal)
        assertEquals(ShortPressure.Trend.RISING, sp.trend)
    }

    @Test
    fun `baseline 0(전 구간 무공매도)이면 neutral(0 나눗셈 방지)`() {
        val sp = calc.calculate(listOf(0.0, 0.0, 0.0, 0.5))
        assertNotNull(sp)
        assertEquals(ShortPressure.Signal.NEUTRAL, sp!!.signal)
        assertEquals(ShortPressure.Trend.FLAT, sp.trend)
    }

    @Test
    fun `데이터 3개 미만이면 null(카드 숨김)`() {
        assertNull(calc.calculate(listOf(40.0, 41.0)))
    }
}
