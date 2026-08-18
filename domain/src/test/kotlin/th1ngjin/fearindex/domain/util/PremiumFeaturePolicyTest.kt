package th1ngjin.fearindex.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.PremiumFeature

/** iOS `PremiumFeaturePolicyTests` 1:1 (v1.9.4). */
class PremiumFeaturePolicyTest {

    @Test
    fun `프리미엄이면 모든 기능 사용 가능`() {
        for (feature in PremiumFeature.entries) {
            assertTrue("$feature 는 프리미엄에서 열려야 함", PremiumFeaturePolicy.canUse(feature, isPremium = true))
        }
    }

    @Test
    fun `무료면 모든 프리미엄 기능 잠금`() {
        for (feature in PremiumFeature.entries) {
            assertFalse("$feature 는 무료에서 잠겨야 함", PremiumFeaturePolicy.canUse(feature, isPremium = false))
        }
    }

    @Test
    fun `v1_9_4 프리미엄 기능은 점수 탐색기 + 알림 내역 무제한 2종`() {
        assertEquals(2, PremiumFeature.entries.size)
        assertTrue(PremiumFeature.entries.contains(PremiumFeature.SCORE_EXPLORER))
        assertTrue(PremiumFeature.entries.contains(PremiumFeature.NOTIFICATION_HISTORY_UNLIMITED))
    }

    @Test
    fun `analyticsKey 는 iOS rawValue 와 동일한 snake_case`() {
        assertEquals("score_explorer", PremiumFeature.SCORE_EXPLORER.analyticsKey)
        assertEquals("notification_history_unlimited", PremiumFeature.NOTIFICATION_HISTORY_UNLIMITED.analyticsKey)
    }
}
