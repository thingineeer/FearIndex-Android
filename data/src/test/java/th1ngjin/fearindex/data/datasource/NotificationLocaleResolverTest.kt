package th1ngjin.fearindex.data.datasource

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NotificationLocaleResolverTest {

    @Test
    fun `languageTag - region 포함 BCP47 태그를 보존한다`() {
        assertEquals("pt-BR", NotificationLocaleResolver.languageTag(Locale.forLanguageTag("pt-BR")))
        assertEquals("zh-TW", NotificationLocaleResolver.languageTag(Locale.forLanguageTag("zh-TW")))
        assertEquals("ko-KR", NotificationLocaleResolver.languageTag(Locale.forLanguageTag("ko-KR")))
    }

    @Test
    fun `normalize - underscore 구분자를 hyphen으로 정규화한다`() {
        assertEquals("pt-PT", NotificationLocaleResolver.normalize("pt_PT"))
        assertEquals("zh-Hant", NotificationLocaleResolver.normalize("zh_Hant"))
    }

    @Test
    fun `normalize - 빈 값은 en으로 fallback한다`() {
        assertEquals("en", NotificationLocaleResolver.normalize(""))
        assertEquals("en", NotificationLocaleResolver.normalize(null))
    }
}
