package th1ngjin.fearindex.core.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import th1ngjin.fearindex.core.BuildConfig

class ScreenshotModeTest {

    @Test
    fun `debug 빌드에서만 system property screenshot mode를 지원한다`() {
        assertEquals(BuildConfig.DEBUG, BuildConfig.SCREENSHOT_MODE_SUPPORTED)
    }

    @Test
    fun `testing override false는 fixture mode를 끈다`() {
        ScreenshotMode.setOverrideForTesting(false)
        try {
            assertFalse(ScreenshotMode.isEnabled())
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }
}
