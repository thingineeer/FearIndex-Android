package th1ngjin.fearindex.presentation.component

import th1ngjin.fearindex.domain.entity.FearIndexType

data class InterstitialAdPolicyConfig(
    val canRequestAds: Boolean = true,
    val adsEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
    val kospiEntryEnabled: Boolean = true,
    val sessionCap: Int = DEFAULT_SESSION_CAP,
    val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    val kospiEntryDelayMillis: Long = DEFAULT_KOSPI_ENTRY_DELAY_MILLIS,
) {
    companion object {
        const val DEFAULT_SESSION_CAP = 2
        const val DEFAULT_COOLDOWN_MILLIS = 180_000L
        const val DEFAULT_KOSPI_ENTRY_DELAY_MILLIS = 5_000L
    }
}

class InterstitialAdPolicy {

    var impressionCount: Int = 0
        private set

    private var lastImpressionAtMillis: Long? = null
    private var isShowing = false
    private var didRecordCurrentShow = false
    private var didShowKospiEntry = false

    fun shouldScheduleKospiEntry(
        previousType: FearIndexType,
        selectedType: FearIndexType,
        config: InterstitialAdPolicyConfig,
    ): Boolean =
        config.adsEnabled &&
            config.canRequestAds &&
            config.interstitialEnabled &&
            config.kospiEntryEnabled &&
            !isShowing &&
            !didShowKospiEntry &&
            previousType != FearIndexType.KOSPI &&
            selectedType == FearIndexType.KOSPI &&
            impressionCount < config.sessionCap

    fun canShow(
        isReady: Boolean,
        nowMillis: Long,
        config: InterstitialAdPolicyConfig,
    ): Boolean {
        if (!config.adsEnabled || !config.canRequestAds || !config.interstitialEnabled) return false
        if (!isReady || isShowing) return false
        if (impressionCount >= config.sessionCap) return false
        val last = lastImpressionAtMillis ?: return true
        return nowMillis - last >= config.cooldownMillis
    }

    fun canShowKospiEntry(
        isReady: Boolean,
        nowMillis: Long,
        config: InterstitialAdPolicyConfig,
    ): Boolean =
        config.kospiEntryEnabled &&
            !didShowKospiEntry &&
            canShow(isReady = isReady, nowMillis = nowMillis, config = config)

    fun markShowing() {
        isShowing = true
        didRecordCurrentShow = false
    }

    fun recordShown(nowMillis: Long, kospiEntry: Boolean = false) {
        if (isShowing && didRecordCurrentShow) return
        impressionCount += 1
        lastImpressionAtMillis = nowMillis
        didRecordCurrentShow = true
        if (kospiEntry) {
            didShowKospiEntry = true
        }
    }

    fun recordDismissed() {
        isShowing = false
        didRecordCurrentShow = false
    }

    fun recordFailedToShow() {
        isShowing = false
        didRecordCurrentShow = false
    }

    fun resetSession() {
        impressionCount = 0
        lastImpressionAtMillis = null
        isShowing = false
        didRecordCurrentShow = false
        didShowKospiEntry = false
    }
}

object InterstitialAdSessionState {
    val policy = InterstitialAdPolicy()
}
