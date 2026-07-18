package th1ngjin.fearindex.presentation.feature.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.purchases.PurchaseEvent
import th1ngjin.fearindex.core.purchases.PurchaseManager
import javax.inject.Inject

/**
 * 설정 화면 ViewModel — 광고 제거 IAP 구매/복원 액션 + 상태 노출.
 *
 * iOS [SettingsView] 의 premiumSection 상태(isAdFree/isPurchasing/isRestoring/priceText) 대응.
 * 구매/복원의 진행 상태와 결과 다이얼로그를 관리하고, 실제 결제 로직은 [PurchaseManager] 에 위임한다.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val purchaseManager: PurchaseManager,
) : ViewModel() {

    /** 광고 제거 보유 여부 — 카드가 구매 행/구매됨 행을 분기하는 데 사용. */
    val isAdFree: StateFlow<Boolean> = purchaseManager.isAdFree

    /** 로드된 가격(미로드시 null → UI fallback "US$4.99"). */
    val priceText: StateFlow<String?> = purchaseManager.priceText

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _dialog = MutableStateFlow<SettingsDialog?>(null)
    val dialog: StateFlow<SettingsDialog?> = _dialog.asStateFlow()

    init {
        // 구매 흐름 결과(구매 완료/실패/취소)를 구독해 진행 상태와 다이얼로그를 갱신.
        purchaseManager.purchaseEvents
            .onEach { event ->
                _isPurchasing.value = false
                when (event) {
                    PurchaseEvent.Completed -> Unit // isAdFree Flow 가 카드를 구매됨 행으로 전환
                    PurchaseEvent.Cancelled -> Unit // 조용히 종료
                    is PurchaseEvent.Failed -> _dialog.value = SettingsDialog.PurchaseFailed
                }
            }
            .launchIn(viewModelScope)

        // 안전망: 화면 이탈 등으로 구매 완료 이벤트를 놓쳐도 광고 제거가 활성화되면 스피너를 해제.
        purchaseManager.isAdFree
            .onEach { adFree -> if (adFree) _isPurchasing.value = false }
            .launchIn(viewModelScope)
    }

    fun purchaseRemoveAds(activity: Activity) {
        if (_isPurchasing.value || _isRestoring.value) return
        _isPurchasing.value = true
        purchaseManager.purchaseRemoveAds(activity)
    }

    fun restorePurchases() {
        if (_isPurchasing.value || _isRestoring.value) return
        _isRestoring.value = true
        viewModelScope.launch {
            val restored = purchaseManager.restorePurchases()
            _isRestoring.value = false
            _dialog.value = if (restored) {
                SettingsDialog.RestoreSuccess
            } else {
                SettingsDialog.RestoreFailure
            }
        }
    }

    fun dismissDialog() {
        _dialog.value = null
    }
}

/** 설정 화면 결과 다이얼로그 종류. */
enum class SettingsDialog {
    PurchaseFailed,
    RestoreSuccess,
    RestoreFailure,
}
