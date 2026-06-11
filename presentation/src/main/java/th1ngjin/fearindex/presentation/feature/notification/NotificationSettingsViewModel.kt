package th1ngjin.fearindex.presentation.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.domain.entity.NotificationSettings
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * 알림 설정 ViewModel — 서버 동기화 + debounce.
 *
 * - 슬라이더 드래그 중 매 프레임 서버 호출 방지를 위해 debounce 0.5초 적용.
 * - 마스터 토글은 즉시 동기화.
 * - 로컬 캐시를 SSOT로 사용하고 서버에 비동기 동기화 (optimistic update).
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val analytics: AnalyticsManager,
) : ViewModel() {

    private val _settings = MutableStateFlow(NotificationSettings.DEFAULT)
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    /** debounce 전용 Flow — 슬라이더 변경 시 emit. */
    private val settingsUpdateFlow = MutableSharedFlow<NotificationSettings>(extraBufferCapacity = 1)

    private val deviceId: String by lazy { deviceIdProvider.loadDeviceId() }

    init {
        loadSettings()
        setupDebounce()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val loaded = notificationRepository.loadSettingsLocal()
            _settings.value = loaded
        }
    }

    private fun setupDebounce() {
        settingsUpdateFlow
            .debounce(500L)
            .onEach { settings -> syncToServer(settings) }
            .launchIn(viewModelScope)
    }

    fun toggleNotification(enabled: Boolean) {
        val updated = _settings.value.copy(notificationEnabled = enabled)
        _settings.value = updated
        analytics.log(AnalyticsEvent.알림설정변경(활성화 = enabled))
        // 토글은 즉시 동기화 (debounce 없음)
        viewModelScope.launch {
            notificationRepository.saveSettingsLocal(updated)
            syncToServer(updated)
        }
    }

    fun toggleGlobal(enabled: Boolean) {
        updateCategory(_settings.value.copy(globalNotificationEnabled = enabled))
    }

    fun toggleKospi(enabled: Boolean) {
        updateCategory(_settings.value.copy(kospiNotificationEnabled = enabled))
    }

    fun toggleCrypto(enabled: Boolean) {
        updateCategory(_settings.value.copy(cryptoNotificationEnabled = enabled))
    }

    fun toggleWeekly(enabled: Boolean) {
        updateCategory(_settings.value.copy(weeklyReportNotificationEnabled = enabled))
    }

    fun updateMarketLower(value: Int) {
        val current = _settings.value
        val clamped = value.coerceAtMost(current.marketUpperThreshold - 1)
        val updated = current.copy(marketLowerThreshold = clamped)
        _settings.value = updated
        viewModelScope.launch { notificationRepository.saveSettingsLocal(updated) }
        settingsUpdateFlow.tryEmit(updated)
    }

    fun updateMarketUpper(value: Int) {
        val current = _settings.value
        val clamped = value.coerceAtLeast(current.marketLowerThreshold + 1)
        val updated = current.copy(marketUpperThreshold = clamped)
        _settings.value = updated
        viewModelScope.launch { notificationRepository.saveSettingsLocal(updated) }
        settingsUpdateFlow.tryEmit(updated)
    }

    fun updateCryptoLower(value: Int) {
        val current = _settings.value
        val clamped = value.coerceAtMost(current.cryptoUpperThreshold - 1)
        val updated = current.copy(cryptoLowerThreshold = clamped)
        _settings.value = updated
        viewModelScope.launch { notificationRepository.saveSettingsLocal(updated) }
        settingsUpdateFlow.tryEmit(updated)
    }

    fun updateKospiLower(value: Int) {
        val current = _settings.value
        val clamped = value.coerceAtMost(current.kospiUpperThreshold - 1)
        val updated = current.copy(kospiLowerThreshold = clamped)
        _settings.value = updated
        viewModelScope.launch { notificationRepository.saveSettingsLocal(updated) }
        settingsUpdateFlow.tryEmit(updated)
    }

    fun updateKospiUpper(value: Int) {
        val current = _settings.value
        val clamped = value.coerceAtLeast(current.kospiLowerThreshold + 1)
        val updated = current.copy(kospiUpperThreshold = clamped)
        _settings.value = updated
        viewModelScope.launch { notificationRepository.saveSettingsLocal(updated) }
        settingsUpdateFlow.tryEmit(updated)
    }

    fun updateCryptoUpper(value: Int) {
        val current = _settings.value
        val clamped = value.coerceAtLeast(current.cryptoLowerThreshold + 1)
        val updated = current.copy(cryptoUpperThreshold = clamped)
        _settings.value = updated
        viewModelScope.launch { notificationRepository.saveSettingsLocal(updated) }
        settingsUpdateFlow.tryEmit(updated)
    }

    fun onMarketSliderFinished() {
        val s = _settings.value
        analytics.log(
            AnalyticsEvent.알림임계값변경(
                하한값 = s.marketLowerThreshold,
                상한값 = s.marketUpperThreshold,
            ),
        )
    }

    fun onCryptoSliderFinished() {
        val s = _settings.value
        analytics.log(
            AnalyticsEvent.암호화폐알림임계값변경(
                하한값 = s.cryptoLowerThreshold,
                상한값 = s.cryptoUpperThreshold,
            ),
        )
    }

    fun onKospiSliderFinished() {
        val s = _settings.value
        analytics.log(
            AnalyticsEvent.알림임계값변경(
                하한값 = s.kospiLowerThreshold,
                상한값 = s.kospiUpperThreshold,
            ),
        )
    }

    fun clearError() {
        _syncError.value = null
    }

    private suspend fun syncToServer(settings: NotificationSettings) {
        try {
            notificationRepository.updateSettings(deviceId, settings)
            _syncError.value = null
        } catch (e: Exception) {
            Timber.e(e, "알림 설정 서버 동기화 실패")
            _syncError.value = e.message
        }
    }

    private fun updateCategory(updated: NotificationSettings) {
        _settings.value = updated
        viewModelScope.launch {
            notificationRepository.saveSettingsLocal(updated)
            syncToServer(updated)
        }
    }
}
