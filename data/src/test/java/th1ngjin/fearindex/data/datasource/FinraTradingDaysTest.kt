package th1ngjin.fearindex.data.datasource

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * FINRA 후보 거래일 생성 — 어제부터(당일 미발행) 주말 제외, count개.
 * iOS SPXShortRatioDataSource 후보일 로직 대응.
 */
class FinraTradingDaysTest {

    @Test
    fun `평일 기준 - 어제부터 주말 건너뛰고 count개`() {
        // NY 2026-07-03(금) → 0702(목),0701(수),0630(화),0629(월),0626(금)
        val candidates = FinraTradingDays.candidates(LocalDate.of(2026, 7, 3), count = 5)
        assertEquals(listOf("20260702", "20260701", "20260630", "20260629", "20260626"), candidates)
    }

    @Test
    fun `월요일 기준 - 어제(일)와 토요일 스킵`() {
        // NY 2026-07-06(월) → 0703(금),0702(목),0701(수),0630(화),0629(월)
        val candidates = FinraTradingDays.candidates(LocalDate.of(2026, 7, 6), count = 5)
        assertEquals(listOf("20260703", "20260702", "20260701", "20260630", "20260629"), candidates)
    }
}
