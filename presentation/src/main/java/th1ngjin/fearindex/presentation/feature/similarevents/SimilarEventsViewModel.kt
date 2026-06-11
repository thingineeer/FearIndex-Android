package th1ngjin.fearindex.presentation.feature.similarevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.SimilarEventsResult
import th1ngjin.fearindex.domain.repository.SimilarEventsRepository
import javax.inject.Inject

/**
 * SimilarEvents 구독 ViewModel.
 * Firestore `insights/similarEvents_{indexType}` 실시간 구독.
 */
@HiltViewModel
class SimilarEventsViewModel @Inject constructor(
    private val repository: SimilarEventsRepository,
) : ViewModel() {

    private val _marketResult = MutableStateFlow(SimilarEventsResult.EMPTY)
    val marketResult: StateFlow<SimilarEventsResult> = _marketResult.asStateFlow()

    private val _kospiResult = MutableStateFlow(SimilarEventsResult.EMPTY)
    val kospiResult: StateFlow<SimilarEventsResult> = _kospiResult.asStateFlow()

    private val _cryptoResult = MutableStateFlow(SimilarEventsResult.EMPTY)
    val cryptoResult: StateFlow<SimilarEventsResult> = _cryptoResult.asStateFlow()

    init {
        observeType(FearIndexType.MARKET)
        observeType(FearIndexType.KOSPI)
        observeType(FearIndexType.CRYPTO)
    }

    fun resultFor(indexType: FearIndexType): StateFlow<SimilarEventsResult> = when (indexType) {
        FearIndexType.MARKET -> marketResult
        FearIndexType.KOSPI -> kospiResult
        FearIndexType.CRYPTO -> cryptoResult
    }

    private fun observeType(indexType: FearIndexType) {
        viewModelScope.launch {
            repository.observe(indexType).collectLatest { result ->
                when (indexType) {
                    FearIndexType.MARKET -> _marketResult.value = result
                    FearIndexType.KOSPI -> _kospiResult.value = result
                    FearIndexType.CRYPTO -> _cryptoResult.value = result
                }
            }
        }
    }

    /**
     * 현재 점수 기반 Callable Function 트리거.
     * Firestore 문서가 없을 때 서버에 캐시 생성 요청 → snapshot listener가 자동으로 받음.
     * 같은 (indexType, score) 조합은 1번만 호출 (Repository 내부 캐시).
     */
    fun triggerForScore(indexType: FearIndexType, score: Int) {
        viewModelScope.launch {
            repository.triggerCallable(indexType, score)
        }
    }
}
