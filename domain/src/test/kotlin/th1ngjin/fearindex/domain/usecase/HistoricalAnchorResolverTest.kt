package th1ngjin.fearindex.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexDateContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * iOS `HistoricalAnchorResolverTests` 와 1:1 대응.
 *
 * 비교 카드(전일/1주전/1개월전/1년전) 앵커를 **배열 인덱스가 아니라 날짜 기반**으로
 * 고른다 — 정확히 N일/개월/년 전 날짜 이하의 가장 최신 데이터.
 */
class HistoricalAnchorResolverTest {

    private fun index(score: Double, day: String, context: FearIndexDateContext): FearIndex {
        val instant = LocalDate.parse(day).atStartOfDay(context.zoneId).toInstant()
        return FearIndex(
            score = score,
            rating = FearIndex.Rating.from(score),
            timestamp = instant,
            previousClose = score,
            previous1Week = score,
            previous1Month = score,
        )
    }

    private fun refDate(day: String, zone: ZoneId) =
        LocalDate.parse(day).atStartOfDay(zone).toInstant()

    @Test
    fun `crypto - 배열 인덱스가 아니라 UTC 날짜로 앵커`() {
        val ctx = FearIndexDateContext.CRYPTO
        val current = index(10.0, "2024-03-04", ctx)
        val history = listOf(
            index(88.0, "2023-03-04", ctx),
            index(47.0, "2024-02-02", ctx),
            index(45.0, "2024-02-26", ctx),
            index(15.0, "2024-03-03", ctx),
            index(10.0, "2024-03-04", ctx),
        )

        val result = HistoricalAnchorResolver.resolve(
            current = current,
            history = history,
            context = ctx,
        )

        assertEquals(15.0, result.previousClose, 0.0)   // 기준일(03-04) 이전 최신 = 03-03
        assertEquals(45.0, result.previous1Week, 0.0)   // 02-26 이하 최신 = 02-26
        assertEquals(47.0, result.previous1Month, 0.0)  // 02-04 이하 최신 = 02-02
        assertEquals(88.0, result.previous1Year!!, 0.0) // 2023-03-04 이하 최신 = 2023-03-04
    }

    @Test
    fun `kospi - 최신 거래일 기준 + 휴장일 건너뜀`() {
        val ctx = FearIndexDateContext.KOSPI
        val current = index(32.1, "2026-06-07", ctx)
        val reference = refDate("2026-06-05", ctx.zoneId)
        val history = listOf(
            index(75.8, "2025-06-05", ctx),
            index(60.7, "2026-05-04", ctx),
            index(43.0, "2026-05-29", ctx),
            index(41.1, "2026-06-04", ctx),
            index(32.1, "2026-06-05", ctx),
        )

        val result = HistoricalAnchorResolver.resolve(
            current = current,
            history = history,
            context = ctx,
            referenceDate = reference,
        )

        assertEquals(41.1, result.previousClose, 0.0)
        assertEquals(43.0, result.previous1Week, 0.0)
        assertEquals(60.7, result.previous1Month, 0.0)
        assertEquals(75.8, result.previous1Year!!, 0.0)
    }

    @Test
    fun `빈 history - 모든 앵커는 현재 점수로 fallback (1년전은 null)`() {
        val ctx = FearIndexDateContext.CRYPTO
        val current = index(50.0, "2024-03-04", ctx)

        val result = HistoricalAnchorResolver.resolve(
            current = current,
            history = emptyList(),
            context = ctx,
        )

        assertEquals(50.0, result.previousClose, 0.0)
        assertEquals(50.0, result.previous1Week, 0.0)
        assertEquals(50.0, result.previous1Month, 0.0)
        assertNull(result.previous1Year) // 1년전은 fallback 없이 null
    }

    @Test
    fun `enrich - current 의 score 는 보존하고 앵커만 채움`() {
        val ctx = FearIndexDateContext.CRYPTO
        val current = index(10.0, "2024-03-04", ctx)
        val history = listOf(index(15.0, "2024-03-03", ctx))

        val enriched = HistoricalAnchorResolver.enrich(current, history, ctx)

        assertEquals(10.0, enriched.score, 0.0)          // score 보존
        assertEquals(15.0, enriched.previousClose!!, 0.0) // 앵커 갱신
    }
}
