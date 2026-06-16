package th1ngjin.fearindex.domain.entity

import java.time.ZoneId

/**
 * 지수 종류별 날짜/시각 표시 컨텍스트 (타임존).
 *
 * 현재 지수의 업데이트 시각을 각 시장의 현지 타임존으로 표시하기 위함.
 * iOS `FearIndexDateContext` 와 1:1 대응.
 */
enum class FearIndexDateContext(val zoneIdentifier: String) {
    /** 글로벌 시장(S&P 500) — 뉴욕 증시 기준. */
    GLOBAL_MARKET("America/New_York"),

    /** KOSPI — 한국 증시 기준. */
    KOSPI("Asia/Seoul"),

    /** 암호화폐 — 24시간 거래, UTC 기준. */
    CRYPTO("UTC");

    val zoneId: ZoneId get() = ZoneId.of(zoneIdentifier)
}

/** 지수 종류 → 날짜 컨텍스트 매핑. iOS `FearIndexType.dateContext` 대칭. */
val FearIndexType.dateContext: FearIndexDateContext
    get() = when (this) {
        FearIndexType.MARKET -> FearIndexDateContext.GLOBAL_MARKET
        FearIndexType.KOSPI -> FearIndexDateContext.KOSPI
        FearIndexType.CRYPTO -> FearIndexDateContext.CRYPTO
    }
