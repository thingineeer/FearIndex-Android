package th1ngjin.fearindex.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdRequestAvailability {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    fun update(canRequestAds: Boolean) {
        _canRequestAds.value = canRequestAds
    }
}
