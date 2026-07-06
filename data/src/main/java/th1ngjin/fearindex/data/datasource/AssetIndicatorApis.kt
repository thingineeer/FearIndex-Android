package th1ngjin.fearindex.data.datasource

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import th1ngjin.fearindex.data.dto.OfficialIndicatorsResponse
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** FINRA RegSHO 일별 공매도 파일 (파이프 구분 텍스트, 무인증). */
interface FinraShortVolumeApi {
    @GET("equity/regsho/daily/CNMSshvol{ymd}.txt")
    suspend fun getDailyShortVolume(@Path("ymd") ymd: String): ResponseBody
}

/**
 * 서버 official indicators endpoint (Cloud Functions asia-northeast3).
 * BTC 지표는 Binance 공식 소스를 서버가 수집 — 클라는 소비만 (iOS v1.8.8 parity).
 */
interface OfficialIndicatorsApi {
    @GET("cryptoOfficialIndicatorsV1")
    suspend fun getCryptoOfficialIndicators(): OfficialIndicatorsResponse
}

/**
 * FINRA 후보 거래일 생성 — 어제부터(당일 파일 미발행) 주말 제외 count개.
 * iOS SPXShortRatioDataSource 후보일 로직 대응. 미발행/휴장일은 fetch 실패로 걸러진다.
 */
object FinraTradingDays {
    private val format = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun candidates(todayNy: LocalDate, count: Int): List<String> {
        val result = mutableListOf<String>()
        var day = todayNy.minusDays(1)
        var scanned = 0
        while (result.size < count && scanned < LOOKBACK_CALENDAR_DAYS) {
            scanned++
            if (day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY) {
                result.add(day.format(format))
            }
            day = day.minusDays(1)
        }
        return result
    }

    private const val LOOKBACK_CALENDAR_DAYS = 14
}
