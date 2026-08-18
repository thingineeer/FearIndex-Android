package th1ngjin.fearindex.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GMA Next-Gen SDK 초기화 완료 여부 — 프로세스 전역 단일 상태.
 *
 * Next-Gen 은 `MobileAds.initialize()` 전에 광고를 load 하면 `UninitializedPropertyAccessException` 이
 * 날 수 있다(공식 migration 가이드). 초기화는 백그라운드 스레드에서 비동기로 끝나므로, 배너/인터스티셜/앱오픈
 * 로드는 전부 이 플래그가 true 가 된 뒤에만 시작한다. [AdRequestAvailability](UMP 동의)와 같은 성격의 게이트.
 */
object AdSdkState {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** MobileAds.initialize 완료 콜백에서 호출. 멱등. */
    fun markInitialized() {
        _isInitialized.value = true
    }

    /** 테스트 격리용. */
    fun resetForTest() {
        _isInitialized.value = false
    }
}
