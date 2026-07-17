package th1ngjin.fearindex.presentation.feature.onboarding

import androidx.compose.ui.geometry.Rect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.domain.service.OnboardingStore

class OnboardingTourViewModelTest {

    private fun vm(
        eligible: Boolean = true,
        seen: Boolean = false,
        analytics: AnalyticsManager = mockk(relaxed = true),
        store: OnboardingStore = mockk(relaxed = true),
    ): Pair<OnboardingTourViewModel, Pair<OnboardingStore, AnalyticsManager>> {
        every { store.isTourEligible() } returns eligible
        every { store.hasSeenTour() } returns seen
        return OnboardingTourViewModel(store, analytics) to (store to analytics)
    }

    @Test
    fun `신규 설치 자격 있고 미노출이면 투어 시작 + hasSeenTour 기록`() {
        val (m, deps) = vm(eligible = true, seen = false)
        m.startIfEligible()
        assertTrue(m.uiState.isActive)
        assertEquals(1, m.activeStepNumber)
        verify { deps.first.markTourSeen() }
    }

    @Test
    fun `자격 없으면 시작 안 함`() {
        val (m, _) = vm(eligible = false, seen = false)
        m.startIfEligible()
        assertFalse(m.uiState.isActive)
    }

    @Test
    fun `이미 본 유저면 시작 안 함`() {
        val (m, _) = vm(eligible = true, seen = true)
        m.startIfEligible()
        assertFalse(m.uiState.isActive)
    }

    @Test
    fun `force면 자격 무관 강제 시작 + 시작 단계 지정`() {
        val (m, _) = vm(eligible = false, seen = true)
        m.startIfEligible(force = true, startStep = 2)
        assertTrue(m.uiState.isActive)
        assertEquals(2, m.activeStepNumber)
    }

    @Test
    fun `restart는 자격 무관 1단계부터`() {
        val (m, _) = vm(eligible = false, seen = true)
        m.restart()
        assertTrue(m.uiState.isActive)
        assertEquals(1, m.activeStepNumber)
    }

    @Test
    fun `advance는 마지막 전까지 단계 증가`() {
        val (m, _) = vm()
        m.startIfEligible()
        m.advance()
        assertEquals(2, m.activeStepNumber)
    }

    @Test
    fun `마지막 단계에서 advance하면 완료 + onboarding_done(8) 로깅 + 종료`() {
        val analytics = mockk<AnalyticsManager>(relaxed = true)
        val (m, _) = vm(analytics = analytics)
        m.startIfEligible(force = true, startStep = 8)
        m.advance()
        assertFalse(m.uiState.isActive)
        verify { analytics.log(AnalyticsEvent.온보딩완료(8)) }
    }

    @Test
    fun `skip하면 도달 단계로 onboarding_skip 로깅 + 종료`() {
        val analytics = mockk<AnalyticsManager>(relaxed = true)
        val (m, _) = vm(analytics = analytics)
        m.startIfEligible(force = true, startStep = 3)
        m.skip()
        assertFalse(m.uiState.isActive)
        verify { analytics.log(AnalyticsEvent.온보딩건너뛰기(3)) }
    }

    @Test
    fun `registerAnchor 추가_갱신_제거`() {
        val (m, _) = vm()
        m.startIfEligible()
        val r = Rect(0f, 0f, 10f, 10f)
        m.registerAnchor(1, r)
        assertEquals(r, m.uiState.anchors[1])
        m.registerAnchor(1, null)
        assertFalse(m.uiState.anchors.containsKey(1))
    }
}
