package th1ngjin.fearindex.core.purchases

/**
 * `onPurchasesUpdated` 의 결과 분기 — Play Billing `BillingResponseCode` + 구매 목록 → 도메인 결과.
 *
 * iOS [PurchaseManager.handlePurchaseResult] 의 success/userCancelled/pending switch 에 대응하는 순수 로직.
 * BillingClient 글루(onPurchasesUpdated) 는 이 판정을 그대로 따르기만 하면 되므로 얇게 유지된다.
 */
object IapPurchaseOutcome {

    // Play Billing `BillingClient.BillingResponseCode` 상수 (라이브러리 의존 없이 판정하기 위해 명시).
    const val RESPONSE_OK = 0
    const val RESPONSE_USER_CANCELED = 1
    const val RESPONSE_ITEM_ALREADY_OWNED = 7

    sealed interface Outcome {
        /** 대상 상품이 구매 완료됨 — grant + acknowledge 진행. */
        data class Completed(val tokensToAcknowledge: List<String>) : Outcome

        /** 사용자가 결제 시트를 조용히 취소함. */
        data object Cancelled : Outcome

        /** 이미 소유한 상품 재구매 시도 — 실패가 아니라 entitlement 재평가로 grant 처리. */
        data object AlreadyOwned : Outcome

        /** 그 외 실패(오류/pending 등). `code` 로그/Analytics 용. */
        data class Failed(val code: Int) : Outcome
    }

    fun evaluate(
        responseCode: Int,
        purchases: List<IapPurchaseSnapshot>,
        productId: String,
    ): Outcome = when (responseCode) {
        RESPONSE_OK -> {
            val entitlement = IapEntitlement.evaluate(purchases, productId)
            if (entitlement.isAdFree) {
                Outcome.Completed(tokensToAcknowledge = entitlement.tokensToAcknowledge)
            } else {
                // OK 인데 대상 상품의 PURCHASED 구매가 없음 = pending/미결제 → 실패로 취급(조용히 grant 안 함).
                Outcome.Failed(responseCode)
            }
        }
        RESPONSE_USER_CANCELED -> Outcome.Cancelled
        RESPONSE_ITEM_ALREADY_OWNED -> Outcome.AlreadyOwned
        else -> Outcome.Failed(responseCode)
    }
}
