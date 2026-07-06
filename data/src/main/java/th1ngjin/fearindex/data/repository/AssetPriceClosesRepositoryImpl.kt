package th1ngjin.fearindex.data.repository

import kotlinx.coroutines.withTimeout
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.KospiFearIndexDataSource
import th1ngjin.fearindex.data.datasource.OfficialIndicatorsApi
import th1ngjin.fearindex.data.datasource.YahooChartApi
import th1ngjin.fearindex.data.mapper.KospiCloseParser
import th1ngjin.fearindex.data.mapper.YahooCloseParser
import th1ngjin.fearindex.domain.entity.AssetCloseSeries
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.IndicatorSourceMetadata
import th1ngjin.fearindex.domain.repository.AssetPriceClosesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자산 일봉 종가 라우팅 — iOS `AssetPriceCloseDataSource` 대응 (v1.8.8).
 * - MARKET: Yahoo ^GSPC(6mo) 기기 직접 호출 — 서버 endpoint 없음 (FINRA/FRED가 GCP IP 차단).
 * - CRYPTO: 서버 cryptoOfficialIndicatorsV1 (Binance BTCUSDT 일봉 180개 + 서버 메타데이터).
 * - KOSPI: 기존 스냅샷의 kospiClose (추가 API 없음).
 * 외부 호출은 8초 strict timeout (SDK hang 방어). 출처 라벨은 locale-neutral 영문 고정.
 */
@Singleton
class AssetPriceClosesRepositoryImpl @Inject constructor(
    private val yahooApi: YahooChartApi,
    private val officialApi: OfficialIndicatorsApi,
    private val kospiDataSource: KospiFearIndexDataSource,
) : AssetPriceClosesRepository {

    override suspend fun dailyCloses(type: FearIndexType): AssetCloseSeries {
        if (ScreenshotMode.isEnabled()) return AssetCloseSeries.EMPTY
        return when (type) {
            FearIndexType.MARKET -> withTimeout(TIMEOUT_MS) {
                AssetCloseSeries(
                    closes = YahooCloseParser.closes(
                        yahooApi.getCloseChart(SPX_SYMBOL, "1d", "6mo"),
                    ),
                    sourceMetadata = MARKET_RSI_METADATA,
                )
            }
            FearIndexType.CRYPTO -> withTimeout(TIMEOUT_MS) {
                val rsi = officialApi.getCryptoOfficialIndicators().rsi
                AssetCloseSeries(
                    closes = if (rsi.available) rsi.closesOrValues else emptyList(),
                    sourceMetadata = rsi.toSourceMetadata(),
                )
            }
            FearIndexType.KOSPI -> {
                val history = kospiDataSource
                    .fetchSnapshot(includeHistory = true, forceRefresh = false)
                    .chartHistoryForDisplay
                AssetCloseSeries(
                    closes = KospiCloseParser.closes(history),
                    sourceMetadata = KOSPI_RSI_METADATA.copy(
                        asOf = KospiCloseParser.lastCloseDate(history),
                    ),
                )
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000L
        const val SPX_SYMBOL = "^GSPC"

        /** 클라 하드코딩 메타데이터 — iOS 동일 문자열, 번역 금지. Yahoo는 비공식 소스(경고 아이콘). */
        val MARKET_RSI_METADATA = IndicatorSourceMetadata(
            sourceName = "Yahoo Finance ^GSPC",
            basisLabel = "S&P 500",
            asOf = null,
            isOfficial = false,
            methodology = "14-day RSI from Yahoo Finance ^GSPC daily closes.",
        )
        val KOSPI_RSI_METADATA = IndicatorSourceMetadata(
            sourceName = "KIS/KRX KOSPI",
            basisLabel = "KOSPI",
            asOf = null,
            isOfficial = true,
            methodology = "Self-computed 14-day RSI from official KOSPI daily closes.",
        )
    }
}
