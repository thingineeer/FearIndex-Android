package th1ngjin.fearindex.presentation.feature.onboarding

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.domain.service.OnboardingStore
import javax.inject.Inject

/** 투어 오버레이/스크롤/앵커 수집이 공유하는 상태. */
data class OnboardingTourUiState(
    val isActive: Boolean = false,
    val index: Int = 0, // 0-based
    val anchors: Map<Int, Rect> = emptyMap(),
)

/**
 * 하위 화면(홈/투표/설정)이 앵커 등록·활성 상태 조회에 쓰는 핸들.
 * CompositionLocal 로 주입 (null = 투어 시스템 없음, 프리뷰 등).
 */
@Stable
interface OnboardingTourHandle {
    val isActive: Boolean
    val activeStepNumber: Int // 1-based, 0 = 비활성
    val activeAnchorId: Int?
    fun registerAnchor(id: Int, rect: Rect?)

    /** 설정 "앱 사용법" 행에서 1단계부터 재실행 (자격 무관). */
    fun restart()
}

val LocalOnboardingTour = staticCompositionLocalOf<OnboardingTourHandle?> { null }

/** 대상 뷰를 온보딩 투어 [id] 단계의 하이라이트 앵커로 등록. */
fun Modifier.tourAnchor(handle: OnboardingTourHandle?, id: Int): Modifier {
    if (handle == null) return this
    return this.onGloballyPositioned { handle.registerAnchor(id, it.boundsInWindow()) }
}

@HiltViewModel
class OnboardingTourViewModel @Inject constructor(
    private val store: OnboardingStore,
    private val analytics: AnalyticsManager,
) : ViewModel(), OnboardingTourHandle {

    var uiState by mutableStateOf(OnboardingTourUiState())
        private set

    private val steps = OnboardingSteps.ALL

    override val isActive: Boolean get() = uiState.isActive
    override val activeStepNumber: Int get() = if (uiState.isActive) uiState.index + 1 else 0
    override val activeAnchorId: Int?
        get() = if (uiState.isActive) steps[uiState.index].anchorId else null

    val currentStep: OnboardingStep? get() = if (uiState.isActive) steps.getOrNull(uiState.index) else null
    val totalSteps: Int get() = steps.size

    /**
     * 신규 설치 첫 실행 자동 노출 (전 지원 로케일). 뜬 순간 hasSeenTour 기록 → 재노출 없음.
     * @param force QA/스크린샷용 강제 표시 (자격 무관)
     * @param startStep 시작 단계 (1-based, QA)
     */
    fun startIfEligible(force: Boolean = false, startStep: Int = 1) {
        if (uiState.isActive) return
        val eligible = force || (store.isTourEligible() && !store.hasSeenTour())
        if (!eligible) return
        store.markTourSeen()
        uiState = uiState.copy(isActive = true, index = (startStep - 1).coerceIn(0, steps.lastIndex))
    }

    /** 설정 "앱 사용법" 행에서 1단계부터 재실행 (자격 무관, 기존/신규 공통). */
    override fun restart() {
        if (uiState.isActive) return
        uiState = uiState.copy(isActive = true, index = 0)
    }

    override fun registerAnchor(id: Int, rect: Rect?) {
        val current = uiState.anchors
        if (rect == null) {
            if (!current.containsKey(id)) return
            uiState = uiState.copy(anchors = current - id)
        } else {
            if (current[id] == rect) return
            uiState = uiState.copy(anchors = current + (id to rect))
        }
    }

    /** [다음]/[시작하기]. */
    fun advance() {
        if (!uiState.isActive) return
        if (uiState.index >= steps.lastIndex) {
            finish(completed = true)
        } else {
            uiState = uiState.copy(index = uiState.index + 1)
        }
    }

    /** [건너뛰기]. */
    fun skip() {
        if (!uiState.isActive) return
        finish(completed = false)
    }

    private fun finish(completed: Boolean) {
        val reachedStep = uiState.index + 1
        uiState = uiState.copy(isActive = false)
        if (completed) {
            analytics.log(AnalyticsEvent.온보딩완료(reachedStep))
        } else {
            analytics.log(AnalyticsEvent.온보딩건너뛰기(reachedStep))
        }
    }
}
