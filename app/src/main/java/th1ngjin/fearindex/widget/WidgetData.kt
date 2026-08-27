package th1ngjin.fearindex.widget

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.presentation.R

/** 위젯 렌더에 필요한 최소 데이터. null 이면 로드 실패로 placeholder 를 표시한다. */
data class WidgetIndexData(
    val score: Int,
    val rating: FearIndex.Rating,
    /** 전일 대비 변화 (previousClose 없으면 null → 표시 생략) */
    val dailyChange: Int? = null,
)

private fun entryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java,
    )

/**
 * 지정한 지수를 백그라운드(IO)에서 가져온다. 네트워크/파싱 실패는 runCatching 으로 흡수해
 * 하나의 지수 실패가 대시보드 전체를 깨지 않도록 한다.
 */
suspend fun loadWidgetIndex(context: Context, type: FearIndexType): WidgetIndexData? =
    withContext(Dispatchers.IO) {
        runCatching {
            val ep = entryPoint(context)
            val fearIndex: FearIndex = when (type) {
                FearIndexType.MARKET -> ep.getFearIndex().invoke()
                FearIndexType.KOSPI -> ep.getKospiFearIndex().invoke().fearIndex
                FearIndexType.CRYPTO -> ep.getCryptoFearIndex().invoke()
            }
            WidgetIndexData(
                score = fearIndex.roundedScore,
                rating = fearIndex.rating,
                dailyChange = WidgetGaugeSpec.dailyChange(fearIndex.roundedScore, fearIndex.previousClose),
            )
        }.getOrNull()
    }

/** 지수 이름 라벨 (presentation 리소스 사용). */
fun indexLabel(context: Context, type: FearIndexType): String {
    val resId = when (type) {
        FearIndexType.MARKET -> R.string.widget_index_market
        FearIndexType.KOSPI -> R.string.widget_index_kospi
        FearIndexType.CRYPTO -> R.string.widget_index_crypto
    }
    return context.getString(resId)
}

/** Rating enum → 로케일라이즈된 등급 문자열. */
fun ratingLabel(context: Context, rating: FearIndex.Rating): String {
    val resId = when (rating) {
        FearIndex.Rating.EXTREME_FEAR -> R.string.rating_extreme_fear
        FearIndex.Rating.FEAR -> R.string.rating_fear
        FearIndex.Rating.NEUTRAL -> R.string.rating_neutral
        FearIndex.Rating.GREED -> R.string.rating_greed
        FearIndex.Rating.EXTREME_GREED -> R.string.rating_extreme_greed
    }
    return context.getString(resId)
}
