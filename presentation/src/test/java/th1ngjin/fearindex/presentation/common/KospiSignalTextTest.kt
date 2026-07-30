package th1ngjin.fearindex.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiConfidence
import th1ngjin.fearindex.presentation.R

class KospiSignalTextTest {
    @Test
    fun `maps all eight known signal names to localized string resources`() {
        assertEquals(R.string.kospi_signal_kospi_momentum, KospiSignalText.signalNameResId("kospiMomentum"))
        assertEquals(R.string.kospi_signal_price_strength, KospiSignalText.signalNameResId("priceStrength"))
        assertEquals(R.string.kospi_signal_price_breadth, KospiSignalText.signalNameResId("priceBreadth"))
        assertEquals(R.string.kospi_signal_market_volatility, KospiSignalText.signalNameResId("marketVolatility"))
        assertEquals(R.string.kospi_signal_junk_bond_spread, KospiSignalText.signalNameResId("junkBondSpread"))
        assertEquals(R.string.kospi_signal_safe_haven, KospiSignalText.signalNameResId("safeHaven"))
        assertEquals(R.string.kospi_signal_foreigner_flow, KospiSignalText.signalNameResId("foreignerFlow"))
        assertEquals(R.string.kospi_signal_margin_balance, KospiSignalText.signalNameResId("marginBalance"))
    }

    @Test
    fun `falls back to unknown signal label for unrecognized names`() {
        assertEquals(R.string.kospi_signal_unknown, KospiSignalText.signalNameResId("brandNewSignal"))
        assertEquals(R.string.kospi_signal_unknown, KospiSignalText.signalNameResId(""))
    }

    @Test
    fun `maps clusters to localized string resources`() {
        assertEquals(R.string.kospi_cluster_price, KospiSignalText.clusterNameResId(KospiCluster.PRICE))
        assertEquals(R.string.kospi_cluster_breadth, KospiSignalText.clusterNameResId(KospiCluster.BREADTH))
        assertEquals(R.string.kospi_cluster_sentiment, KospiSignalText.clusterNameResId(KospiCluster.SENTIMENT))
        assertEquals(R.string.kospi_cluster_credit, KospiSignalText.clusterNameResId(KospiCluster.CREDIT))
        assertEquals(R.string.kospi_cluster_unknown, KospiSignalText.clusterNameResId(KospiCluster.UNKNOWN))
    }

    @Test
    fun `maps confidence to localized string resources with unavailable fallback`() {
        assertEquals(R.string.kospi_method_confidence_high, KospiSignalText.confidenceResId(KospiConfidence.HIGH))
        assertEquals(R.string.kospi_method_confidence_medium, KospiSignalText.confidenceResId(KospiConfidence.MEDIUM))
        assertEquals(R.string.kospi_method_confidence_low, KospiSignalText.confidenceResId(KospiConfidence.LOW))
        assertEquals(R.string.kospi_method_unavailable, KospiSignalText.confidenceResId(KospiConfidence.UNKNOWN))
    }
}
