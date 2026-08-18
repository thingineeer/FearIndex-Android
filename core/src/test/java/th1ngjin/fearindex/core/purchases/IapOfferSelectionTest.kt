package th1ngjin.fearindex.core.purchases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 일회성 상품 오퍼 선택 순수 로직 검증 (Play Billing 8 마이그레이션).
 *
 * v8 부터 일회성(INAPP) 상품도 구매 시 offerToken 이 필요하고, 오퍼는 목록으로 내려온다.
 * 가격 표시와 결제에 **같은 오퍼**가 쓰이는지가 핵심.
 */
class IapOfferSelectionTest {

    private fun offer(token: String, price: String? = "₩7,500") =
        IapOfferSelection.Offer(offerToken = token, formattedPrice = price)

    @Test
    fun `오퍼가 하나면 그 오퍼를 선택한다`() {
        val selected = IapOfferSelection.select(listOf(offer("token-a")))

        assertEquals("token-a", selected?.offerToken)
        assertEquals("₩7,500", selected?.formattedPrice)
    }

    @Test
    fun `오퍼가 여러 개면 첫 번째(기본 구매 옵션)를 선택한다`() {
        val selected = IapOfferSelection.select(
            listOf(offer("token-a"), offer("token-b", "₩9,900")),
        )

        assertEquals("token-a", selected?.offerToken)
    }

    @Test
    fun `토큰이 비어 있는 오퍼는 건너뛴다`() {
        val selected = IapOfferSelection.select(
            listOf(offer(""), offer("  "), offer("token-real")),
        )

        assertEquals("token-real", selected?.offerToken)
    }

    @Test
    fun `선택 가능한 오퍼가 없으면 null (가격 미표시 + 결제 차단)`() {
        assertNull(IapOfferSelection.select(emptyList()))
        assertNull(IapOfferSelection.select(listOf(offer(""))))
    }

    @Test
    fun `가격이 없는 오퍼도 토큰이 있으면 선택한다 (결제는 가능)`() {
        val selected = IapOfferSelection.select(listOf(offer("token-a", price = null)))

        assertEquals("token-a", selected?.offerToken)
        assertNull(selected?.formattedPrice)
    }
}
