package th1ngjin.fearindex.presentation.component

private const val MIN_BANNER_WIDTH_DP = 320

internal fun bannerAdWidthDp(parentContentWidthDp: Float): Int? {
    val widthDp = parentContentWidthDp.toInt()
    return widthDp.takeIf { it >= MIN_BANNER_WIDTH_DP }
}
