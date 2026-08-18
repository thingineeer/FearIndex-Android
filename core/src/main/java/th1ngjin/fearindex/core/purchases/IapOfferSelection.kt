package th1ngjin.fearindex.core.purchases

/**
 * 일회성(INAPP) 상품의 구매 오퍼 선택 — Play Billing 8 대응 순수 로직.
 *
 * v8 부터 일회성 상품도 구매 옵션(purchase option) 단위로 오퍼가 내려오고,
 * `launchBillingFlow` 에 해당 오퍼의 `offerToken` 을 넘겨야 결제 시트가 열린다.
 * 가격 표시와 결제가 어긋나지 않도록 **선택된 하나의 오퍼**에서 둘 다 취한다.
 *
 * 광고 제거 상품(`remove_ads_lifetime`)은 구매 옵션이 하나뿐이라 첫 오퍼가 곧 기본 구매 옵션이다.
 * (옵션이 여러 개인 상품을 추가하면 그때 선택 기준을 이 함수에 넣는다.)
 */
object IapOfferSelection {

    /** Play Billing `ProductDetails.OneTimePurchaseOfferDetails` 에서 필요한 값만 추린 순수 데이터. */
    data class Offer(
        val offerToken: String,
        val formattedPrice: String?,
    )

    /** 결제에 쓸 오퍼. 토큰이 없는 오퍼는 결제가 불가능하므로 제외한다. */
    fun select(offers: List<Offer>): Offer? =
        offers.firstOrNull { it.offerToken.isNotBlank() }
}
