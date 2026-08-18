package th1ngjin.fearindex.domain.util

import th1ngjin.fearindex.domain.entity.PremiumFeature

/**
 * 프리미엄 기능 사용 가능 여부 순수 판정 (플랫폼/Billing 무관). iOS `PremiumFeaturePolicy` 1:1.
 *
 * v1.9.4 정책: 모든 [PremiumFeature] 는 프리미엄 권한을 요구한다.
 * 기능별 예외(무료 티저 등)가 생기면 여기서만 분기한다.
 */
object PremiumFeaturePolicy {

    /**
     * @param feature 판정 대상 기능
     * @param isPremium 프리미엄 권한 보유 여부 (`isAdFree`)
     * @return 사용 가능하면 true
     */
    @Suppress("UNUSED_PARAMETER")
    fun canUse(feature: PremiumFeature, isPremium: Boolean): Boolean = isPremium
}
