package th1ngjin.fearindex.domain.entity

/**
 * Buy/Hold/Sell 투표 선택지
 *
 * 서버 전송 값(value)은 vote Cloud Functions 스펙과 일치한다.
 */
enum class VoteChoice(val value: String) {
    BUY("buy"),
    HOLD("hold"),
    SELL("sell");

    companion object {
        fun fromServer(raw: String?): VoteChoice? = when (raw) {
            "buy" -> BUY
            "hold" -> HOLD
            "sell" -> SELL
            else -> null
        }
    }
}
