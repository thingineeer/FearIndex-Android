package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class FearIndexTypeTest {

    @Test
    fun `entries - iOS와 동일한 순서로 market kospi crypto를 제공한다`() {
        val serverNames = FearIndexType.entries.map { it.serverName }

        assertEquals(listOf("market", "kospi", "crypto"), serverNames)
    }

    @Test
    fun `KOSPI - 서버 경로 이름은 kospi다`() {
        val serverName = FearIndexType.KOSPI.serverName

        assertEquals("kospi", serverName)
    }
}
