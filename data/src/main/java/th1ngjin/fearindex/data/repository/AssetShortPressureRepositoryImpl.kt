package th1ngjin.fearindex.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.BinanceFuturesApi
import th1ngjin.fearindex.data.datasource.FinraShortVolumeApi
import th1ngjin.fearindex.data.datasource.FinraTradingDays
import th1ngjin.fearindex.data.datasource.KospiFearIndexApi
import th1ngjin.fearindex.data.mapper.BinanceShortRatioParser
import th1ngjin.fearindex.data.mapper.FinraShortVolumeParser
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.repository.AssetShortPressureRepository
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자산 공매도 비중 라우팅 — iOS `AssetShortPressureRepository`/`AssetShortRatioDataSource` 대응.
 * - MARKET: FINRA RegSHO 일별 .txt (SPY, 하루 ~1.5MB) → 최근 3거래일 병렬 fetch + 12h 캐시.
 * - CRYPTO: Binance globalLongShortAccountRatio (소형 시계열) + 30m 캐시.
 * - KOSPI: 서버 /api/kospi/short (KIS 시총상위 합산) + 1h 캐시.
 * 부분 실패 허용(FINRA 주말/미발행), 8초 strict timeout.
 */
@Singleton
class AssetShortPressureRepositoryImpl internal constructor(
    private val finraApi: FinraShortVolumeApi,
    private val binanceApi: BinanceFuturesApi,
    private val kospiApi: KospiFearIndexApi,
    private val now: () -> Instant,
) : AssetShortPressureRepository {

    @Inject
    constructor(
        finraApi: FinraShortVolumeApi,
        binanceApi: BinanceFuturesApi,
        kospiApi: KospiFearIndexApi,
    ) : this(finraApi, binanceApi, kospiApi, { Instant.now() })

    private data class CacheEntry(val ratios: List<Double>, val expiresAtMs: Long)

    private val cache = mutableMapOf<FearIndexType, CacheEntry>()
    private val cacheMutex = Mutex()

    override suspend fun dailyShortRatios(type: FearIndexType): List<Double> {
        if (ScreenshotMode.isEnabled()) return emptyList()
        cached(type)?.let { return it }
        val ratios = when (type) {
            FearIndexType.MARKET -> fetchFinraRatios()
            FearIndexType.CRYPTO -> withTimeout(TIMEOUT_MS) {
                BinanceShortRatioParser.shortRatios(binanceApi.getGlobalLongShortAccountRatio())
            }
            FearIndexType.KOSPI -> withTimeout(TIMEOUT_MS) {
                kospiApi.getKospiShort().shortRatios
            }
        }
        // 3개 미만이면 계산 불가 데이터라 캐시하지 않음 (다음 호출에서 재시도).
        if (ratios.size >= MIN_RATIOS) save(type, ratios)
        return ratios
    }

    /** 후보 거래일(주말 제외, 넉넉히 +2일) 병렬 fetch — 미발행/휴장일은 스킵하고 최근 3일만 사용. */
    private suspend fun fetchFinraRatios(): List<Double> = coroutineScope {
        val todayNy = now().atZone(NY_ZONE).toLocalDate()
        val candidates = FinraTradingDays.candidates(todayNy, count = MAX_TRADING_DAYS + 2)
        candidates
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
            .map { it.second }
    }

    private suspend fun fetchFinraRatio(ymd: String): Double? = withTimeout(TIMEOUT_MS) {
        FinraShortVolumeParser.shortRatioPercent(
            text = finraApi.getDailyShortVolume(ymd).string(),
            symbol = SPX_PROXY_SYMBOL,
        )
    }

    private suspend fun cached(type: FearIndexType): List<Double>? = cacheMutex.withLock {
        cache[type]?.takeIf { now().toEpochMilli() < it.expiresAtMs }?.ratios
    }

    private suspend fun save(type: FearIndexType, ratios: List<Double>) = cacheMutex.withLock {
        cache[type] = CacheEntry(ratios, now().toEpochMilli() + ttlMs(type))
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
    }
}
