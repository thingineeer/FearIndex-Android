package th1ngjin.fearindex.presentation.component

private const val MIN_BANNER_WIDTH_DP = 320
private const val MAX_INLINE_BANNER_HEIGHT_DP = 120

/** 표준 배너 높이(50dp). 로드 전 추정 높이가 없거나 0일 때 공간 확보용 fallback. */
internal const val FALLBACK_BANNER_HEIGHT_DP = 50

/** 테스트에서 클램프 상한을 검증하기 위한 공개 별칭. */
internal const val MAX_INLINE_BANNER_HEIGHT_DP_PUBLIC = MAX_INLINE_BANNER_HEIGHT_DP

internal fun bannerAdWidthDp(parentContentWidthDp: Float): Int? {
    val widthDp = parentContentWidthDp.toInt()
    return widthDp.takeIf { it >= MIN_BANNER_WIDTH_DP }
}

internal fun bannerAdMaxHeightDp(): Int = MAX_INLINE_BANNER_HEIGHT_DP

/**
 * AdBanner 컨테이너 높이(dp)를 결정한다.
 *
 * 인라인 어댑티브 배너는 로드 전 `adSize.height`(estimatedHeightDp)가 0/음수일 수 있어,
 * 컨테이너를 그 값으로 고정하면 광고가 수신돼도 0높이/클립으로 안 보인다(배너 미표시 버그).
 *
 * - 로드 후([loadedHeightDp] != null): **SDK가 정한 실제 높이를 clamp 없이 그대로** 사용.
 *   ★ 임의 상한으로 자르면 광고가 잘려 AdMob "광고 프레임 크기 변경" 정책 위반.
 *   iOS `AdBannerView.swift` 도 `bannerView.adSize.size` 를 clamp 없이 그대로 쓴다.
 *   측정 0이면 [FALLBACK_BANNER_HEIGHT_DP] 로 공간 유지.
 * - 로드 전: 실제 크기를 모르므로 추정 높이로 공간만 예약하되, 과도한 점프 방지를 위해
 *   [MAX_INLINE_BANNER_HEIGHT_DP] 상한으로만 예약 (로드되면 실제 높이로 교체됨).
 */
internal fun resolveBannerHeightDp(estimatedHeightDp: Int, loadedHeightDp: Int?): Int {
    if (loadedHeightDp != null) {
        // 로드 완료: 실제 높이를 그대로 (clamp 금지 — 짤림/정책위반 방지). 0이면 fallback.
        return if (loadedHeightDp > 0) loadedHeightDp else FALLBACK_BANNER_HEIGHT_DP
    }
    // 로드 전: 추정 높이로 공간 예약 (상한 maxHeight, 0/음수면 fallback).
    val reserved = if (estimatedHeightDp > 0) estimatedHeightDp else FALLBACK_BANNER_HEIGHT_DP
    return reserved.coerceAtMost(MAX_INLINE_BANNER_HEIGHT_DP)
}
