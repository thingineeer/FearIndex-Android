package th1ngjin.fearindex.presentation.feature.chart

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.analytics.PremiumPurchaseSource
import th1ngjin.fearindex.core.purchases.PurchaseEvent
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.domain.entity.DateRange
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.PremiumFeature
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.entity.ReturnHorizon
import th1ngjin.fearindex.domain.repository.ReturnDataRepository
import th1ngjin.fearindex.domain.util.PremiumFeaturePolicy
import th1ngjin.fearindex.domain.util.ScoreExplorerPoint
import th1ngjin.fearindex.domain.util.ScoreExplorerStats
import java.time.Instant
import javax.inject.Inject

/** 점수별 과거 수익률 카드 상태 (iOS `ScoreExplorerCardView.Model` + Interactor 파생값). */
data class ScoreExplorerUiState(
    val indexType: FearIndexType = FearIndexType.MARKET,
    /** 슬라이더 범위 (표본 n>0 최소..최대). null = 표시할 데이터 없음(미로드 포함). */
    val range: IntRange? = null,
    val selectedScore: Int = ScoreExplorerSelection.DEFAULT_SCORE,
    /** 슬라이더가 현재 점수 위치인가 (리셋 버튼 비활성). */
    val isAtCurrent: Boolean = true,
    /** 선택 점수의 정확 버킷. null = 그 점수를 기록한 날 없음. */
    val point: ScoreExplorerPoint? = null,
    val sourceRange: DateRange? = null,
    val updatedAt: Instant? = null,
    val isPremium: Boolean = false,
    val priceText: String? = null,
    /** 구매/복원 진행 중 (잠금 row CTA 스피너). */
    val isBusy: Boolean = false,
    /** 현재 자산 테이블 로드 중. */
    val isLoading: Boolean = true,
    val dialog: ScoreExplorerDialog? = null,
)

/** 카드 안 구매/복원 결과 다이얼로그. */
enum class ScoreExplorerDialog {
    PurchaseFailed,
    RestoreFailure,
}

/**
 * 점수별 과거 수익률(Score Explorer) ViewModel — 차트 탭 카드 전용 (v1.9.4 iOS parity).
 *
 * - 선택 로직은 순수 [ScoreExplorerSelection] 에 위임(테스트 대상), 여기선 테이블 로드/권한/결제 글루만.
 * - 자산별 [ReturnDataTable] 은 VM 안에서 캐시 (Repository 도 캐시하지만 재계산 없이 즉시 바인딩하기 위함).
 * - 프리미엄 여부 = [PurchaseManager.isAdFree] (광고 제거 IAP 가 곧 프리미엄).
 */
@HiltViewModel
class ScoreExplorerViewModel @Inject constructor(
    private val returnDataRepository: ReturnDataRepository,
    private val purchaseManager: PurchaseManager,
    private val analytics: AnalyticsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreExplorerUiState())
    val uiState: StateFlow<ScoreExplorerUiState> = _uiState.asStateFlow()

    private var selection = ScoreExplorerSelection()
    private val tables = mutableMapOf<FearIndexType, ReturnDataTable>()
    private val loading = mutableSetOf<FearIndexType>()
    private var currentScore: Int? = null

    init {
        purchaseManager.isAdFree
            .onEach { adFree -> _uiState.update { it.copy(isPremium = canUseExplorer(adFree), isBusy = it.isBusy && !adFree) } }
            .launchIn(viewModelScope)
        purchaseManager.priceText
            .onEach { price -> _uiState.update { it.copy(priceText = price) } }
            .launchIn(viewModelScope)
        purchaseManager.purchaseEvents
            .onEach(::onPurchaseEvent)
            .launchIn(viewModelScope)
    }

    /**
     * 차트 세그먼트/현재 점수가 바뀔 때마다 호출. 자산 전환 시 그 자산의 선택값(없으면 현재 점수)으로 이동하고,
     * 테이블이 없으면 로드 후 다시 바인딩한다.
     */
    fun bind(indexType: FearIndexType, currentScore: Int?) {
        this.currentScore = currentScore
        val table = tables[indexType]
        selection = selection.bind(indexType, currentScore, table?.let(ScoreExplorerStats::scoreRange))
        publish()
        if (table == null) loadTable(indexType)
    }

    /** 슬라이더 드래그 (매 tick). 범위 밖은 클램프. */
    fun move(score: Int) {
        selection = selection.move(score)
        publish()
    }

    /** 드래그 종료 시 1회 기록 (매 tick 아님) — iOS `logExplorerInteraction`. */
    fun moveEnded(horizon: ReturnHorizon) {
        analytics.log(
            AnalyticsEvent.점수탐색기조작(
                indexType = selection.indexType.name.lowercase(),
                score = selection.selectedScore,
                period = horizon.analyticsKey,
            ),
        )
    }

    /** "현재 점수로" 리셋. */
    fun reset() {
        selection = selection.reset()
        publish()
    }

    /** 잠금 row CTA — 잠금 탭 이벤트 + 구매 시트. 결과는 [PurchaseEvent] 로 돌아온다. */
    fun purchase(activity: Activity) {
        if (_uiState.value.isBusy) return
        analytics.log(AnalyticsEvent.프리미엄잠금탭(feature = PremiumFeature.SCORE_EXPLORER.analyticsKey))
        _uiState.update { it.copy(isBusy = true) }
        purchaseManager.purchaseRemoveAds(activity, PremiumPurchaseSource.SCORE_EXPLORER)
    }

    /** 잠금 row 2차 버튼 — 구매 복원. 성공 시 isPremium 이 흐름으로 갱신되어 카드가 즉시 열린다. */
    fun restore() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val restored = purchaseManager.restorePurchases(PremiumPurchaseSource.SCORE_EXPLORER)
            _uiState.update {
                it.copy(isBusy = false, dialog = if (restored) null else ScoreExplorerDialog.RestoreFailure)
            }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    private fun onPurchaseEvent(event: PurchaseEvent) {
        _uiState.update {
            it.copy(
                isBusy = false,
                dialog = if (event is PurchaseEvent.Failed) ScoreExplorerDialog.PurchaseFailed else it.dialog,
            )
        }
    }

    private fun loadTable(indexType: FearIndexType) {
        if (!loading.add(indexType)) return
        viewModelScope.launch {
            val table = returnDataRepository.fetch(indexType)
            tables[indexType] = table
            loading.remove(indexType)
            if (selection.indexType == indexType) bind(indexType, currentScore)
        }
    }

    private fun publish() {
        val type = selection.indexType
        val table = tables[type]
        _uiState.update {
            it.copy(
                indexType = type,
                range = selection.range,
                selectedScore = selection.selectedScore,
                isAtCurrent = selection.isAtCurrent,
                point = table?.let { t -> ScoreExplorerStats.point(selection.selectedScore, t) },
                sourceRange = table?.sourceRange,
                updatedAt = table?.updatedAt,
                isLoading = table == null,
            )
        }
    }

    private fun canUseExplorer(isPremium: Boolean): Boolean =
        PremiumFeaturePolicy.canUse(PremiumFeature.SCORE_EXPLORER, isPremium)
}
