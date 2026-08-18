package th1ngjin.fearindex.presentation.feature.history

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.analytics.PremiumPurchaseSource
import th1ngjin.fearindex.core.purchases.PurchaseEvent
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.entity.PremiumFeature
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import java.time.Instant
import javax.inject.Inject

/** 알림 내역 화면 상태 (iOS Interactor notificationRecords/isNotificationHistoryLoaded/isPremium 대응). */
data class NotificationHistoryUiState(
    val records: List<NotificationRecord> = emptyList(),
    val isLoaded: Boolean = false,
    val isPremium: Boolean = false,
    val priceText: String? = null,
    val isBusy: Boolean = false,
    val dialog: NotificationHistoryDialog? = null,
)

/** 프리미엄 구매/복원 결과 다이얼로그 (설정 화면 SettingsDialog 와 동일 문구 재사용). */
enum class NotificationHistoryDialog { PurchaseFailed, RestoreSuccess, RestoreFailure }

/**
 * 알림 내역 화면 ViewModel — 로드/확인 처리/analytics + 프리미엄 잠금 row 구매·복원.
 * iOS `FearIndexInteractor` 의 Notification History 섹션 1:1 (openNotificationHistory / handleNotificationHistoryChange).
 */
@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    private val useCase: NotificationHistoryUseCase,
    private val purchaseManager: PurchaseManager,
    private val analytics: AnalyticsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationHistoryUiState())
    val uiState: StateFlow<NotificationHistoryUiState> = _uiState.asStateFlow()

    /** 화면 진입 후 확인 처리·analytics 를 1회만 수행하기 위한 게이트 */
    private var hasShown = false

    /** 마지막으로 확인 처리한 최신 레코드 시각 — markSeen→updates 재수신 루프 방지 (FCM sentTime 클럭 스큐 대비) */
    private var lastMarkedNewest: Instant? = null

    init {
        purchaseManager.isPremium
            .onEach { premium -> _uiState.update { it.copy(isPremium = premium) }; reload() }
            .launchIn(viewModelScope)
        purchaseManager.priceText
            .onEach { price -> _uiState.update { it.copy(priceText = price) } }
            .launchIn(viewModelScope)
        // 저장소 변경(새 기록/확인 처리/prune) → 리스트 갱신. 화면 표시 중이면 즉시 확인 처리(iOS handleNotificationHistoryChange).
        // setLastSeenAt 도 updates 를 발행하므로 미확인이 있을 때만 markSeen — 무한 루프 방지.
        useCase.updates
            .onEach { reload(); if (hasShown) markSeenIfUnread() }
            .launchIn(viewModelScope)
        observePurchaseEvents()
    }

    /** 화면 진입(첫 로드 완료 후): 확인 처리 + `notification_history_viewed` 1회 */
    fun onShown() {
        if (hasShown) return
        hasShown = true
        viewModelScope.launch {
            markSeen()
            analytics.log(AnalyticsEvent.알림내역조회(개수 = _uiState.value.records.size))
        }
    }

    fun purchase(activity: Activity) {
        if (_uiState.value.isBusy) return
        analytics.log(AnalyticsEvent.프리미엄잠금탭(feature = PremiumFeature.NOTIFICATION_HISTORY_UNLIMITED.analyticsKey))
        _uiState.update { it.copy(isBusy = true) }
        purchaseManager.purchaseRemoveAds(activity, PremiumPurchaseSource.NOTIFICATION_HISTORY)
    }

    fun restore() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val restored = purchaseManager.restorePurchases(PremiumPurchaseSource.NOTIFICATION_HISTORY)
            val dialog = if (restored) NotificationHistoryDialog.RestoreSuccess else NotificationHistoryDialog.RestoreFailure
            _uiState.update { it.copy(isBusy = false, dialog = dialog) }
        }
    }

    fun dismissDialog() = _uiState.update { it.copy(dialog = null) }

    private fun reload() {
        viewModelScope.launch {
            val records = useCase.fetch(isPremium = _uiState.value.isPremium)
            _uiState.update { it.copy(records = records, isLoaded = true) }
        }
    }

    private suspend fun markSeen() = useCase.markSeen()

    /**
     * 미확인이 있을 때만 확인 처리. `setLastSeenAt` 도 updates 를 발행하고, 레코드 `receivedAt`(서버
     * sentTime) 이 기기 시계보다 미래면 markSeen 후에도 unread 로 남아 무한 루프가 되므로 —
     * 같은 최신 레코드 스냅샷에 대해서는 1회만 확인 처리한다.
     */
    private suspend fun markSeenIfUnread() {
        if (!useCase.hasUnread(isPremium = _uiState.value.isPremium)) return
        val newest = _uiState.value.records.maxOfOrNull { it.receivedAt } ?: return
        if (newest == lastMarkedNewest) return
        lastMarkedNewest = newest
        markSeen()
    }

    private fun observePurchaseEvents() {
        purchaseManager.purchaseEvents
            .onEach { event ->
                val dialog = (event as? PurchaseEvent.Failed)?.let { NotificationHistoryDialog.PurchaseFailed }
                _uiState.update { it.copy(isBusy = false, dialog = dialog ?: it.dialog) }
            }
            .launchIn(viewModelScope)
        // 안전망: 완료 이벤트를 놓쳐도 프리미엄이 켜지면 스피너 해제 (설정 화면과 동일).
        purchaseManager.isPremium
            .onEach { premium -> if (premium) _uiState.update { it.copy(isBusy = false) } }
            .launchIn(viewModelScope)
    }
}
