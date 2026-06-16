package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/**
 * iOS `FearIndexDateContext` 와 1:1 대응하는 타임존 매핑 검증.
 * 현재 지수/업데이트 시각을 indexType별 시장 타임존으로 표시하기 위함.
 */
class FearIndexDateContextTest {

    @Test
    fun `globalMarket 은 뉴욕 타임존`() {
        assertEquals(ZoneId.of("America/New_York"), FearIndexDateContext.GLOBAL_MARKET.zoneId)
    }

    @Test
    fun `kospi 는 서울 타임존`() {
        assertEquals(ZoneId.of("Asia/Seoul"), FearIndexDateContext.KOSPI.zoneId)
    }

    @Test
    fun `crypto 는 UTC`() {
        assertEquals(ZoneId.of("UTC"), FearIndexDateContext.CRYPTO.zoneId)
    }

    @Test
    fun `FearIndexType market 는 globalMarket context`() {
        assertEquals(FearIndexDateContext.GLOBAL_MARKET, FearIndexType.MARKET.dateContext)
    }

    @Test
    fun `FearIndexType kospi 는 kospi context`() {
        assertEquals(FearIndexDateContext.KOSPI, FearIndexType.KOSPI.dateContext)
    }

    @Test
    fun `FearIndexType crypto 는 crypto context`() {
        assertEquals(FearIndexDateContext.CRYPTO, FearIndexType.CRYPTO.dateContext)
    }
}
