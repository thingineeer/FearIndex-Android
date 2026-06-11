package th1ngjin.fearindex.presentation.feature.vote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.service.StuckStatusDebouncer
import th1ngjin.fearindex.domain.usecase.GetVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.ObserveStuckCounterUseCase
import th1ngjin.fearindex.domain.usecase.ObserveVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.SubmitVoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 투표 화면 ViewModel.
 *
 * - 물림 카운터: Firestore 실시간 스트림 + 5초 디바운스 서버 호출
 * - Buy/Hold/Sell 투표: Cloud Function 호출 + Firestore 실시간 스트림
 * - UTC 자정 카운트다운: 1초 주기 갱신
 */
@HiltViewModel
class VoteViewModel @Inject constructor(
    private val observeStuckCounter: ObserveStuckCounterUseCase,
    private val debouncer: StuckStatusDebouncer,
    private val submitVoteUseCase: SubmitVoteUseCase,
    private val getVoteResultUseCase: GetVoteResultUseCase,
    private val observeVoteResultUseCase: ObserveVoteResultUseCase,
) : ViewModel() {

    // ============================================================
    // Stuck Counter
    // ============================================================

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

    // ============================================================
    // Buy/Hold/Sell Vote
    // ============================================================

    private val _marketVoteResult = MutableStateFlow(VoteResult.EMPTY)
    val marketVoteResult: StateFlow<VoteResult> = _marketVoteResult.asStateFlow()

    private val _cryptoVoteResult = MutableStateFlow(VoteResult.EMPTY)
    val cryptoVoteResult: StateFlow<VoteResult> = _cryptoVoteResult.asStateFlow()

    private val _isVoteSubmitting = MutableStateFlow(false)
    val isVoteSubmitting: StateFlow<Boolean> = _isVoteSubmitting.asStateFlow()

    private val _voteError = MutableStateFlow<String?>(null)
    val voteError: StateFlow<String?> = _voteError.asStateFlow()

    /** UTC 자정까지 남은 시간 (hours, minutes, seconds) */
    private val _countdown = MutableStateFlow(Triple(0, 0, 0))
    val countdown: StateFlow<Triple<Int, Int, Int>> = _countdown.asStateFlow()

    private var marketVoteStreamJob: Job? = null
    private var cryptoVoteStreamJob: Job? = null
    private var countdownJob: Job? = null

    init {
        // 물림 카운터 초기화
        _myMarketStatus.value = observeStuckCounter.loadLocalStatus(FearIndexType.MARKET)
        _myCryptoStatus.value = observeStuckCounter.loadLocalStatus(FearIndexType.CRYPTO)
        startStream(FearIndexType.MARKET)
        startStream(FearIndexType.CRYPTO)
        loadInitialStuckResults()
        viewModelScope.launch { debouncer.retryPendingIfNeeded() }

        // 투표 초기화
        startVoteStream(FearIndexType.MARKET)
        startVoteStream(FearIndexType.CRYPTO)
        loadInitialVoteResults()
        startCountdown()
    }

    private fun loadInitialStuckResults() {
        viewModelScope.launch {
            try {
                val result = observeStuckCounter.fetchOnce(FearIndexType.MARKET)
                _marketResult.value = result
            } catch (e: Exception) {
                Timber.e(e, "[VoteViewModel] 시장 물림 카운터 초기 로딩 실패")
            }
        }
        viewModelScope.launch {
            try {
                val result = observeStuckCounter.fetchOnce(FearIndexType.CRYPTO)
                _cryptoResult.value = result
            } catch (e: Exception) {
                Timber.e(e, "[VoteViewModel] 암호화폐 물림 카운터 초기 로딩 실패")
            }
        }
    }

    // ============================================================
    // Stuck Counter — Public API
    // ============================================================

    fun resultFor(indexType: FearIndexType): StateFlow<StuckCounterResult> = when (indexType) {
        FearIndexType.MARKET -> marketResult
        FearIndexType.KOSPI -> marketResult
        FearIndexType.CRYPTO -> cryptoResult
    }

    fun myStatusFor(indexType: FearIndexType): StateFlow<StuckStatus> = when (indexType) {
        FearIndexType.MARKET -> myMarketStatus
        FearIndexType.KOSPI -> myMarketStatus
        FearIndexType.CRYPTO -> myCryptoStatus
    }

    fun toggleStuckStatus(indexType: FearIndexType, status: StuckStatus) {
        when (indexType) {
            FearIndexType.MARKET -> {
                _myMarketStatus.value = status
                _marketResult.value = _marketResult.value.withOptimisticToggle(status)
            }
            FearIndexType.KOSPI -> {
                _myMarketStatus.value = status
                _marketResult.value = _marketResult.value.withOptimisticToggle(status)
            }
            FearIndexType.CRYPTO -> {
                _myCryptoStatus.value = status
                _cryptoResult.value = _cryptoResult.value.withOptimisticToggle(status)
            }
        }
        debouncer.schedule(indexType, status)
    }

    // ============================================================
    // Buy/Hold/Sell Vote — Public API
    // ============================================================

    fun voteResultFor(indexType: FearIndexType): StateFlow<VoteResult> = when (indexType) {
        FearIndexType.MARKET -> marketVoteResult
        FearIndexType.KOSPI -> marketVoteResult
        FearIndexType.CRYPTO -> cryptoVoteResult
    }

    fun canVoteToday(indexType: FearIndexType): Boolean {
        val result = voteResultFor(indexType).value
        return result.myVote == null
    }

    fun submitVote(indexType: FearIndexType, choice: VoteChoice, fearScore: Int) {
        if (!canVoteToday(indexType)) return

        // 낙관적 업데이트
        val current = voteResultFor(indexType).value
        val optimistic = current.copy(
            buyCount = current.buyCount + if (choice == VoteChoice.BUY) 1 else 0,
            holdCount = current.holdCount + if (choice == VoteChoice.HOLD) 1 else 0,
            sellCount = current.sellCount + if (choice == VoteChoice.SELL) 1 else 0,
            totalCount = current.totalCount + 1,
            myVote = choice,
        )
        updateVoteResult(indexType, optimistic)

        viewModelScope.launch {
            _isVoteSubmitting.value = true
            _voteError.value = null
            try {
                val serverResult = submitVoteUseCase(
                    indexType = indexType.serverValue(),
                    choice = choice,
                    fearScore = fearScore,
                )
                updateVoteResult(indexType, serverResult)
            } catch (e: Exception) {
                Timber.e(e, "[VoteViewModel] submitVote 실패")
                _voteError.value = e.message
                // 롤백
                updateVoteResult(indexType, current)
            } finally {
                _isVoteSubmitting.value = false
            }
        }
    }

    fun clearVoteError() {
        _voteError.value = null
    }

    // ============================================================
    // Private — Stuck Counter
    // ============================================================

    private fun startStream(indexType: FearIndexType) {
        val job = viewModelScope.launch {
            observeStuckCounter.stream(indexType)
                .catch { e -> Timber.e(e, "[VoteViewModel] stream error: $indexType") }
                .collect { result ->
                    when (indexType) {
                        FearIndexType.MARKET -> _marketResult.value = result
                        FearIndexType.KOSPI -> _marketResult.value = result
                        FearIndexType.CRYPTO -> _cryptoResult.value = result
                    }
                }
        }
        when (indexType) {
            FearIndexType.MARKET -> {
                marketStreamJob?.cancel(); marketStreamJob = job
            }
            FearIndexType.KOSPI -> {
                marketStreamJob?.cancel(); marketStreamJob = job
            }
            FearIndexType.CRYPTO -> {
                cryptoStreamJob?.cancel(); cryptoStreamJob = job
            }
        }
    }

    // ============================================================
    // Private — Vote
    // ============================================================

    private fun startVoteStream(indexType: FearIndexType) {
        val job = viewModelScope.launch {
            observeVoteResultUseCase(indexType.serverValue())
                .catch { e -> Timber.e(e, "[VoteViewModel] vote stream error: $indexType") }
                .collect { result ->
                    updateVoteResult(indexType, result)
                }
        }
        when (indexType) {
            FearIndexType.MARKET -> {
                marketVoteStreamJob?.cancel(); marketVoteStreamJob = job
            }
            FearIndexType.KOSPI -> {
                marketVoteStreamJob?.cancel(); marketVoteStreamJob = job
            }
            FearIndexType.CRYPTO -> {
                cryptoVoteStreamJob?.cancel(); cryptoVoteStreamJob = job
            }
        }
    }

    private fun loadInitialVoteResults() {
        viewModelScope.launch {
            try {
                val marketResult = getVoteResultUseCase(FearIndexType.MARKET.serverValue())
                updateVoteResult(FearIndexType.MARKET, marketResult)
            } catch (e: Exception) {
                Timber.e(e, "[VoteViewModel] 시장 투표 결과 초기 로딩 실패")
            }
        }
        viewModelScope.launch {
            try {
                val cryptoResult = getVoteResultUseCase(FearIndexType.CRYPTO.serverValue())
                updateVoteResult(FearIndexType.CRYPTO, cryptoResult)
            } catch (e: Exception) {
                Timber.e(e, "[VoteViewModel] 암호화폐 투표 결과 초기 로딩 실패")
            }
        }
    }

    private fun updateVoteResult(indexType: FearIndexType, result: VoteResult) {
        when (indexType) {
            FearIndexType.MARKET -> _marketVoteResult.value = result
            FearIndexType.KOSPI -> _marketVoteResult.value = result
            FearIndexType.CRYPTO -> _cryptoVoteResult.value = result
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val now = ZonedDateTime.now(ZoneOffset.UTC)
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC)
                val remainingSeconds = ChronoUnit.SECONDS.between(now, nextMidnight)

                val hours = (remainingSeconds / 3600).toInt()
                val minutes = ((remainingSeconds % 3600) / 60).toInt()
                val seconds = (remainingSeconds % 60).toInt()
                _countdown.value = Triple(hours, minutes, seconds)

                delay(1_000L)
            }
        }
    }

    private fun FearIndexType.serverValue(): String = name.lowercase()

    // ============================================================
    // Lifecycle
    // ============================================================

    override fun onCleared() {
        super.onCleared()
        marketStreamJob?.cancel()
        cryptoStreamJob?.cancel()
        marketVoteStreamJob?.cancel()
        cryptoVoteStreamJob?.cancel()
        countdownJob?.cancel()
    }
}
