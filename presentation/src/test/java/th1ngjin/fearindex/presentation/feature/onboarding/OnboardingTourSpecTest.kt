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

    // ── 카드 y 위치: 하이라이트에 밀착 (사용자 요청 2026-08-27) ──────────────

    @Test
    fun `앵커 아래 배치면 카드 상단이 앵커 바로 아래에 붙는다`() {
        val anchor = Rect(0f, 300f, 1080f, 900f)
        val top = onboardingCardTopPx(
            placement = OnboardingCardPlacement.BOTTOM,
            anchor = anchor,
            cardHeightPx = 700f,
            screenHeightPx = 2400f,
            gapPx = 24f,
            minTopPx = 100f,
            maxBottomPx = 2200f,
        )
        assertEquals(924f, top, 0.01f)
    }

    @Test
    fun `앵커 위 배치면 카드 하단이 앵커 바로 위에 붙는다`() {
        val anchor = Rect(0f, 1800f, 1080f, 2000f)
        val top = onboardingCardTopPx(
            placement = OnboardingCardPlacement.TOP,
            anchor = anchor,
            cardHeightPx = 700f,
            screenHeightPx = 2400f,
            gapPx = 24f,
            minTopPx = 100f,
            maxBottomPx = 2200f,
        )
        assertEquals(1076f, top, 0.01f) // 1800 - 24 - 700
    }

    @Test
    fun `아래 공간이 부족하면 하단 한계 안으로 당겨진다`() {
        val anchor = Rect(0f, 300f, 1080f, 1900f) // 아래 붙이면 1924 + 700 = 2624 > 2200
        val top = onboardingCardTopPx(
            placement = OnboardingCardPlacement.BOTTOM,
            anchor = anchor,
            cardHeightPx = 700f,
            screenHeightPx = 2400f,
            gapPx = 24f,
            minTopPx = 100f,
            maxBottomPx = 2200f,
        )
        assertEquals(1500f, top, 0.01f) // 2200 - 700
    }

    @Test
    fun `위 공간이 부족하면 상단 한계 아래로 밀린다`() {
        val anchor = Rect(0f, 400f, 1080f, 700f) // 위에 붙이면 400 - 24 - 700 = -324
        val top = onboardingCardTopPx(
            placement = OnboardingCardPlacement.TOP,
            anchor = anchor,
            cardHeightPx = 700f,
            screenHeightPx = 2400f,
            gapPx = 24f,
            minTopPx = 100f,
            maxBottomPx = 2200f,
        )
        assertEquals(100f, top, 0.01f)
    }

    @Test
    fun `앵커가 없으면 화면 중앙`() {
        val top = onboardingCardTopPx(
            placement = OnboardingCardPlacement.CENTER,
            anchor = null,
            cardHeightPx = 700f,
            screenHeightPx = 2400f,
            gapPx = 24f,
            minTopPx = 100f,
            maxBottomPx = 2200f,
        )
        assertEquals(850f, top, 0.01f)
    }

    @Test
    fun `카드가 가용 높이보다 크면 상단 한계에 고정`() {
        val anchor = Rect(0f, 300f, 1080f, 900f)
        val top = onboardingCardTopPx(
            placement = OnboardingCardPlacement.BOTTOM,
            anchor = anchor,
            cardHeightPx = 2400f,
            screenHeightPx = 2400f,
            gapPx = 24f,
            minTopPx = 100f,
            maxBottomPx = 2200f,
        )
        assertEquals(100f, top, 0.01f)
    }

    // ── 5단계 투표는 하이라이트 안쪽을 직접 누를 수 있어야 한다 ──────────────

    @Test
    fun `투표 단계만 하이라이트 상호작용 허용`() {
        val steps = OnboardingSteps.ALL
        assertTrue(steps[4].interactiveAnchor)
        assertEquals(
            listOf(false, false, false, false, true, false, false, false),
            steps.map { it.interactiveAnchor },
        )
    }
}
