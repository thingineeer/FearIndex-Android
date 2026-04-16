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

    private val _cryptoResult = MutableStateFlow(SimilarEventsResult.EMPTY)
    val cryptoResult: StateFlow<SimilarEventsResult> = _cryptoResult.asStateFlow()

    init {
        observeType(FearIndexType.MARKET)
        observeType(FearIndexType.CRYPTO)
    }

    fun resultFor(indexType: FearIndexType): StateFlow<SimilarEventsResult> = when (indexType) {
        FearIndexType.MARKET -> marketResult
        FearIndexType.CRYPTO -> cryptoResult
    }

    private fun observeType(indexType: FearIndexType) {
        viewModelScope.launch {
            repository.observe(indexType).collectLatest { result ->
                when (indexType) {
                    FearIndexType.MARKET -> _marketResult.value = result
                    FearIndexType.CRYPTO -> _cryptoResult.value = result
                }
            }
        }
    }
}
