package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 주요 시장 지수 모델. iOS `MarketIndex` 와 1:1 대응.
 */
data class MarketIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double = 0.0,
    val changePercent: Double,
    val timestamp: Instant = Instant.EPOCH,
) {
    /** 상승/하락 여부 (change 기준, iOS isPositive 대칭). */
    val isPositive: Boolean get() = change >= 0

    /** 지수 타입 (symbol 기반). 알 수 없으면 null. */
    val type: MarketIndexType? get() = MarketIndexType.from(symbol)
}

/**
 * 지원하는 시장 지수 타입. iOS `MarketIndexType` 와 1:1 대응.
 * displayName 은 strings.xml 키(resKey)로 매핑 — presentation 에서 stringResource 처리.
 */
enum class MarketIndexType(
    val symbol: String,
    /** 공식 심볼 (예: COMP, SPX, DXY). */
    val officialSymbol: String,
    /** strings.xml 키 (market_xxx). */
    val displayNameKey: String,
    /** 상세 화면 공식 표시명 키 (대부분 displayNameKey 와 동일, NASDAQ/DXY만 별도). */
    val detailDisplayNameKey: String,
    /** 상세 화면에서 provider 심볼을 숨기는지 (KOSPI/KOSDAQ). */
    val hidesProviderSymbolInDetail: Boolean = false,
    /** Yahoo Finance 로 조회하는지 (한국 지수는 Naver). */
    val usesYahooFinance: Boolean = true,
) {
    NASDAQ("^IXIC", "COMP", "market_nasdaq", "market_detail_nasdaq_composite"),
    NASDAQ100_FUTURES("NQ=F", "NQ", "market_nasdaq100_futures", "market_nasdaq100_futures"),
    SP500("^GSPC", "SPX", "market_sp500", "market_sp500"),
    RUSSELL2000_FUTURES("RTY=F", "RTY", "market_russell2000_futures", "market_russell2000_futures"),
    DOW_JONES("^DJI", "DJI", "market_dow_jones", "market_dow_jones"),
    PHILADELPHIA_SEMICONDUCTOR("^SOX", "SOX", "market_philadelphia_semiconductor", "market_philadelphia_semiconductor"),
    VIX("^VIX", "VIX", "market_vix", "market_vix"),
    DOLLAR_INDEX("DX-Y.NYB", "DXY", "market_dollar_index", "market_detail_dollar_index_official"),
    KOSPI("^KS11", "KOSPI", "market_kospi", "market_kospi", hidesProviderSymbolInDetail = true, usesYahooFinance = false),
    KOSDAQ("^KQ11", "KOSDAQ", "market_kosdaq", "market_kosdaq", hidesProviderSymbolInDetail = true, usesYahooFinance = false);

    /** provider 심볼 표시 (^ 접두사 제거). 선물(NQ=F 등)은 원본 유지. */
    val providerDisplaySymbol: String
        get() = if (symbol.startsWith("^")) symbol.drop(1) else symbol

    /**
     * 상세 화면 보조 심볼.
     * - KOSPI/KOSDAQ: 공식 심볼만
     * - 공식≠provider: "공식 · provider" (예: "COMP · IXIC")
     * - 같으면 provider 만
     */
    val detailSubtitle: String
        get() = when {
            hidesProviderSymbolInDetail -> officialSymbol
            officialSymbol != providerDisplaySymbol -> "$officialSymbol · $providerDisplaySymbol"
            else -> providerDisplaySymbol
        }

    companion object {
        fun from(symbol: String): MarketIndexType? = entries.firstOrNull { it.symbol == symbol }

        /** 지수 탭 (글로벌 7 + 한국 2). iOS globalIndexCases + koreanIndexCases 순서. */
        val indicesTabCases: List<MarketIndexType>
            get() = listOf(
                NASDAQ, NASDAQ100_FUTURES, SP500, RUSSELL2000_FUTURES,
                DOW_JONES, PHILADELPHIA_SEMICONDUCTOR, VIX, KOSPI, KOSDAQ,
            )

        /** 환율 탭 지수 (달러지수). */
        val exchangeIndexCases: List<MarketIndexType> get() = listOf(DOLLAR_INDEX)
    }
}
