package th1ngjin.fearindex.data.datasource

import java.util.Locale

internal object NotificationLocaleResolver {
    fun languageTag(locale: Locale?): String =
        normalize(locale?.toLanguageTag())

    fun normalize(tag: String?): String {
        val normalized = tag.orEmpty().trim().replace('_', '-')
        return normalized.ifEmpty { FALLBACK }
    }

    private const val FALLBACK = "en"
}
