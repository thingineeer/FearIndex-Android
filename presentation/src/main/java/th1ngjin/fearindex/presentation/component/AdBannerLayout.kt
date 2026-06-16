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
 * - 로드 전: 추정 높이가 유효하면 그대로, 0/음수면 [FALLBACK_BANNER_HEIGHT_DP] 로 공간 확보.
 * - 로드 후([loadedHeightDp] != null): 실제 측정 높이를 우선 사용. 0이면 fallback 유지.
 * - 결과는 항상 [MAX_INLINE_BANNER_HEIGHT_DP] 이하로 클램프.
 */
internal fun resolveBannerHeightDp(estimatedHeightDp: Int, loadedHeightDp: Int?): Int {
    val base = when {
        loadedHeightDp != null && loadedHeightDp > 0 -> loadedHeightDp
        loadedHeightDp != null -> FALLBACK_BANNER_HEIGHT_DP // 로드됐으나 측정 0 → 공간 유지
        estimatedHeightDp > 0 -> estimatedHeightDp
        else -> FALLBACK_BANNER_HEIGHT_DP
    }
    return base.coerceAtMost(MAX_INLINE_BANNER_HEIGHT_DP)
}
