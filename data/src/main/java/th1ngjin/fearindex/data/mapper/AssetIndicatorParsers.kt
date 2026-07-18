package th1ngjin.fearindex.data.mapper

import th1ngjin.fearindex.data.dto.KospiHistoryDTO
import th1ngjin.fearindex.data.dto.YahooCloseChartResponse

/**
 * RSI/공매도 보조 지표 파서 — 순수 함수 (iOS AssetPriceCloseParser/AssetShortRatioParser 대칭).
 * 네트워크/캐시는 Repository 가 담당, 여기는 페이로드 → 시계열 변환만(SRP).
 */

/** Yahoo chart 응답 → 일봉 종가(시간순, 휴장일 null 제거). */
object YahooCloseParser {
    fun closes(response: YahooCloseChartResponse): List<Double> =
        response.chart.result
            ?.firstOrNull()
            ?.indicators
            ?.quote
            ?.firstOrNull()
            ?.close
            ?.filterNotNull()
            ?: emptyList()
}

/** FINRA CNMSshvol 일별 텍스트 → 심볼 공매도 비중(%) = ShortVolume/TotalVolume*100. */
object FinraShortVolumeParser {
    /** 형식: `Date|Symbol|ShortVolume|ShortExemptVolume|TotalVolume|Market` */
    fun shortRatioPercent(text: String, symbol: String): Double? {
        for (line in text.lineSequence()) {
            val fields = line.split("|")
            if (fields.size < 5 || fields[1] != symbol) continue
            val short = fields[2].toDoubleOrNull() ?: return null
            val total = fields[4].toDoubleOrNull() ?: return null
            if (total <= 0) return null
            return short / total * 100
        }
        return null
    }
}

/** KOSPI 스냅샷 chartHistoryForDisplay → kospiClose 종가(날짜 오름차순, null 제거). */
object KospiCloseParser {
    fun closes(history: List<KospiHistoryDTO>): List<Double> =
        history.sortedBy { it.date }.mapNotNull { it.kospiClose }

    /** RSI 출처 라벨 asOf용 — 종가가 존재하는 마지막 날짜(yyyy-MM-dd). */
    fun lastCloseDate(history: List<KospiHistoryDTO>): String? =
        history.filter { it.kospiClose != null }.maxOfOrNull { it.date }
}
