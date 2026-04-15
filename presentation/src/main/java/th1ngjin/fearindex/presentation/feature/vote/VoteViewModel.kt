package th1ngjin.fearindex.presentation.feature.vote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.service.StuckStatusDebouncer
import th1ngjin.fearindex.domain.usecase.ObserveStuckCounterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 물림 카운터 화면 ViewModel.
 *
 * - 각 indexType에 대해 Firestore 실시간 스트림 구독
 * - 사용자가 토글하면 Optimistic UI(즉시 myStatus 갱신) + 5초 디바운스 후 서버 호출
 * - 네트워크 실패 시 SharedPreferences에 pending 저장(데보운서가 처리)
 */
@HiltViewModel
class VoteViewModel @Inject constructor(
    private val observeStuckCounter: ObserveStuckCounterUseCase,
    private val debouncer: StuckStatusDebouncer,
) : ViewModel() {

    private val _marketResult = MutableStateFlow(StuckCounterResult.EMPTY)
    val marketResult: StateFlow<StuckCounterResult> = _marketResult.asStateFlow()

    private val _cryptoResult = MutableStateFlow(StuckCounterResult.EMPTY)
    val cryptoResult: StateFlow<StuckCounterResult> = _cryptoResult.asStateFlow()

    private val _myMarketStatus = MutableStateFlow(StuckStatus.NONE)
    val myMarketStatus: StateFlow<StuckStatus> = _myMarketStatus.asStateFlow()

    private val _myCryptoStatus = MutableStateFlow(StuckStatus.NONE)
    val myCryptoStatus: StateFlow<StuckStatus> = _myCryptoStatus.asStateFlow()

    private var marketStreamJob: Job? = null
    private var cryptoStreamJob: Job? = null

    init {
        // 로컬 캐시에서 마지막 사용자 상태 복구
        _myMarketStatus.value = observeStuckCounter.loadLocalStatus(FearIndexType.MARKET)
        _myCryptoStatus.value = observeStuckCounter.loadLocalStatus(FearIndexType.CRYPTO)

        startStream(FearIndexType.MARKET)
        startStream(FearIndexType.CRYPTO)

        // 앱 시작 시 실패한 pending 재시도
        viewModelScope.launch { debouncer.retryPendingIfNeeded() }
    }

    fun resultFor(indexType: FearIndexType): StateFlow<StuckCounterResult> = when (indexType) {
        FearIndexType.MARKET -> marketResult
        FearIndexType.CRYPTO -> cryptoResult
    }

    fun myStatusFor(indexType: FearIndexType): StateFlow<StuckStatus> = when (indexType) {
        FearIndexType.MARKET -> myMarketStatus
        FearIndexType.CRYPTO -> myCryptoStatus
    }

    /**
     * 사용자가 토글했을 때 호출. Optimistic UI 즉시 반영 + 5초 디바운스 후 서버 호출.
     */
    fun toggleStuckStatus(indexType: FearIndexType, status: StuckStatus) {
        when (indexType) {
            FearIndexType.MARKET -> _myMarketStatus.value = status
            FearIndexType.CRYPTO -> _myCryptoStatus.value = status
        }
        debouncer.schedule(indexType, status)
    }

    private fun startStream(indexType: FearIndexType) {
        val job = viewModelScope.launch {
            observeStuckCounter.stream(indexType)
                .catch { e -> Timber.e(e, "[VoteViewModel] stream error: $indexType") }
                .collect { result ->
                    when (indexType) {
                        FearIndexType.MARKET -> _marketResult.value = result
                        FearIndexType.CRYPTO -> _cryptoResult.value = result
                    }
                }
        }
        when (indexType) {
            FearIndexType.MARKET -> {
                marketStreamJob?.cancel(); marketStreamJob = job
            }
            FearIndexType.CRYPTO -> {
                cryptoStreamJob?.cancel(); cryptoStreamJob = job
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        marketStreamJob?.cancel()
        cryptoStreamJob?.cancel()
    }
}
