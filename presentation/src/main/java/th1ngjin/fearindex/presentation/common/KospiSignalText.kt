package th1ngjin.fearindex.presentation.common

import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiConfidence
import th1ngjin.fearindex.presentation.R

/**
 * 서버 KOSPI 신호 이름/클러스터/신뢰도 → Android 리소스 ID 매핑.
 * iOS `KospiPresentationText` 대칭 — 알 수 없는 신호는 unknown 라벨로 폴백.
 */
object KospiSignalText {
    fun signalNameResId(name: String): Int =
        signalKeyMap[name] ?: R.string.kospi_signal_unknown

    fun clusterNameResId(cluster: KospiCluster): Int = when (cluster) {
        KospiCluster.PRICE -> R.string.kospi_cluster_price
        KospiCluster.BREADTH -> R.string.kospi_cluster_breadth
        KospiCluster.SENTIMENT -> R.string.kospi_cluster_sentiment
        KospiCluster.CREDIT -> R.string.kospi_cluster_credit
        KospiCluster.UNKNOWN -> R.string.kospi_cluster_unknown
    }

    fun confidenceResId(confidence: KospiConfidence): Int = when (confidence) {
        KospiConfidence.HIGH -> R.string.kospi_method_confidence_high
        KospiConfidence.MEDIUM -> R.string.kospi_method_confidence_medium
        KospiConfidence.LOW -> R.string.kospi_method_confidence_low
        KospiConfidence.UNKNOWN -> R.string.kospi_method_unavailable
    }

    private val signalKeyMap: Map<String, Int> = mapOf(
        "kospiMomentum" to R.string.kospi_signal_kospi_momentum,
        "priceStrength" to R.string.kospi_signal_price_strength,
        "priceBreadth" to R.string.kospi_signal_price_breadth,
        "marketVolatility" to R.string.kospi_signal_market_volatility,
        "junkBondSpread" to R.string.kospi_signal_junk_bond_spread,
        "safeHaven" to R.string.kospi_signal_safe_haven,
        "foreignerFlow" to R.string.kospi_signal_foreigner_flow,
        "marginBalance" to R.string.kospi_signal_margin_balance,
    )
}
