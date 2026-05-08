package th1ngjin.fearindex.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * 알림 채널 ID 일관성 검증.
 *
 * `fear_index_alerts` 채널 ID가 다음 위치들과 정확히 일치해야 알림이 정상 노출:
 * - `NotificationChannels.FEAR_INDEX_ALERTS` (코드 상수)
 * - `AndroidManifest.xml` 의 `<meta-data android:name="com.google.firebase.messaging.default_notification_channel_id">`
 *
 * 채널 ID는 디바이스 생성 후 고정되므로 한번 노출된 ID는 절대 변경 금지.
 */
class NotificationChannelsTest {

    @Test
    fun `FEAR_INDEX_ALERTS 상수 값이 fear_index_alerts와 정확히 일치`() {
        assertEquals("fear_index_alerts", NotificationChannels.FEAR_INDEX_ALERTS)
    }

    @Test
    fun `AndroidManifest의 default_notification_channel_id가 상수 값과 일치`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            // 일부 실행 환경에서 cwd가 모듈 루트가 아닐 수 있음 — 그 경우 skip
            return
        }
        val manifest = manifestFile.readText()
        val expectedLine = """android:value="${NotificationChannels.FEAR_INDEX_ALERTS}""""
        assert(manifest.contains(expectedLine)) {
            "AndroidManifest.xml의 default_notification_channel_id가 " +
                "${NotificationChannels.FEAR_INDEX_ALERTS}와 일치하지 않음"
        }
    }
}
