package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NotificationRecordTest {

    private val now: Instant = Instant.ofEpochSecond(1_800_000_000)

    @Test
    fun `NotificationKind fromStorage - 저장값 매핑, 알 수 없는 값과 null은 OTHER 폴백`() {
        assertEquals(NotificationKind.MARKET, NotificationKind.fromStorage("market"))
        assertEquals(NotificationKind.KOSPI, NotificationKind.fromStorage("kospi"))
        assertEquals(NotificationKind.CRYPTO, NotificationKind.fromStorage("crypto"))
        assertEquals(NotificationKind.WEEKLY, NotificationKind.fromStorage("weekly"))
        assertEquals(NotificationKind.OTHER, NotificationKind.fromStorage("other"))
        assertEquals(NotificationKind.OTHER, NotificationKind.fromStorage("futureKind"))
        assertEquals(NotificationKind.OTHER, NotificationKind.fromStorage(null))
    }

    @Test
    fun `NotificationKind storageValue - iOS rawValue와 동일 (저장 포맷 불변)`() {
        assertEquals(
            listOf("market", "kospi", "crypto", "weekly", "other"),
            NotificationKind.entries.map { it.storageValue },
        )
    }

    @Test
    fun `fallbackId - kind-epochSeconds 형식`() {
        assertEquals("crypto-1800000000", NotificationRecord.fallbackId(NotificationKind.CRYPTO, now))
        // 밀리초는 버린다 (초 단위 dedup)
        assertEquals(
            "kospi-1800000000",
            NotificationRecord.fallbackId(NotificationKind.KOSPI, now.plusMillis(999)),
        )
    }

    @Test
    fun `hasFallbackId - id가 파생 키 형식이면 true, FCM message id면 false`() {
        val derived = record(id = NotificationRecord.fallbackId(NotificationKind.MARKET, now))
        val real = record(id = "0:1700000000%abc123")
        assertTrue(derived.hasFallbackId)
        assertFalse(real.hasFallbackId)
        // kind 가 다르면 같은 초라도 파생 키가 아니다
        assertFalse(record(id = "kospi-1800000000").hasFallbackId)
    }

    private fun record(id: String) = NotificationRecord(
        id = id,
        kind = NotificationKind.MARKET,
        title = "t",
        body = "b",
        score = 20,
        receivedAt = now,
    )
}
