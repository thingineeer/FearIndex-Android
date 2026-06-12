package th1ngjin.fearindex.edge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EdgeToEdgePolicyTest {

    @Test
    fun `MainActivity는 Android 15 edge-to-edge 호환 호출을 유지한다`() {
        val source = File("src/main/java/th1ngjin/fearindex/MainActivity.kt").readText()

        assertTrue(
            "Android 15 targetSdk 35 호환을 위해 Activity onCreate에서 enableEdgeToEdge()를 호출해야 합니다.",
            source.contains("enableEdgeToEdge()"),
        )
    }

    @Test
    fun `theme code는 deprecated system bar color API를 직접 호출하지 않는다`() {
        val themeSource = File("../presentation/src/main/java/th1ngjin/fearindex/presentation/theme/Theme.kt")
            .readText()

        assertFalse(
            "Android 15 Play 권장 조치 대응: Window.statusBarColor 직접 설정을 제거해야 합니다.",
            themeSource.contains(".statusBarColor"),
        )
        assertFalse(
            "Android 15 Play 권장 조치 대응: Window.navigationBarColor 직접 설정을 제거해야 합니다.",
            themeSource.contains(".navigationBarColor"),
        )
    }
}
