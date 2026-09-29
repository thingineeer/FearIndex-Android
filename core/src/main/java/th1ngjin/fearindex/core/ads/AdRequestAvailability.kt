package th1ngjin.fearindex.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdRequestAvailability {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /** UMP 가 개인정보 옵션 진입점을 요구하는지(EEA 등). 설정 화면 '개인정보 선택' 행 노출 여부. */
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun update(canRequestAds: Boolean) {
        _canRequestAds.value = canRequestAds
    }

    fun updatePrivacyOptionsRequired(required: Boolean) {
        _privacyOptionsRequired.value = required
    }
}
