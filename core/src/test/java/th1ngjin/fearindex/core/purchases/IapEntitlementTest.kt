package th1ngjin.fearindex.core.purchases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 광고 제거 entitlement 판정 순수 로직 검증.
 *
 * iOS [PurchaseManager.refreshEntitlements] (productID 매칭 + verified 판정) 대응.
 */
class IapEntitlementTest {

    private val productId = "remove_ads_lifetime"

    private fun purchase(
        productIds: List<String> = listOf(productId),
        state: Int = IapEntitlement.STATE_PURCHASED,
        acknowledged: Boolean = true,
        token: String = "token",
    ) = IapPurchaseSnapshot(
        productIds = productIds,
        state = state,
        isAcknowledged = acknowledged,
        purchaseToken = token,
    )

    @Test
    fun `PURCHASED 상태에 대상 상품이 있으면 광고 제거`() {
        val result = IapEntitlement.evaluate(listOf(purchase()), productId)
        assertTrue(result.isAdFree)
    }

    @Test
    fun `구매 목록이 비어있으면 광고 유지`() {
        val result = IapEntitlement.evaluate(emptyList(), productId)
        assertFalse(result.isAdFree)
        assertTrue(result.tokensToAcknowledge.isEmpty())
    }

    @Test
    fun `다른 상품만 구매했으면 광고 유지`() {
        val result = IapEntitlement.evaluate(
            listOf(purchase(productIds = listOf("some_other_product"))),
            productId,
        )
        assertFalse(result.isAdFree)
    }

    @Test
    fun `PENDING 상태는 entitlement 아님`() {
        val result = IapEntitlement.evaluate(
            listOf(purchase(state = 2)), // PENDING
            productId,
        )
        assertFalse(result.isAdFree)
    }

    @Test
    fun `미확인 구매는 acknowledge 대상 토큰으로 산출`() {
        val result = IapEntitlement.evaluate(
            listOf(purchase(acknowledged = false, token = "tok-unack")),
            productId,
        )
        assertTrue(result.isAdFree)
        assertEquals(listOf("tok-unack"), result.tokensToAcknowledge)
    }

    @Test
    fun `이미 확인된 구매는 acknowledge 대상에서 제외`() {
        val result = IapEntitlement.evaluate(
            listOf(purchase(acknowledged = true, token = "tok-ack")),
            productId,
        )
        assertTrue(result.isAdFree)
        assertTrue(result.tokensToAcknowledge.isEmpty())
    }

    @Test
    fun `여러 상품 묶음에 대상 상품이 섞여 있어도 매칭`() {
        val result = IapEntitlement.evaluate(
            listOf(purchase(productIds = listOf("other", productId))),
            productId,
        )
        assertTrue(result.isAdFree)
    }

    @Test
    fun `PENDING 대상 상품은 acknowledge 대상 아님`() {
        val result = IapEntitlement.evaluate(
            listOf(purchase(state = 2, acknowledged = false)),
            productId,
        )
        assertFalse(result.isAdFree)
        assertTrue(result.tokensToAcknowledge.isEmpty())
    }
}
