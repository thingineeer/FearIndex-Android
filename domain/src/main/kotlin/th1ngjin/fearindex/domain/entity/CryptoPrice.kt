package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 암호화폐 시세. iOS `CryptoPrice` 와 1:1 대응 (CoinGecko simple/price).
 */
data class CryptoPrice(
    /** CoinGecko id (예: "bitcoin"). */
    val id: String,
    /** 심볼 (예: "BTC"). */
    val symbol: String,
    /** 이름 (예: "Bitcoin"). */
    val name: String,
    /** USD 가격. */
    val price: Double,
    /** 24시간 변동률 %. */
    val change24h: Double,
    val timestamp: Instant = Instant.EPOCH,
) {
    val isPositive: Boolean get() = change24h >= 0
}

/**
 * 표시할 암호화폐 종목. iOS `CryptoCoinType` 와 1:1 (CoinGecko ids).
 */
enum class CryptoCoinType(val id: String, val symbol: String, val displayName: String) {
    BITCOIN("bitcoin", "BTC", "Bitcoin"),
    ETHEREUM("ethereum", "ETH", "Ethereum"),
    RIPPLE("ripple", "XRP", "XRP"),
    SOLANA("solana", "SOL", "Solana"),
    BNB("binancecoin", "BNB", "BNB");

    companion object {
        /** CoinGecko ids 콤마 조인 (요청 파라미터). */
        val allIds: String get() = entries.joinToString(",") { it.id }
    }
}
