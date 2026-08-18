package th1ngjin.fearindex.core.purchases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** debug 소스셋 전용 타입 — 저장값 매핑/강제값 매핑 (iOS DebugPremiumOverride 동일). */
class DebugPremiumOverrideTest {

    @Test
    fun `저장값 파싱 — 알 수 없는 값과 null 은 REAL`() {
        assertEquals(DebugPremiumOverride.PURCHASED, DebugPremiumOverride.fromStorage("purchased"))
        assertEquals(DebugPremiumOverride.NOT_PURCHASED, DebugPremiumOverride.fromStorage("notPurchased"))
        assertEquals(DebugPremiumOverride.REAL, DebugPremiumOverride.fromStorage("real"))
        assertEquals(DebugPremiumOverride.REAL, DebugPremiumOverride.fromStorage("garbage"))
        assertEquals(DebugPremiumOverride.REAL, DebugPremiumOverride.fromStorage(null))
    }

    @Test
    fun `강제값 — REAL 은 null, PURCHASED true, NOT_PURCHASED false`() {
        assertNull(DebugPremiumOverride.REAL.forcedAdFree)
        assertEquals(true, DebugPremiumOverride.PURCHASED.forcedAdFree)
        assertEquals(false, DebugPremiumOverride.NOT_PURCHASED.forcedAdFree)
    }
}
