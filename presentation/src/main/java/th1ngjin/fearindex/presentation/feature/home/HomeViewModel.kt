package th1ngjin.fearindex.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesUseCase
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

data class HomeUiState(
    val selectedType: FearIndexType = FearIndexType.MARKET,
    val marketState: FearIndexState = FearIndexState.Loading,
    val cryptoState: FearIndexState = FearIndexState.Loading,
    val marketHistory: List<FearIndex> = emptyList(),
    val cryptoHistory: List<FearIndex> = emptyList(),
    val marketHistoryDays: Int = DEFAULT_MARKET_DAYS,
    val cryptoHistoryDays: Int = DEFAULT_CRYPTO_DAYS,
    val isMarketHistoryLoading: Boolean = false,
    val isCryptoHistoryLoading: Boolean = false,
    val marketIndices: List<MarketIndex> = emptyList(),
) {
    companion object {
        const val DEFAULT_MARKET_DAYS = 90
        const val DEFAULT_CRYPTO_DAYS = 90
    }
}

sealed interface FearIndexState {
    data object Loading : FearIndexState
    data class Loaded(val fearIndex: FearIndex) : FearIndexState
    data class Error(val message: String) : FearIndexState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFearIndex: GetFearIndexUseCase,
    private val getFearIndexHistory: GetFearIndexHistoryUseCase,
    private val getCryptoFearIndex: GetCryptoFearIndexUseCase,
    private val getCryptoFearIndexHistory: GetCryptoFearIndexHistoryUseCase,
    private val getMarketIndices: GetMarketIndicesUseCase,
    private val analytics: AnalyticsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var marketHistoryJob: Job? = null
    private var cryptoHistoryJob: Job? = null

    init {
        loadMarketCurrent()
        loadCryptoCurrent()
        loadMarketHistory(HomeUiState.DEFAULT_MARKET_DAYS)
        loadCryptoHistory(HomeUiState.DEFAULT_CRYPTO_DAYS)
        loadMarketIndices()
    }

    fun selectIndexType(type: FearIndexType) {
        val previous = _uiState.value.selectedType
        _uiState.value = _uiState.value.copy(selectedType = type)
        if (previous != type) {
            analytics.log(
                AnalyticsEvent.지수타입전환(
                    타입 = type.toLabel(),
                    화면 = "홈",
                    이전타입 = previous.toLabel(),
                ),
            )
        }
    }

    fun refresh() {
        val selectedType = _uiState.value.selectedType
        analytics.log(AnalyticsEvent.수동새로고침(화면 = selectedType.toLabel()))
        when (selectedType) {
            FearIndexType.MARKET -> {
                loadMarketCurrent(forceRefresh = true)
                loadMarketHistory(_uiState.value.marketHistoryDays, forceRefresh = true)
            }
            FearIndexType.KOSPI -> {
                loadMarketCurrent(forceRefresh = true)
                loadMarketHistory(_uiState.value.marketHistoryDays, forceRefresh = true)
            }
            FearIndexType.CRYPTO -> {
                loadCryptoCurrent(forceRefresh = true)
                loadCryptoHistory(_uiState.value.cryptoHistoryDays, forceRefresh = true)
            }
        }
    }

    /**
     * 차트 기간 변경 시 호출. 같은 days로 중복 호출 방지.
     */
    fun loadMarketHistoryForDays(days: Int) {
        if (_uiState.value.marketHistoryDays == days &&
            _uiState.value.marketHistory.isNotEmpty() &&
            !_uiState.value.isMarketHistoryLoading
        ) return
        loadMarketHistory(days)
    }

    fun loadCryptoHistoryForDays(days: Int) {
        if (_uiState.value.cryptoHistoryDays == days &&
            _uiState.value.cryptoHistory.isNotEmpty() &&
            !_uiState.value.isCryptoHistoryLoading
        ) return
        loadCryptoHistory(days)
    }

    private fun loadMarketIndices() {
        viewModelScope.launch {
            try {
                val indices = getMarketIndices()
                _uiState.value = _uiState.value.copy(marketIndices = indices)
            } catch (e: Exception) {
                // 시장 지수 로딩 실패 시 빈 리스트 유지 (전체 화면 깨트리지 않음)
                Timber.w(e, "Failed to load market indices")
            }
        }
    }

    private fun loadMarketCurrent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(marketState = FearIndexState.Loading)
            try {
                val index = getFearIndex(forceRefresh)
                _uiState.value = _uiState.value.copy(
                    marketState = FearIndexState.Loaded(index),
                )
                analytics.log(
                    AnalyticsEvent.공포지수조회(
                        현재점수 = index.roundedScore,
                        등급 = index.rating.name,
                    ),
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    marketState = FearIndexState.Error(e.message ?: "Network error"),
                )
                analytics.log(AnalyticsEvent.네트워크에러(에러메시지 = e.message ?: "Unknown"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    marketState = FearIndexState.Error(e.message ?: "Unknown error"),
                )
                analytics.log(
                    AnalyticsEvent.API에러(
                        에러유형 = "시장공포지수",
                        에러메시지 = e.message ?: "Unknown",
                    ),
                )
            }
        }
    }

    private fun loadCryptoCurrent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cryptoState = FearIndexState.Loading)
            try {
                val index = getCryptoFearIndex(forceRefresh)
                _uiState.value = _uiState.value.copy(
                    cryptoState = FearIndexState.Loaded(index),
                )
                analytics.log(
                    AnalyticsEvent.암호화폐공포지수조회(
                        현재점수 = index.roundedScore,
                        등급 = index.rating.name,
                    ),
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    cryptoState = FearIndexState.Error(e.message ?: "Network error"),
                )
                analytics.log(AnalyticsEvent.네트워크에러(에러메시지 = e.message ?: "Unknown"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cryptoState = FearIndexState.Error(e.message ?: "Unknown error"),
                )
                analytics.log(
                    AnalyticsEvent.API에러(
                        에러유형 = "암호화폐공포지수",
                        에러메시지 = e.message ?: "Unknown",
                    ),
                )
            }
        }
    }

    private fun loadMarketHistory(days: Int, forceRefresh: Boolean = false) {
        marketHistoryJob?.cancel()
        marketHistoryJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                marketHistoryDays = days,
                isMarketHistoryLoading = true,
            )
            try {
                val history = getFearIndexHistory(days, forceRefresh)
                _uiState.value = _uiState.value.copy(
                    marketHistory = history,
                    isMarketHistoryLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isMarketHistoryLoading = false,
                )
            }
        }
    }

    private fun loadCryptoHistory(days: Int, forceRefresh: Boolean = false) {
        cryptoHistoryJob?.cancel()
        cryptoHistoryJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                cryptoHistoryDays = days,
                isCryptoHistoryLoading = true,
            )
            try {
                val history = getCryptoFearIndexHistory(days, forceRefresh)
                _uiState.value = _uiState.value.copy(
                    cryptoHistory = history,
                    isCryptoHistoryLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCryptoHistoryLoading = false,
                )
            }
        }
    }
}

private fun FearIndexType.toLabel(): String = when (this) {
    FearIndexType.MARKET -> "시장"
    FearIndexType.KOSPI -> "코스피"
    FearIndexType.CRYPTO -> "암호화폐"
}
