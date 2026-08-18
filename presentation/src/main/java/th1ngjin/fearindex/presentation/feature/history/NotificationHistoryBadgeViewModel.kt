package th1ngjin.fearindex.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import javax.inject.Inject

/**
 * 홈 🔔 미확인 배지 ViewModel (iOS `refreshUnreadBadge` 대응).
 * 저장소 변경(`updates`) / 프리미엄 전환 / 화면 복귀([refresh]) 시 재계산.
 */
@HiltViewModel
class NotificationHistoryBadgeViewModel @Inject constructor(
    private val useCase: NotificationHistoryUseCase,
    private val purchaseManager: PurchaseManager,
) : ViewModel() {

    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    init {
        purchaseManager.isPremium.onEach { refresh() }.launchIn(viewModelScope)
        useCase.updates.onEach { refresh() }.launchIn(viewModelScope)
    }

    /** 배지 재계산 (홈 진입 / ON_RESUME) */
    fun refresh() {
        viewModelScope.launch {
            _hasUnread.value = useCase.hasUnread(isPremium = purchaseManager.isPremium.value)
        }
    }
}
