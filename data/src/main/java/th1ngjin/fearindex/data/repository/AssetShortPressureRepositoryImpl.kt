package th1ngjin.fearindex.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.FinraShortVolumeApi
import th1ngjin.fearindex.data.datasource.FinraTradingDays
import th1ngjin.fearindex.data.datasource.KospiFearIndexApi
import th1ngjin.fearindex.data.datasource.OfficialIndicatorsApi
import th1ngjin.fearindex.data.mapper.FinraShortVolumeParser
import th1ngjin.fearindex.domain.entity.AssetShortRatioSeries
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.IndicatorSourceMetadata
import th1ngjin.fearindex.domain.repository.AssetShortPressureRepository
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자산 공매도 비중 라우팅 — iOS `AssetShortRatioDataSource` 대응 (v1.8.8).
 * - MARKET: FINRA RegSHO 일별 .txt (SPY) 기기 직접 → 최근 3거래일 병렬 fetch + 12h 캐시.
 * - CRYPTO: 서버 cryptoOfficialIndicatorsV1 (Binance 롱숏 shortAccount %, 14일) + 30m 캐시.
 * - KOSPI: 서버 /api/kospi/short + 1h 캐시. KRX 공식 소스 확정 전까지 available=false(카드 숨김).
 * 부분 실패 허용(FINRA 주말/미발행), 8초 strict timeout. 출처 라벨은 locale-neutral 영문 고정.
 */
@Singleton
class AssetShortPressureRepositoryImpl internal constructor(
    private val finraApi: FinraShortVolumeApi,
    private val officialApi: OfficialIndicatorsApi,
    private val kospiApi: KospiFearIndexApi,
    private val now: () -> Instant,
) : AssetShortPressureRepository {

    @Inject
    constructor(
        finraApi: FinraShortVolumeApi,
        officialApi: OfficialIndicatorsApi,
        kospiApi: KospiFearIndexApi,
    ) : this(finraApi, officialApi, kospiApi, { Instant.now() })

    private data class CacheEntry(val series: AssetShortRatioSeries, val expiresAtMs: Long)

    private val cache = mutableMapOf<FearIndexType, CacheEntry>()
    private val cacheMutex = Mutex()

    override suspend fun dailyShortRatios(type: FearIndexType): AssetShortRatioSeries {
        if (ScreenshotMode.isEnabled()) return AssetShortRatioSeries.EMPTY
        cached(type)?.let { return it }
        val series = when (type) {
            FearIndexType.MARKET -> fetchFinraSeries()
            FearIndexType.CRYPTO -> withTimeout(TIMEOUT_MS) {
                val short = officialApi.getCryptoOfficialIndicators().short
                AssetShortRatioSeries(
                    ratios = if (short.available) short.ratiosOrValues else emptyList(),
                    sourceMetadata = short.toSourceMetadata(),
                )
            }
            FearIndexType.KOSPI -> withTimeout(TIMEOUT_MS) {
                val response = kospiApi.getKospiShort()
                AssetShortRatioSeries(
                    ratios = if (response.available) response.shortRatios else emptyList(),
                    sourceMetadata = response.toSourceMetadata(),
                )
            }
        }
        // 3개 미만이면 계산 불가 데이터라 캐시하지 않음 (다음 호출에서 재시도).
        if (series.ratios.size >= MIN_RATIOS) save(type, series)
        return series
    }

    /** 후보 거래일(주말 제외, 넉넉히 +2일) 병렬 fetch — 미발행/휴장일은 스킵하고 최근 3일만 사용. */
    private suspend fun fetchFinraSeries(): AssetShortRatioSeries = coroutineScope {
        val todayNy = now().atZone(NY_ZONE).toLocalDate()
        val candidates = FinraTradingDays.candidates(todayNy, count = MAX_TRADING_DAYS + 2)
        val recent = candidates
            .map { ymd ->
                async {
                    runCatching { ymd to fetchFinraRatio(ymd) }
                        .onFailure { Timber.d("FINRA $ymd 스킵: ${it.message}") }
                        .getOrNull()
                }
            }
            .mapNotNull { it.await() }
            .mapNotNull { (ymd, ratio) -> ratio?.let { ymd to it } }
            .sortedBy { it.first }
            .takeLast(MAX_TRADING_DAYS)
        AssetShortRatioSeries(
            ratios = recent.map { it.second },
            sourceMetadata = MARKET_SHORT_METADATA.copy(
                asOf = recent.lastOrNull()?.first?.let(::formatFinraDate),
            ),
        )
    }

    private suspend fun fetchFinraRatio(ymd: String): Double? = withTimeout(TIMEOUT_MS) {
        FinraShortVolumeParser.shortRatioPercent(
            text = finraApi.getDailyShortVolume(ymd).string(),
            symbol = SPX_PROXY_SYMBOL,
        )
    }

    /** FINRA 파일 날짜 "yyyyMMdd" → asOf 표기 "yyyy-MM-dd". */
    private fun formatFinraDate(ymd: String): String =
        "${ymd.take(4)}-${ymd.substring(4, 6)}-${ymd.substring(6, 8)}"

    private suspend fun cached(type: FearIndexType): AssetShortRatioSeries? = cacheMutex.withLock {
        cache[type]?.takeIf { now().toEpochMilli() < it.expiresAtMs }?.series
    }

    private suspend fun save(type: FearIndexType, series: AssetShortRatioSeries) =
        cacheMutex.withLock {
            cache[type] = CacheEntry(series, now().toEpochMilli() + ttlMs(type))
        }

    private fun ttlMs(type: FearIndexType): Long = when (type) {
        FearIndexType.MARKET -> 12 * 60 * 60 * 1000L // 일별 파일 → 12h
        FearIndexType.CRYPTO -> 30 * 60 * 1000L // 롱숏비율 → 30m
        FearIndexType.KOSPI -> 60 * 60 * 1000L // 서버 집계 → 1h
    }

    private companion object {
        const val TIMEOUT_MS = 8_000L
        const val MAX_TRADING_DAYS = 3
        const val MIN_RATIOS = 3
        const val SPX_PROXY_SYMBOL = "SPY"
        val NY_ZONE: ZoneId = ZoneId.of("America/New_York")

        /** 클라 하드코딩 메타데이터 — iOS 동일 문자열, 번역 금지. */
        val MARKET_SHORT_METADATA = IndicatorSourceMetadata(
            sourceName = "FINRA Daily Short Sale Volume",
            basisLabel = "SPY ETF",
            asOf = null,
            isOfficial = true,
            methodology = "SPY ShortVolume / TotalVolume from FINRA CNMS daily files.",
        )
    }
}
