package th1ngjin.fearindex.presentation.component

internal fun isAdScreenshotMode(): Boolean = try {
    val systemProperties = Class.forName("android.os.SystemProperties")
    val get = systemProperties.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.screenshot_mode", "0") as? String) == "1"
} catch (e: Throwable) {
    false
}
