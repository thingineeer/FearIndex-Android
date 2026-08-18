package th1ngjin.fearindex.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 알림 탭 런처 인텐트 extras → payload 순수 매핑 테스트. */
class LaunchIntentPayloadTest {

    @Test
    fun `FCM 탭 인텐트 - message id, sent_time(Long), data 키 매핑`() {
        val payload = LaunchIntentPayload.fromMap(
            mapOf(
                "google.message_id" to "msg-1",
                "google.sent_time" to 1750000000000L,
                "type" to "kospi_alert",
                "score" to "17",
                "title" to "코스피 극단적 공포",
                "body" to "지수가 17까지 내려왔어요.",
                "unrelated" to 42,
            ),
        )
        assertNotNull(payload)
        assertEquals("msg-1", payload!!.messageId)
        assertEquals(1750000000000L, payload.sentTimeMillis)
        assertEquals("kospi_alert", payload.data["type"])
        assertEquals("17", payload.data["score"])
        assertEquals("코스피 극단적 공포", payload.title)
        assertEquals("지수가 17까지 내려왔어요.", payload.body)
    }

    @Test
    fun `gcm notification title,body 폴백 + 문자열 sent_time 파싱`() {
        val payload = LaunchIntentPayload.fromMap(
            mapOf(
                "gcm.message_id" to "msg-2",
                "gcm.notification.title" to "t",
                "gcm.notification.body" to "b",
                "google.sent_time" to "1750000000001",
            ),
        )
        assertNotNull(payload)
        assertEquals("msg-2", payload!!.messageId)
        assertEquals("t", payload.title)
        assertEquals("b", payload.body)
        assertEquals(1750000000001L, payload.sentTimeMillis)
    }

    @Test
    fun `message id 도 type 도 없으면 null - 일반 런처 기동 무시`() {
        assertNull(LaunchIntentPayload.fromMap(mapOf("android.intent.extra.REFERRER" to "x")))
        assertNull(LaunchIntentPayload.fromMap(emptyMap()))
    }

    @Test
    fun `type 만 있어도 기록 대상 (fallback id 경로)`() {
        val payload = LaunchIntentPayload.fromMap(mapOf("type" to "crypto_alert", "title" to "t"))
        assertNotNull(payload)
        assertNull(payload!!.messageId)
        assertEquals("crypto_alert", payload.data["type"])
    }

    @Test
    fun `빈 문자열,0 이하 sent_time 은 무시`() {
        val payload = LaunchIntentPayload.fromMap(
            mapOf("google.message_id" to "  ", "type" to "weekly_report", "google.sent_time" to 0L),
        )
        assertNotNull(payload)
        assertNull(payload!!.messageId)
        assertNull(payload.sentTimeMillis)
    }
}
