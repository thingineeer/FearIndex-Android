package th1ngjin.fearindex.presentation.feature.onboarding

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** iOS OnboardingTourView.steps 미러 검증 — 순서/개수/목적지/앵커 고정. */
class OnboardingTourSpecTest {

    @Test
    fun `총 8단계`() {
        assertEquals(8, OnboardingSteps.COUNT)
    }

    @Test
    fun `1~3단계는 게이지 앵커 + market_kospi_crypto 세그먼트`() {
        val steps = OnboardingSteps.ALL
        assertEquals(OnboardingAnchor.GAUGE, steps[0].anchorId)
        assertEquals(OnboardingAnchor.GAUGE, steps[1].anchorId)
        assertEquals(OnboardingAnchor.GAUGE, steps[2].anchorId)
        assertEquals(OnboardingDestination.HOME_MARKET, steps[0].destination)
        assertEquals(OnboardingDestination.HOME_KOSPI, steps[1].destination)
        assertEquals(OnboardingDestination.HOME_CRYPTO, steps[2].destination)
    }

    @Test
    fun `4~7단계 목적지 매핑`() {
        val steps = OnboardingSteps.ALL
        assertEquals(OnboardingDestination.HOME_INSIGHT, steps[3].destination)
        assertEquals(OnboardingAnchor.INSIGHT, steps[3].anchorId)
        assertEquals(OnboardingDestination.VOTE, steps[4].destination)
        assertEquals(OnboardingAnchor.VOTE, steps[4].anchorId)
        assertEquals(OnboardingDestination.SETTINGS, steps[5].destination)
        assertEquals(OnboardingAnchor.NOTIFICATION, steps[5].anchorId)
        assertEquals(OnboardingDestination.SETTINGS_WIDGET, steps[6].destination)
        assertEquals(OnboardingAnchor.WIDGET, steps[6].anchorId)
    }

    @Test
    fun `8단계는 앵커 없이 심볼 카드 + 홈 market 유지`() {
        val last = OnboardingSteps.ALL[7]
        assertNull(last.anchorId)
        assertTrue(last.showSymbol)
        assertEquals(OnboardingDestination.HOME_MARKET, last.destination)
    }

    @Test
    fun `8단계 외에는 심볼 미표시`() {
        OnboardingSteps.ALL.dropLast(1).forEach { assertFalse(it.showSymbol) }
    }

    @Test
    fun `카드 배치 - 대상 없으면 중앙`() {
        assertEquals(OnboardingCardPlacement.CENTER, onboardingCardPlacement(null, 2000f))
    }

    @Test
    fun `카드 배치 - 대상이 하단 절반이면 카드는 위쪽`() {
        val anchor = Rect(0f, 1300f, 100f, 1700f) // center 1500
        assertEquals(OnboardingCardPlacement.TOP, onboardingCardPlacement(anchor, 2000f))
    }

    @Test
    fun `카드 배치 - 대상이 상단 절반이면 카드는 아래쪽`() {
        val anchor = Rect(0f, 200f, 100f, 600f) // center 400
        assertEquals(OnboardingCardPlacement.BOTTOM, onboardingCardPlacement(anchor, 2000f))
    }

    @Test
    fun `카드 배치 - 정확히 중앙선이면 아래쪽`() {
        val anchor = Rect(0f, 800f, 100f, 1200f) // center 1000
        assertEquals(OnboardingCardPlacement.BOTTOM, onboardingCardPlacement(anchor, 2000f))
    }

    @Test
    fun `카드 배치 - 대상이 화면 대부분을 덮으면(대형 카드) 중앙이 아래여도 탭바 쪽`() {
        // 4단계 인사이트 카드처럼 화면 높이 대부분을 덮는 앵커: center 1085(하단 절반)지만 BOTTOM
        val tall = Rect(0f, 450f, 100f, 1720f) // height 1270 = 63.5% of 2000
        assertEquals(OnboardingCardPlacement.BOTTOM, onboardingCardPlacement(tall, 2000f))
    }
}
