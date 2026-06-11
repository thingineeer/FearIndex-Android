package th1ngjin.fearindex.firebase

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
        assert(manifest.contains("com.google.firebase.provider.FirebaseInitProvider")) {
            "FirebaseInitProvider removal must stay explicit so screenshot mode can skip FirebaseApp initialization."
        }
        assert(manifest.contains("com.google.android.gms.ads.MobileAdsInitProvider")) {
            "MobileAdsInitProvider removal must stay explicit so screenshot mode can skip AdMob startup."
        }
        assert(manifest.contains("""tools:node="remove"""")) {
            "Remote SDK init providers must be removed from the merged manifest."
        }
    }

    private fun assertManifestMetaDataDisabled(manifest: String, key: String) {
        val entry = Regex(
            pattern = """<meta-data\s+android:name="$key"\s+android:value="false"\s*/>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL),
        )
        assert(entry.containsMatchIn(manifest)) {
            "$key must default to false; app startup enables it only outside screenshot mode."
        }
    }
}
