package th1ngjin.fearindex.presentation.component

import th1ngjin.fearindex.core.remoteconfig.AdsRemoteConfig
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
    private var backgroundEnteredAtMillis: Long? = null

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

    /** 백그라운드 진입 시각 기록 (AppOpenAdPolicy 와 동일 패턴). */
    fun recordBackgroundEntry(nowMillis: Long) {
        backgroundEnteredAtMillis = nowMillis
    }

    /**
     * 포그라운드 복귀 처리 — 백그라운드 체류가 [InterstitialForegroundGate] 기준 이상이면 새 세션으로 리셋.
     * 백그라운드 기록은 1회만 소비된다(콜드스타트/중복 복귀는 리셋 없음).
     * @return 세션을 리셋했으면 true.
     */
    fun handleForegroundEntry(nowMillis: Long): Boolean {
        val enteredAt = backgroundEnteredAtMillis ?: return false
        backgroundEnteredAtMillis = null
        if (!InterstitialForegroundGate.shouldResetSession(backgroundMillis = nowMillis - enteredAt)) return false
        resetSession()
        return true
    }
}

object InterstitialAdSessionState {
    val policy = InterstitialAdPolicy()

    /**
     * Application 라이프사이클(백그라운드 진입/포그라운드 복귀) 훅용 코디네이터 — [policy] 를 공유한다.
     * show 경로는 HomeScreen 의 reporter 달린 코디네이터가 담당하므로 여기선 reporter 없음.
     */
    val lifecycleCoordinator: InterstitialAdCoordinator by lazy { InterstitialAdCoordinator(policy = policy) }
}

/** Remote Config 광고 값 → 인터스티셜 정책 설정. `canRequestAds` 는 UMP 동의 && 광고 제거 미구매. */
fun AdsRemoteConfig.interstitialAdPolicyConfig(canRequestAds: Boolean): InterstitialAdPolicyConfig =
    InterstitialAdPolicyConfig(
        canRequestAds = canRequestAds,
        adsEnabled = adsEnabled,
        interstitialEnabled = interstitialAdsEnabled,
        kospiEntryEnabled = kospiInterstitialEnabled,
        sessionCap = interstitialSessionCap,
        cooldownMillis = interstitialCooldownMillis,
    )
