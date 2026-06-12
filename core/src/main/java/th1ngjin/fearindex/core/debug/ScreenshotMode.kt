package th1ngjin.fearindex.core.debug

import th1ngjin.fearindex.core.BuildConfig

object ScreenshotMode {
    @Volatile
    private var overrideForTesting: Boolean? = null

    fun isEnabled(): Boolean =
        overrideForTesting ?: (BuildConfig.SCREENSHOT_MODE_SUPPORTED && readSystemProperty())

    fun setOverrideForTesting(enabled: Boolean?) {
        overrideForTesting = enabled
    }

    private fun readSystemProperty(): Boolean = try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val get = systemProperties.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, "debug.screenshot_mode", "0") as? String) == "1"
    } catch (e: Throwable) {
        false
    }
}
