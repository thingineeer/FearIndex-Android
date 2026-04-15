package th1ngjin.fearindex.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
) {
    companion object {
        const val DEFAULT_MARKET_DAYS = 90
        const val DEFAULT_CRYPTO_DAYS = 30
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
    }

    fun selectIndexType(type: FearIndexType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun refresh() {
        when (_uiState.value.selectedType) {
            FearIndexType.MARKET -> {
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

    private fun loadMarketCurrent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(marketState = FearIndexState.Loading)
            try {
                val index = getFearIndex(forceRefresh)
                _uiState.value = _uiState.value.copy(
                    marketState = FearIndexState.Loaded(index),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    marketState = FearIndexState.Error(e.message ?: "Unknown error"),
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cryptoState = FearIndexState.Error(e.message ?: "Unknown error"),
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
