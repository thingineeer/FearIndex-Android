package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** iOS `NotificationRecordPayloadTests` 포팅 + Android FCM(RemoteMessage) 특성 반영. */
class NotificationRecordMapperTest {

    private val now: Instant = Instant.ofEpochSecond(1_800_000_000)

    private fun parse(
        data: Map<String, String>,
        messageId: String? = null,
        title: String? = "제목",
        body: String? = "본문",
    ): NotificationRecord? =
        NotificationRecordMapper.fromPush(data, messageId, title, body, now)

    @Test
    fun `kindFromType - fear_index_alert와 global_ 접두사는 MARKET`() {
        assertEquals(NotificationKind.MARKET, NotificationRecordMapper.kindFromType("fear_index_alert"))
        assertEquals(NotificationKind.MARKET, NotificationRecordMapper.kindFromType("global_fear_index_alert"))
    }

    @Test
    fun `kindFromType - kospi_ crypto_ weekly_ 접두사 매핑, 그 외와 null은 OTHER`() {
        assertEquals(NotificationKind.KOSPI, NotificationRecordMapper.kindFromType("kospi_fear_index_alert"))
        assertEquals(NotificationKind.CRYPTO, NotificationRecordMapper.kindFromType("crypto_fear_index_alert"))
        assertEquals(NotificationKind.WEEKLY, NotificationRecordMapper.kindFromType("weekly_report"))
        assertEquals(NotificationKind.OTHER, NotificationRecordMapper.kindFromType("onboarding_drip"))
        assertEquals(NotificationKind.OTHER, NotificationRecordMapper.kindFromType(null))
    }

    @Test
    fun `fromPush - data type이 kind로 정규화된다`() {
        assertEquals(NotificationKind.MARKET, parse(mapOf("type" to "fear_index_alert"))?.kind)
        assertEquals(NotificationKind.KOSPI, parse(mapOf("type" to "kospi_fear_index_alert"))?.kind)
        assertEquals(NotificationKind.OTHER, parse(mapOf("type" to "onboarding_drip", "day" to "2"))?.kind)
        assertEquals(NotificationKind.OTHER, parse(emptyMap())?.kind)
    }

    @Test
    fun `fromPush - id 우선순위 messageId - google_message_id - gcm_message_id - fallback`() {
        val data = mapOf(
            "type" to "crypto_fear_index_alert",
            "google.message_id" to "google-id",
            "gcm.message_id" to "gcm-id",
        )
        assertEquals("msg-1", parse(data, messageId = "msg-1")?.id)
        assertEquals("google-id", parse(data, messageId = null)?.id)
        assertEquals("google-id", parse(data, messageId = "   ")?.id)
        assertEquals("gcm-id", parse(data - "google.message_id")?.id)
        assertEquals("crypto-1800000000", parse(mapOf("type" to "crypto_fear_index_alert"))?.id)
    }

    @Test
    fun `fromPush - score 문자열은 Int로 반올림, 없거나 파싱 불가면 null`() {
        assertEquals(25, parse(mapOf("type" to "fear_index_alert", "score" to "25"))?.score)
        assertEquals(25, parse(mapOf("type" to "fear_index_alert", "score" to "25.4"))?.score)
        assertEquals(24, parse(mapOf("type" to "fear_index_alert", "score" to "23.6"))?.score)
        assertNull(parse(mapOf("type" to "fear_index_alert", "score" to "abc"))?.score)
        assertNull(parse(mapOf("type" to "fear_index_alert", "score" to "NaN"))?.score)
        assertNull(parse(mapOf("type" to "weekly_report"))?.score)
    }

    @Test
    fun `fromPush - title body receivedAt 보존, 앞뒤 공백 제거`() {
        val record = parse(
            mapOf("type" to "kospi_fear_index_alert"),
            title = " 코스피 극단적 공포 ",
            body = "지수 15 ",
        )
        assertEquals("코스피 극단적 공포", record?.title)
        assertEquals("지수 15", record?.body)
        assertEquals(now, record?.receivedAt)
    }

    @Test
    fun `fromPush - 제목과 본문이 모두 비어 있으면 (silent push) null`() {
        assertNull(parse(mapOf("type" to "fear_index_alert"), title = "", body = "  "))
        assertNull(parse(mapOf("type" to "fear_index_alert"), title = null, body = null))
        assertNotNull(parse(mapOf("type" to "fear_index_alert"), title = "T", body = ""))
        assertNotNull(parse(mapOf("type" to "fear_index_alert"), title = null, body = "B"))
    }
}
