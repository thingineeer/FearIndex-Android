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
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetKospiFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetKospiFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesDetailUseCase
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

data class HomeUiState(
    val selectedHomeType: FearIndexType = FearIndexType.MARKET,
    val selectedChartType: FearIndexType = FearIndexType.MARKET,
    val selectedVoteType: FearIndexType = FearIndexType.MARKET,
    val marketState: FearIndexState = FearIndexState.Loading,
    val kospiState: FearIndexState = FearIndexState.Loading,
    val cryptoState: FearIndexState = FearIndexState.Loading,
    val kospiSnapshot: KospiFearIndex? = null,
    val marketHistory: List<FearIndex> = emptyList(),
    val kospiHistory: List<FearIndex> = emptyList(),
    val cryptoHistory: List<FearIndex> = emptyList(),
    val marketHistoryDays: Int = DEFAULT_MARKET_DAYS,
    val kospiHistoryDays: Int = DEFAULT_KOSPI_DAYS,
    val cryptoHistoryDays: Int = DEFAULT_CRYPTO_DAYS,
    val isMarketHistoryLoading: Boolean = false,
    val isKospiHistoryLoading: Boolean = false,
    val isCryptoHistoryLoading: Boolean = false,
    val marketIndices: List<MarketIndex> = emptyList(),
) {
    companion object {
        const val DEFAULT_MARKET_DAYS = 90
        const val DEFAULT_KOSPI_DAYS = 90
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
    private val getKospiFearIndex: GetKospiFearIndexUseCase,
    private val getKospiFearIndexHistory: GetKospiFearIndexHistoryUseCase,
    private val getMarketIndices: GetMarketIndicesDetailUseCase,
    private val analytics: AnalyticsManager,
) : ViewModel() {
    companion object {
        const val NETWORK_ERROR_MESSAGE =
            "Market data is temporarily unavailable. Please check your connection and try again."
        const val GENERIC_ERROR_MESSAGE = "Market data is temporarily unavailable. Please try again."
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var marketHistoryJob: Job? = null
    private var kospiHistoryJob: Job? = null
    private var cryptoHistoryJob: Job? = null

    init {
        loadMarketCurrent()
        loadKospiCurrent()
        loadCryptoCurrent()
        loadMarketHistory(HomeUiState.DEFAULT_MARKET_DAYS)
        loadKospiHistory(HomeUiState.DEFAULT_KOSPI_DAYS)
        loadCryptoHistory(HomeUiState.DEFAULT_CRYPTO_DAYS)
        loadMarketIndices()
    }

    fun selectHomeIndexType(type: FearIndexType) =
        selectIndexType(type, screen = "홈", previous = _uiState.value.selectedHomeType) {
            _uiState.value = _uiState.value.copy(selectedHomeType = type)
        }

    fun selectChartIndexType(type: FearIndexType) =
        selectIndexType(type, screen = "차트", previous = _uiState.value.selectedChartType) {
            _uiState.value = _uiState.value.copy(selectedChartType = type)
        }

    fun selectVoteIndexType(type: FearIndexType) =
        selectIndexType(type, screen = "투표", previous = _uiState.value.selectedVoteType) {
            _uiState.value = _uiState.value.copy(selectedVoteType = type)
        }

    fun refresh() {
        val selectedType = _uiState.value.selectedHomeType
        analytics.log(AnalyticsEvent.수동새로고침(화면 = selectedType.toLabel()))
        when (selectedType) {
            FearIndexType.MARKET -> {
                loadMarketCurrent(forceRefresh = true)
                loadMarketHistory(_uiState.value.marketHistoryDays, forceRefresh = true)
            }
            FearIndexType.KOSPI -> {
                loadKospiCurrent(forceRefresh = true)
                loadKospiHistory(_uiState.value.kospiHistoryDays, forceRefresh = true)
            }
            FearIndexType.CRYPTO -> {
                loadCryptoCurrent(forceRefresh = true)
                loadCryptoHistory(_uiState.value.cryptoHistoryDays, forceRefresh = true)
            }
        }
    }

    private fun selectIndexType(
        type: FearIndexType,
        screen: String,
        previous: FearIndexType,
        applySelection: () -> Unit,
    ) {
        applySelection()
        if (previous != type) {
            analytics.log(
                AnalyticsEvent.지수타입전환(
                    타입 = type.toLabel(),
                    화면 = screen,
                    이전타입 = previous.toLabel(),
                ),
            )
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

    fun loadKospiHistoryForDays(days: Int) {
        if (_uiState.value.kospiHistoryDays == days &&
            _uiState.value.kospiHistory.isNotEmpty() &&
            !_uiState.value.isKospiHistoryLoading
        ) return
        loadKospiHistory(days)
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
                    marketState = FearIndexState.Error(NETWORK_ERROR_MESSAGE),
                )
                analytics.log(AnalyticsEvent.네트워크에러(에러메시지 = e.message ?: "Unknown"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    marketState = FearIndexState.Error(GENERIC_ERROR_MESSAGE),
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

    private fun loadKospiCurrent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(kospiState = FearIndexState.Loading)
            try {
                val snapshot = getKospiFearIndex(forceRefresh)
                val index = snapshot.fearIndex
                _uiState.value = _uiState.value.copy(
                    kospiState = FearIndexState.Loaded(index),
                    kospiSnapshot = snapshot,
                )
                analytics.log(
                    AnalyticsEvent.공포지수조회(
                        현재점수 = index.roundedScore,
                        등급 = index.rating.name,
                    ),
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    kospiState = FearIndexState.Error(NETWORK_ERROR_MESSAGE),
                    kospiSnapshot = null,
                )
                analytics.log(AnalyticsEvent.네트워크에러(에러메시지 = e.message ?: "Unknown"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    kospiState = FearIndexState.Error(GENERIC_ERROR_MESSAGE),
                    kospiSnapshot = null,
                )
                analytics.log(
                    AnalyticsEvent.API에러(
                        에러유형 = "코스피공포지수",
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
                    cryptoState = FearIndexState.Error(NETWORK_ERROR_MESSAGE),
                )
                analytics.log(AnalyticsEvent.네트워크에러(에러메시지 = e.message ?: "Unknown"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cryptoState = FearIndexState.Error(GENERIC_ERROR_MESSAGE),
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

    private fun loadKospiHistory(days: Int, forceRefresh: Boolean = false) {
        kospiHistoryJob?.cancel()
        kospiHistoryJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                kospiHistoryDays = days,
                isKospiHistoryLoading = true,
            )
            try {
                val history = getKospiFearIndexHistory(days, forceRefresh)
                _uiState.value = _uiState.value.copy(
                    kospiHistory = history,
                    isKospiHistoryLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isKospiHistoryLoading = false,
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
