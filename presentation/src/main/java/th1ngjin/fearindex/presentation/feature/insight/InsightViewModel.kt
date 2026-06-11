package th1ngjin.fearindex.presentation.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.util.InsightGenerator
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.repository.ReturnDataRepository
import th1ngjin.fearindex.presentation.feature.home.FearIndexState
import th1ngjin.fearindex.presentation.feature.home.HomeViewModel
import javax.inject.Inject

data class InsightUiState(
    val insights: List<MarketInsight> = emptyList(),
    val selectedInsight: MarketInsight? = null,
    val isLoading: Boolean = true,
)

/**
 * 인사이트 ViewModel.
 *
 * HomeViewModel의 uiState를 관찰하여 인사이트 리스트를 생성.
 * HomeViewModel과 별도로 생성되며, observeHome()으로 연결.
 *
 * returnData는 `ReturnDataRepository`를 통해 Firestore → Default 순으로 로드.
 * (iOS `FetchReturnDataUseCase`와 동일 패턴.)
 */
@HiltViewModel
class InsightViewModel @Inject constructor(
    private val analytics: AnalyticsManager,
    private val returnDataRepository: ReturnDataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightUiState())
    val uiState: StateFlow<InsightUiState> = _uiState.asStateFlow()

    /**
     * HomeViewModel의 uiState를 관찰하여 인사이트를 생성.
     *
     * 성능: distinctUntilChanged로 인사이트 생성에 영향 없는 필드(티커/로딩 플래그 등)가
     * 바뀔 때는 재계산 스킵. 티커가 3초마다 갱신되어도 인사이트 보간/이벤트 매칭은
     * score/indexType/history 변경 시에만 실행.
     */
    fun observeHome(homeViewModel: HomeViewModel) {
        viewModelScope.launch {
            homeViewModel.uiState
                .map { homeState -> InsightInputSnapshot.from(homeState) }
                .distinctUntilChanged()
                .collectLatest { snapshot ->
                    val state = snapshot.state
                    if (state is FearIndexState.Loaded && snapshot.history.isNotEmpty()) {
                        val score = state.fearIndex.roundedScore
                        val table = fetchReturnData(snapshot.indexType)
                        val insights = InsightGenerator.generateInsights(
                            score = score,
                            indexType = snapshot.indexType,
                            history = snapshot.history,
                            returnDataTable = table,
                        )
                        _uiState.value = _uiState.value.copy(
                            insights = insights,
                            isLoading = false,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = state is FearIndexState.Loading,
                        )
                    }
                }
        }
    }

    /**
     * Repository를 경유해 returnData를 가져온다. Repository 내부에서 Firestore 실패 시
     * `DefaultReturnData`로 fallback 하므로 여기서는 추가 try-catch 불필요.
     */
    private suspend fun fetchReturnData(indexType: FearIndexType): ReturnDataTable =
        returnDataRepository.fetch(indexType)

    /**
     * 인사이트 생성에 영향을 주는 필드만 추린 스냅샷.
     * equals 비교가 HomeUiState 전체보다 훨씬 저렴 (score int + history size 참조).
     */
    private data class InsightInputSnapshot(
        val indexType: FearIndexType,
        val state: FearIndexState,
        val history: List<th1ngjin.fearindex.domain.entity.FearIndex>,
    ) {
        companion object {
            fun from(home: th1ngjin.fearindex.presentation.feature.home.HomeUiState): InsightInputSnapshot {
                val indexType = home.selectedType
                val state = when (indexType) {
                    FearIndexType.MARKET -> home.marketState
                    FearIndexType.KOSPI -> home.marketState
                    FearIndexType.CRYPTO -> home.cryptoState
                }
                val history = when (indexType) {
                    FearIndexType.MARKET -> home.marketHistory
                    FearIndexType.KOSPI -> home.marketHistory
                    FearIndexType.CRYPTO -> home.cryptoHistory
                }
                return InsightInputSnapshot(indexType, state, history)
            }
        }
    }

    fun selectInsight(insight: MarketInsight) {
        _uiState.value = _uiState.value.copy(selectedInsight = insight)
        analytics.log(
            AnalyticsEvent.인사이트상세조회(
                카드타입 = insight.type.name,
                점수 = insight.score,
            ),
        )
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedInsight = null)
    }

    fun logCardViewed(insight: MarketInsight) {
        analytics.log(
            AnalyticsEvent.인사이트카드노출(
                카드타입 = insight.type.name,
                지수타입 = insight.indexType.name,
            ),
        )
    }
}
