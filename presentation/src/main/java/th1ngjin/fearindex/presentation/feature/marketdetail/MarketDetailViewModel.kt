package th1ngjin.fearindex.presentation.feature.marketdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import th1ngjin.fearindex.domain.entity.CryptoPrice
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.usecase.GetCryptoPricesUseCase
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesDetailUseCase
import th1ngjin.fearindex.domain.usecase.GetUsdKrwRateUseCase
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

data class MarketDetailUiState(
    val indices: List<MarketIndex> = emptyList(),
    val cryptoPrices: List<CryptoPrice> = emptyList(),
    val usdKrwRate: ExchangeRateQuote? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** 마지막 수동 새로고침 시각 (epoch millis). 0 이면 없음. */
    val lastManualRefreshAtMillis: Long = 0L,
)

@HiltViewModel
class MarketDetailViewModel @Inject constructor(
    private val getIndices: GetMarketIndicesDetailUseCase,
    private val getCryptoPrices: GetCryptoPricesUseCase,
    private val getUsdKrwRate: GetUsdKrwRateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketDetailUiState())
    val uiState: StateFlow<MarketDetailUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    /** 수동 새로고침 (10분 쿨다운). 쿨다운 중이거나 진행 중이면 무시. */
    fun refresh(nowMillis: Long) {
        val state = _uiState.value
        if (state.isRefreshing || cooldownRemainingMillis(state, nowMillis) > 0) return
        load(forceRefresh = true, manualRefreshAtMillis = nowMillis)
    }

    private fun load(forceRefresh: Boolean, manualRefreshAtMillis: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.indices.isEmpty(),
                isRefreshing = forceRefresh,
            )
            // 3개 데이터소스 병렬 조회. 각각 실패해도 나머지는 표시.
            val indicesDeferred = async { runCatching { getIndices(forceRefresh) }.getOrElse { emptyList() } }
            val cryptoDeferred = async { runCatching { getCryptoPrices(forceRefresh) }.getOrElse { emptyList() } }
            val rateDeferred = async { runCatching { getUsdKrwRate(forceRefresh) }.getOrNull() }

            val indices = indicesDeferred.await()
            val crypto = cryptoDeferred.await()
            val rate = rateDeferred.await()

            if (indices.isEmpty() && crypto.isEmpty() && rate == null) {
                Timber.w("[MarketDetailViewModel] 전체 fetch 실패")
            }

            _uiState.value = _uiState.value.copy(
                indices = indices.ifEmpty { _uiState.value.indices },
                cryptoPrices = crypto.ifEmpty { _uiState.value.cryptoPrices },
                usdKrwRate = rate ?: _uiState.value.usdKrwRate,
                isLoading = false,
                isRefreshing = false,
                lastManualRefreshAtMillis = manualRefreshAtMillis ?: _uiState.value.lastManualRefreshAtMillis,
            )
        }
    }

    companion object {
        const val REFRESH_COOLDOWN_MILLIS = 10 * 60 * 1000L

        fun cooldownRemainingMillis(state: MarketDetailUiState, nowMillis: Long): Long {
            if (state.lastManualRefreshAtMillis <= 0L) return 0L
            val elapsed = nowMillis - state.lastManualRefreshAtMillis
            return (REFRESH_COOLDOWN_MILLIS - elapsed).coerceAtLeast(0L)
        }

        /** 환율/지수/암호화폐 중 가장 최신 timestamp (갱신 시각 표시용). */
        fun latestUpdatedAt(state: MarketDetailUiState, tab: MarketDetailTab): Instant? = when (tab) {
            MarketDetailTab.INDICES -> state.indices.maxOfOrNull { it.timestamp }
            MarketDetailTab.EXCHANGE -> listOfNotNull(state.usdKrwRate?.lastUpdated).maxOrNull()
            MarketDetailTab.CRYPTO -> state.cryptoPrices.maxOfOrNull { it.timestamp }
        }?.takeIf { it != Instant.EPOCH }
    }
}

enum class MarketDetailTab { INDICES, EXCHANGE, CRYPTO }
