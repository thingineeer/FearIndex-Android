package th1ngjin.fearindex.data.datasource

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import th1ngjin.fearindex.domain.entity.NotificationSettings
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationClientMetadata(
    val language: String,
    val platform: String,
    val appVersion: String,
    val buildNumber: String,
    val notificationSchemaVersion: Int,
)

@Singleton
class NotificationClientMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun current(): NotificationClientMetadata {
        val packageInfo = loadPackageInfo()
        return NotificationClientMetadata(
            language = currentLanguageTag(),
            platform = PLATFORM,
            appVersion = packageInfo?.versionName ?: UNKNOWN,
            buildNumber = packageInfo?.versionCodeString() ?: UNKNOWN,
            notificationSchemaVersion = NotificationSettings.SCHEMA_VERSION,
        )
    }

    private fun loadPackageInfo(): PackageInfo? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()

    private fun currentLanguageTag(): String {
        val locale = context.resources.configuration.locales.firstOrNull()
        return NotificationLocaleResolver.languageTag(locale)
    }

    private fun LocaleList.firstOrNull() = if (size() == 0) null else get(0)

    private fun PackageInfo.versionCodeString(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            versionCode.toString()
        }

    private companion object {
        const val PLATFORM = "android"
        const val UNKNOWN = "unknown"
    }
}
