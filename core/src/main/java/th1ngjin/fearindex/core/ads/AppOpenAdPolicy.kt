package th1ngjin.fearindex.core.ads

/**
 * 앱오픈 광고 정책 설정 — iOS ServerConfig의 app_open_* 키 대응 (Android는 Firebase RC로 흡수).
 */
data class AppOpenAdConfig(
    val enabled: Boolean = false,
    val sessionCap: Int = 2,
    val cooldownMillis: Long = 600_000L, // 10분
    val minBackgroundMillis: Long = 30_000L, // 30초
)

/**
 * 앱오픈 광고 노출/세션 정책 — 순수 로직. iOS `AppOpenAdCoordinator` 1:1 포팅.
 *
 * 콜드스타트 최초 실행은 절대 노출하지 않는다(backgroundEnteredAt이 없으면 자격 없음).
 * 백그라운드에 [AppOpenAdConfig.minBackgroundMillis] 이상 머물다 포그라운드로 복귀할 때만,
 * 세션 cap/cooldown 이내에서 노출한다. 노출 후 backgroundEnteredAt을 소비(리셋)해 같은
 * 포그라운드 세션에서 중복 노출되지 않게 한다.
 */
class AppOpenAdPolicy {

    var impressionCount: Int = 0
        private set

    private var lastImpressionAtMillis: Long? = null
    private var backgroundEnteredAtMillis: Long? = null

    /** 백그라운드 진입(ON_STOP) 시각 기록. 이후 포그라운드 복귀 판정의 기준. */
    fun recordBackgroundEntry(nowMillis: Long) {
        backgroundEnteredAtMillis = nowMillis
    }

    /**
     * 포그라운드 복귀 시 앱오픈 광고를 노출할 자격이 있는지. iOS canAttemptForegroundShow 게이트 순서.
     */
    fun canShowOnForeground(
        nowMillis: Long,
        isReady: Boolean,
        isAdFree: Boolean,
        canRequestAds: Boolean,
        config: AppOpenAdConfig,
    ): Boolean {
        if (isAdFree) return false
        if (!config.enabled || !canRequestAds) return false
        if (!isReady) return false
        if (impressionCount >= config.sessionCap) return false
        lastImpressionAtMillis?.let { last ->
            if (nowMillis - last < config.cooldownMillis) return false
        }
        // 콜드스타트/노출후 리셋 상태면 backgroundEnteredAt이 없어 자격 없음.
        val enteredAt = backgroundEnteredAtMillis ?: return false
        return nowMillis - enteredAt >= config.minBackgroundMillis
    }

    /** 노출 성공 기록 — count++, 마지막 노출 시각 갱신, backgroundEnteredAt 소비(리셋). */
    fun recordImpression(nowMillis: Long) {
        impressionCount += 1
        lastImpressionAtMillis = nowMillis
        backgroundEnteredAtMillis = null
    }

    fun resetSession() {
        impressionCount = 0
        lastImpressionAtMillis = null
        backgroundEnteredAtMillis = null
    }
}
