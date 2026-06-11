package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsTest {

    @Test
    fun `DEFAULT - iOS 1_8_0 알림 schema 기본값과 일치한다`() {
        val settings = NotificationSettings.DEFAULT

        assertFalse(settings.notificationEnabled)
        assertTrue(settings.globalNotificationEnabled)
        assertEquals(30, settings.marketLowerThreshold)
        assertEquals(70, settings.marketUpperThreshold)
        assertTrue(settings.kospiNotificationEnabled)
        assertEquals(30, settings.kospiLowerThreshold)
        assertEquals(70, settings.kospiUpperThreshold)
        assertTrue(settings.cryptoNotificationEnabled)
        assertEquals(25, settings.cryptoLowerThreshold)
        assertEquals(75, settings.cryptoUpperThreshold)
        assertTrue(settings.weeklyReportNotificationEnabled)
        assertEquals(180, NotificationSettings.SCHEMA_VERSION)
    }
}
