package th1ngjin.fearindex.core.remoteconfig

import org.junit.Assert.assertFalse
import org.junit.Test
import th1ngjin.fearindex.core.debug.ScreenshotMode

class RemoteConfigManagerTest {

    @Test
    fun `생성자는 Firebase default app 없이도 안전하다`() {
        ScreenshotMode.setOverrideForTesting(false)

        try {
            val manager = RemoteConfigManager()

            assertFalse(manager.adsEnabled)
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }
}
