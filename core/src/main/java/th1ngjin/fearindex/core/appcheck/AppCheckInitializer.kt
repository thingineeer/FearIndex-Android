package th1ngjin.fearindex.core.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCheckInitializer @Inject constructor() {

    fun initialize(isDebug: Boolean) {
        val appCheck: FirebaseAppCheck = Firebase.appCheck
        if (isDebug) {
            try {
                val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val getInstance = clazz.getMethod("getInstance")
                val factory = getInstance.invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                appCheck.installAppCheckProviderFactory(factory)
            } catch (e: Exception) {
                Timber.w(e, "Debug AppCheck provider not available, skipping")
            }
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
    }
}
