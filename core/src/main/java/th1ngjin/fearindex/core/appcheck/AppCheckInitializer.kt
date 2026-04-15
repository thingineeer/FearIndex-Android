package th1ngjin.fearindex.core.appcheck

import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase App Check 초기화.
 *
 * - release: Play Integrity
 * - debug: Debug Provider (콘솔에 출력되는 토큰을 Firebase Console에 수동 등록해야 함)
 *
 * iOS: App Attest를 사용하므로 플랫폼별 Provider 차이만 있을 뿐 같은 App Check 설정 공유.
 */
@Singleton
class AppCheckInitializer @Inject constructor() {

    fun initialize(isDebug: Boolean) {
        val provider = if (isDebug) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(provider)
    }
}
