package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareUrlBuilderTest {

    @Test
    fun `playStoreUrl - production 패키지 details 링크`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=th1ngjin.fearindex",
            ShareUrlBuilder.playStoreUrl(),
        )
    }

    @Test
    fun `playStoreUrl - https Play Store 도메인`() {
        val url = ShareUrlBuilder.playStoreUrl()
        assertTrue(url.startsWith("https://play.google.com/store/apps/details"))
    }

    @Test
    fun `playStoreUrl - production 패키지 ID 고정 (debug suffix 미포함)`() {
        // debug 빌드에서 공유해도 스토어에는 production 앱만 존재하므로
        // 공유 링크는 항상 production 패키지여야 한다.
        val url = ShareUrlBuilder.playStoreUrl()
        assertTrue(url.contains("id=th1ngjin.fearindex"))
        assertFalse(url.contains(".debug"))
    }

    @Test
    fun `playStoreUrl - 쿼리 파라미터로 점수 등급을 노출하지 않음`() {
        // 점수/등급은 공유 텍스트 본문에 담기므로 링크는 깔끔한 스토어 URL.
        val url = ShareUrlBuilder.playStoreUrl()
        assertFalse(url.contains("score="))
        assertFalse(url.contains("rating="))
    }
}
