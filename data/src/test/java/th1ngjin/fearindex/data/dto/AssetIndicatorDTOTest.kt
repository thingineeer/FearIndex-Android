package th1ngjin.fearindex.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetIndicatorDTOTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `decode - kospi short 신규 unavailable 응답`() {
        val response = json.decodeFromString<KospiShortResponse>(
            """{"available":false,"shortRatios":[]}""",
        )

        assertFalse(response.available)
        assertEquals(emptyList<Double>(), response.shortRatios)
    }

    @Test
    fun `decode - available 필드 없는 구버전 응답은 available=true`() {
        val response = json.decodeFromString<KospiShortResponse>(
            """{"shortRatios":[4.2,4.5,3.9]}""",
        )

        assertTrue(response.available)
        assertEquals(listOf(4.2, 4.5, 3.9), response.shortRatios)
    }
}
