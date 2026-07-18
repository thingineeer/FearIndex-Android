package th1ngjin.fearindex.core.purchases

/**
 * 인앱결제 구매 스냅샷 — Play Billing `Purchase` 에서 판정에 필요한 값만 추린 순수 데이터.
 *
 * BillingClient 타입에 의존하지 않으므로 entitlement 판정 로직을 단위 테스트할 수 있다.
 * `state` 는 Play Billing `Purchase.PurchaseState` 값(1=PURCHASED, 2=PENDING 등)을 그대로 담는다.
 */
data class IapPurchaseSnapshot(
    val productIds: List<String>,
    val state: Int,
    val isAcknowledged: Boolean,
    val purchaseToken: String,
)

/**
 * 광고 제거 entitlement 판정 — 구매 목록에서 대상 상품의 `PURCHASED` 구매를 찾아
 * (1) 광고 제거 권한 보유 여부, (2) acknowledge 가 필요한 미확인 구매 토큰을 산출한다.
 *
 * iOS [PurchaseManager.refreshEntitlements] 의 currentEntitlements 순회에 대응하는 순수 로직.
 * Play 정책상 구매 후 3일 내 acknowledge 하지 않으면 자동 환불되므로, entitlement 를 인정하면서
 * 동시에 미확인 토큰을 acknowledge 대상으로 넘긴다.
 */
object IapEntitlement {

    /** Play Billing `Purchase.PurchaseState.PURCHASED`. */
    const val STATE_PURCHASED = 1

    data class Result(
        val isAdFree: Boolean,
        /** acknowledge 가 필요한(구매됐지만 미확인) 토큰들. */
        val tokensToAcknowledge: List<String>,
    )

    fun evaluate(
        purchases: List<IapPurchaseSnapshot>,
        productId: String,
    ): Result {
        val matching = purchases.filter {
            it.state == STATE_PURCHASED && it.productIds.contains(productId)
        }
        val isAdFree = matching.isNotEmpty()
        val tokensToAcknowledge = matching
            .filterNot { it.isAcknowledged }
            .map { it.purchaseToken }
        return Result(isAdFree = isAdFree, tokensToAcknowledge = tokensToAcknowledge)
    }
}
