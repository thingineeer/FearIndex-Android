package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareUrlBuilderTest {

    @Test
    fun `build - 기본 URL 형식 검증`() {
        val url = ShareUrlBuilder.build(score = 42, type = "market", rating = "Fear")

        assertEquals("https://fear-index-a4f4b.web.app/?score=42&type=market&rating=Fear", url)
    }

    @Test
    fun `build - crypto 타입`() {
        val url = ShareUrlBuilder.build(score = 75, type = "crypto", rating = "Greed")

        assertTrue(url.contains("type=crypto"))
        assertTrue(url.contains("score=75"))
    }

    @Test
    fun `build - rating에 공백 포함 시 URL 인코딩`() {
        val url = ShareUrlBuilder.build(score = 10, type = "market", rating = "Extreme Fear")

        assertTrue(url.contains("rating=Extreme+Fear") || url.contains("rating=Extreme%20Fear"))
    }

    @Test
    fun `build - rating에 특수문자 포함 시 URL 인코딩`() {
        val url = ShareUrlBuilder.build(score = 50, type = "market", rating = "Neutral & Calm")

        // & 문자가 인코딩되어야 함
        assertTrue(url.contains("rating=Neutral+%26+Calm") || url.contains("rating=Neutral%20%26%20Calm"))
    }

    @Test
    fun `build - score 0`() {
        val url = ShareUrlBuilder.build(score = 0, type = "market", rating = "Extreme Fear")

        assertTrue(url.contains("score=0"))
    }

    @Test
    fun `build - score 100`() {
        val url = ShareUrlBuilder.build(score = 100, type = "crypto", rating = "Extreme Greed")

        assertTrue(url.contains("score=100"))
    }

    @Test
    fun `build - 한글 rating 인코딩`() {
        val url = ShareUrlBuilder.build(score = 50, type = "market", rating = "중립")

        // 한글이 URL 인코딩되어야 함
        assertTrue(url.contains("rating="))
        assertTrue(!url.contains("rating=중립")) // 인코딩 되었으므로 한글 직접 포함 안 됨
    }
}
