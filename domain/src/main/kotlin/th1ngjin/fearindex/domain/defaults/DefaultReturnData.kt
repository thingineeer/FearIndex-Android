package th1ngjin.fearindex.domain.defaults

import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.entity.ReturnEventEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * iOS DefaultReturnData.swift 1:1 포팅.
 *
 * 검증일: 2026-04-08 (StatMuse S&P 500, whit3rabbit F&G CSV)
 * SSOT: firebase-functions/seed-return-data.ts
 */
object DefaultReturnData {

    val market: ReturnDataTable by lazy {
        val anchors = listOf(
            dp(0, r(20.5, 30.2, 38.0, 55.0), r(14.0, 15.0, 18.0, 25.0), r(28.0, 45.0, 55.0, 80.0), 8),
            dp(12, r(16.8, 25.4, 31.5, 43.2), r(11.6, 11.4, 14.4, 21.6), r(25.1, 39.9, 44.7, 74.8), 14),
            dp(37, r(5.2, 9.2, 13.2, 14.5), r(3.5, 6.1, 6.4, -7.6), r(7.5, 13.3, 17.2, 34.0), 12),
            dp(62, r(2.9, 2.9, 5.6, 15.1), r(1.4, 0.6, -2.6, 7.4), r(5.1, 7.0, 12.9, 23.7), 10),
            dp(88, r(3.1, -2.2, 5.7, 3.5), r(0.2, -13.7, -3.1, -15.4), r(7.8, 3.7, 13.4, 14.1), 14),
            dp(100, r(1.5, -5.0, 2.0, -3.0), r(-2.0, -18.0, -8.0, -20.0), r(6.0, 2.0, 10.0, 10.0), 10),
        )
        ReturnDataTable(
            version = 1,
            updatedAt = Instant.now(),
            dataPoints = interpolate(anchors),
            historicalEvents = marketEvents(),
        )
    }

    val crypto: ReturnDataTable by lazy {
        val anchors = listOf(
            dp(0, r(25.0, 50.0, 120.0, 300.0), r(-50.0, -35.0, -20.0, -15.0), r(150.0, 250.0, 600.0, 1500.0), 6),
            dp(15, r(18.0, 38.4, 85.0, 300.0), r(-40.0, -25.0, -15.0, -10.0), r(123.0, 200.0, 500.0, 1500.0), 8),
            dp(37, r(8.0, 18.0, 40.0, 25.0), r(-25.0, -15.0, -8.0, -55.0), r(50.0, 80.0, 200.0, 200.0), 10),
            dp(62, r(3.0, 8.0, 15.0, 40.0), r(-15.0, -10.0, -5.0, -30.0), r(25.0, 40.0, 80.0, 150.0), 10),
            dp(88, r(-2.0, -8.0, -5.0, -25.0), r(-30.0, -40.0, -50.0, -76.0), r(15.0, 20.0, 30.0, 50.0), 8),
            dp(100, r(-5.0, -15.0, -10.0, -35.0), r(-40.0, -55.0, -65.0, -80.0), r(10.0, 10.0, 15.0, 20.0), 6),
        )
        ReturnDataTable(
            version = 1,
            updatedAt = Instant.now(),
            dataPoints = interpolate(anchors),
            historicalEvents = cryptoEvents(),
        )
    }

    // -- Interpolation (1점 단위 → 101개) --

    private fun interpolate(anchors: List<ReturnDataPoint>): List<ReturnDataPoint> {
        val sorted = anchors.sortedBy { it.score }
        return (0..100).mapNotNull { s ->
            sorted.firstOrNull { it.score == s }?.let { return@mapNotNull it }
            val lo = sorted.lastOrNull { it.score <= s } ?: return@mapNotNull null
            val hi = sorted.firstOrNull { it.score >= s } ?: return@mapNotNull null
            if (lo.score == hi.score) return@mapNotNull null
            val t = (s - lo.score).toDouble() / (hi.score - lo.score)
            dp(
                s, lerpR(lo.returns, hi.returns, t),
                lerpR(lo.worstCase, hi.worstCase, t),
                lerpR(lo.bestCase, hi.bestCase, t),
                (lo.sampleCount + (hi.sampleCount - lo.sampleCount) * t).roundToInt(),
            )
        }
    }

    // -- Market Events (StatMuse S&P 500 검증 완료) --

    private fun marketEvents(): List<ReturnEventEntry> = listOf(
        ev("covid", d(2020, 3, 12), 2, "COVID-19 대폭락", r(11.3, 22.6, 36.4, 58.9)),
        ev("tariff-2025", d(2025, 4, 8), 4, "2025 관세 충격", r(13.7, 24.9, 35.5, 31.7)),
        ev("inflation-2022", d(2022, 10, 12), 15, "인플레이션 공포", r(10.6, 11.4, 14.4, 21.6)),
        ev("delta-2021", d(2021, 7, 19), 20, "델타 변이 우려", r(3.5, 6.1, 6.4, -7.6)),
        ev("trade-war-2019", d(2019, 8, 5), 22, "미중 무역전쟁", r(4.6, 8.1, 17.2, 17.0)),
        ev("svb-2023", d(2023, 3, 13), 23, "SVB 은행 파산", r(7.5, 13.3, 15.9, 34.0)),
        ev("reopening-2021", d(2021, 3, 15), 55, "경제 재개 기대", r(5.1, 7.0, 12.9, 7.4)),
        ev("post-election-2024", d(2024, 11, 11), 68, "2024 대선 이후", r(1.4, 1.1, -2.6, 14.1)),
        ev("2021bull", d(2021, 2, 12), 73, "2021 강세장", r(0.2, 3.3, 13.4, 11.9)),
        ev("mid-2023", d(2023, 6, 15), 80, "AI 랠리", r(2.2, 0.6, 6.6, 23.7)),
        ev("pre-covid-high", d(2020, 1, 17), 89, "코로나 직전 고점", r(1.2, -13.7, -3.1, 14.1)),
        ev("dotcom", d(2000, 3, 10), 95, "닷컴 버블", r(7.8, 3.7, 6.8, -15.4)),
    )

    // -- Crypto Events (StatMuse BTC, alternative.me API 검증 완료) --

    private fun cryptoEvents(): List<ReturnEventEntry> = listOf(
        ev("crypto-covid", d(2020, 3, 13), 10, "COVID-19 암호화폐 폭락", r(23.0, 70.3, 85.6, 996.2)),
        ev("crypto-luna", d(2022, 5, 12), 12, "루나/테라 붕괴", r(-6.9, -16.0, -42.2, -8.1)),
        ev("crypto-ftx", d(2022, 11, 9), 20, "FTX 파산", r(8.2, 37.6, 73.9, 130.9)),
        ev("crypto-china-ban", d(2021, 9, 24), 27, "중국 암호화폐 금지", r(42.2, 18.9, 2.6, -55.8)),
        ev("crypto-svb", d(2023, 3, 13), 33, "SVB 은행 위기", r(25.5, 6.6, 8.4, 201.7)),
        ev("crypto-sec", d(2023, 6, 10), 47, "SEC 규제 강화", r(17.5, -0.2, 69.1, 168.6)),
        ev("crypto-halving-2024", d(2024, 4, 20), 57, "비트코인 4차 반감기", r(7.3, 3.3, 6.1, 30.9)),
        ev("crypto-etf-approval", d(2024, 1, 10), 73, "비트코인 ETF 승인", r(2.4, 51.4, 23.8, 103.0)),
        ev("crypto-defi-summer", d(2020, 8, 15), 79, "DeFi 서머", r(-9.0, 34.5, 308.7, 296.6)),
        ev("crypto-2021-peak", d(2021, 11, 10), 84, "2021 암호화폐 고점", r(-27.6, -33.5, -53.4, -73.1)),
        ev("crypto-trump-pump", d(2024, 12, 5), 84, "트럼프 펌프", r(-0.6, -8.4, 2.9, -9.7)),
        ev("crypto-2017-peak", d(2017, 12, 17), 95, "2017 암호화폐 고점", r(-41.5, -58.6, -66.0, -81.5)),
    )

    // -- Factories --

    private fun r(m1: Double, m3: Double, m6: Double, y1: Double) =
        HistoricalReturns(oneMonth = m1, threeMonth = m3, sixMonth = m6, oneYear = y1)

    private fun dp(score: Int, ret: HistoricalReturns, worst: HistoricalReturns, best: HistoricalReturns, count: Int) =
        ReturnDataPoint(score = score, returns = ret, worstCase = worst, bestCase = best, sampleCount = count)

    private fun ev(id: String, date: Instant, score: Int, key: String, ret: HistoricalReturns) =
        ReturnEventEntry(id = id, date = date, score = score, descriptionKey = key, returnAfter = ret)

    private fun d(year: Int, month: Int, day: Int): Instant =
        LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun lerpR(a: HistoricalReturns, b: HistoricalReturns, t: Double) = r(
        round1(a.oneMonth + (b.oneMonth - a.oneMonth) * t),
        round1(a.threeMonth + (b.threeMonth - a.threeMonth) * t),
        round1(a.sixMonth + (b.sixMonth - a.sixMonth) * t),
        round1(a.oneYear + (b.oneYear - a.oneYear) * t),
    )

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
}
