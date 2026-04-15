package th1ngjin.fearindex.presentation.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.util.InsightGenerator
import th1ngjin.fearindex.domain.defaults.DefaultReturnData
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketInsight
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
 */
@HiltViewModel
class InsightViewModel @Inject constructor(
    private val analytics: AnalyticsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightUiState())
    val uiState: StateFlow<InsightUiState> = _uiState.asStateFlow()

    /**
     * HomeViewModel의 uiState를 관찰하여 인사이트를 생성.
     */
    fun observeHome(homeViewModel: HomeViewModel) {
        viewModelScope.launch {
            homeViewModel.uiState.collectLatest { homeState ->
                val indexType = homeState.selectedType
                val state = when (indexType) {
                    FearIndexType.MARKET -> homeState.marketState
                    FearIndexType.CRYPTO -> homeState.cryptoState
                }
                val history = when (indexType) {
                    FearIndexType.MARKET -> homeState.marketHistory
                    FearIndexType.CRYPTO -> homeState.cryptoHistory
                }

                if (state is FearIndexState.Loaded && history.isNotEmpty()) {
                    val score = state.fearIndex.roundedScore
                    val table = when (indexType) {
                        FearIndexType.MARKET -> DefaultReturnData.market
                        FearIndexType.CRYPTO -> DefaultReturnData.crypto
                    }
                    val insights = InsightGenerator.generateInsights(
                        score = score,
                        indexType = indexType,
                        history = history,
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
