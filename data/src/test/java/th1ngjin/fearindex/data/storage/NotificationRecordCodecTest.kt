package th1ngjin.fearindex.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import java.time.Instant

class NotificationRecordCodecTest {

    private val now: Instant = Instant.ofEpochSecond(1_800_000_000)

    @Test
    fun `encode - decode 왕복 (score 포함)`() {
        val original = NotificationRecord("m-1", NotificationKind.KOSPI, "코스피 공포", "지수 18", 18, now)
        val line = NotificationRecordCodec.encode(original)
        assertFalse(line.contains('\n'))
        assertEquals(original, NotificationRecordCodec.decode(line))
    }

    @Test
    fun `encode - decode 왕복 (score null 은 생략되고 null 로 복원)`() {
        val original = NotificationRecord("x", NotificationKind.WEEKLY, "주간", "리포트", null, now)
        val line = NotificationRecordCodec.encode(original)
        assertFalse(line.contains("score"))
        assertEquals(original, NotificationRecordCodec.decode(line))
    }

    @Test
    fun `encode - iOS 와 같은 필드명, kind 는 storageValue, receivedAt 은 epoch 초`() {
        val line = NotificationRecordCodec.encode(
            NotificationRecord("m-1", NotificationKind.MARKET, "t", "b", 30, now.plusMillis(700)),
        )
        assertTrue(line.contains("\"kind\":\"market\""))
        assertTrue(line.contains("\"receivedAt\":1800000000"))
        assertFalse(line.contains("1800000000700"))
    }

    @Test
    fun `decode - 알 수 없는 kind 는 OTHER 로 폴백 (전방 호환), 미지 키 무시`() {
        val line = """{"id":"a","kind":"futureKind","title":"t","body":"b","receivedAt":0,"extra":true}"""
        val record = NotificationRecordCodec.decode(line)
        assertEquals(NotificationKind.OTHER, record?.kind)
        assertNull(record?.score)
        assertEquals(Instant.EPOCH, record?.receivedAt)
    }

    @Test
    fun `decode - 손상 라인은 null (필수 필드 누락 포함)`() {
        assertNull(NotificationRecordCodec.decode("{broken"))
        assertNull(NotificationRecordCodec.decode("not-json"))
        assertNull(NotificationRecordCodec.decode("""{"id":"missing-date"}"""))
        assertNull(NotificationRecordCodec.decode(""))
    }
}
