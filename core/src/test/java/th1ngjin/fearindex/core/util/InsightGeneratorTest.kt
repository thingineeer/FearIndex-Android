package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType

class InsightGeneratorTest {

    @Test
    fun `indexTypeLabel - KOSPI는 KOSPI 라벨을 반환한다`() {
        assertEquals("KOSPI", indexTypeLabel(FearIndexType.KOSPI))
    }
}
