package th1ngjin.fearindex.core.purchases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `onPurchasesUpdated` 결과 분기 순수 로직 검증.
 *
 * iOS [PurchaseManager.handlePurchaseResult] success/userCancelled/기타 분기 대응.
 */
class IapPurchaseOutcomeTest {

    private val productId = "remove_ads_lifetime"

    private fun purchase(
        acknowledged: Boolean = false,
        token: String = "token",
    ) = IapPurchaseSnapshot(
        productIds = listOf(productId),
        state = IapEntitlement.STATE_PURCHASED,
        isAcknowledged = acknowledged,
        purchaseToken = token,
    )

    @Test
    fun `OK + 대상 상품 PURCHASED 면 Completed 이고 미확인 토큰을 넘긴다`() {
        val outcome = IapPurchaseOutcome.evaluate(
            responseCode = IapPurchaseOutcome.RESPONSE_OK,
            purchases = listOf(purchase(acknowledged = false, token = "t1")),
            productId = productId,
        )
        assertTrue(outcome is IapPurchaseOutcome.Outcome.Completed)
        assertEquals(
            listOf("t1"),
            (outcome as IapPurchaseOutcome.Outcome.Completed).tokensToAcknowledge,
        )
    }

    @Test
    fun `OK 인데 대상 상품 구매가 없으면 Failed`() {
        val outcome = IapPurchaseOutcome.evaluate(
            responseCode = IapPurchaseOutcome.RESPONSE_OK,
            purchases = emptyList(),
            productId = productId,
        )
        assertTrue(outcome is IapPurchaseOutcome.Outcome.Failed)
    }

    @Test
    fun `USER_CANCELED 는 Cancelled`() {
        val outcome = IapPurchaseOutcome.evaluate(
            responseCode = IapPurchaseOutcome.RESPONSE_USER_CANCELED,
            purchases = emptyList(),
            productId = productId,
        )
        assertEquals(IapPurchaseOutcome.Outcome.Cancelled, outcome)
    }

    @Test
    fun `ITEM_ALREADY_OWNED 는 AlreadyOwned - 실패가 아니라 재평가 grant 경로`() {
        val outcome = IapPurchaseOutcome.evaluate(
            responseCode = IapPurchaseOutcome.RESPONSE_ITEM_ALREADY_OWNED,
            purchases = emptyList(),
            productId = productId,
        )
        assertTrue(outcome is IapPurchaseOutcome.Outcome.AlreadyOwned)
    }

    @Test
    fun `그 외 에러 코드는 Failed 이며 코드를 보존`() {
        val outcome = IapPurchaseOutcome.evaluate(
            responseCode = 6, // ERROR
            purchases = emptyList(),
            productId = productId,
        )
        assertTrue(outcome is IapPurchaseOutcome.Outcome.Failed)
        assertEquals(6, (outcome as IapPurchaseOutcome.Outcome.Failed).code)
    }

    @Test
    fun `OK + 이미 확인된 구매면 Completed 이고 acknowledge 대상 없음`() {
        val outcome = IapPurchaseOutcome.evaluate(
            responseCode = IapPurchaseOutcome.RESPONSE_OK,
            purchases = listOf(purchase(acknowledged = true)),
            productId = productId,
        )
        assertTrue(outcome is IapPurchaseOutcome.Outcome.Completed)
        assertTrue(
            (outcome as IapPurchaseOutcome.Outcome.Completed).tokensToAcknowledge.isEmpty(),
        )
    }
}
