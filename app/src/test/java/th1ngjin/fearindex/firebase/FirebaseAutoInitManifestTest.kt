package th1ngjin.fearindex.firebase

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FirebaseAutoInitManifestTest {

    @Test
    fun `Firebase auto init은 manifest에서 기본 비활성화`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) return

        val manifest = manifestFile.readText()

        assertManifestMetaDataDisabled(manifest, "firebase_analytics_collection_enabled")
        assertManifestMetaDataDisabled(manifest, "firebase_crashlytics_collection_enabled")
        assertManifestMetaDataDisabled(manifest, "firebase_messaging_auto_init_enabled")
        assertTrue(
            "FirebaseInitProvider removal must stay explicit so screenshot mode can skip FirebaseApp initialization.",
            manifest.contains("com.google.firebase.provider.FirebaseInitProvider"),
        )
        assertTrue(
            "MobileAdsInitProvider removal must stay explicit so screenshot mode can skip AdMob startup.",
            manifest.contains("com.google.android.gms.ads.MobileAdsInitProvider"),
        )
        assertTrue(
            "Remote SDK init providers must be removed from the merged manifest.",
            manifest.contains("""tools:node="remove""""),
        )
    }

    @Test
    fun `Firebase 수동 초기화는 google services 리소스 옵션을 사용한다`() {
        val appFile = File("src/main/java/th1ngjin/fearindex/FearIndexApp.kt")
        if (!appFile.exists()) return

        val source = appFile.readText()

        assertTrue(
            "FirebaseInitProvider is removed, so manual startup must read generated google-services options.",
            source.contains("FirebaseOptions.fromResource(this)"),
        )
        assertTrue(
            "Manual startup must initialize the default FirebaseApp with explicit options.",
            source.contains("FirebaseApp.initializeApp(this, options)"),
        )
    }

    private fun assertManifestMetaDataDisabled(manifest: String, key: String) {
        val entry = Regex(
            pattern = """<meta-data\s+android:name="$key"\s+android:value="false"\s*/>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL),
        )
        assertTrue(
            "$key must default to false; app startup enables it only outside screenshot mode.",
            entry.containsMatchIn(manifest),
        )
    }
}
