package th1ngjin.fearindex.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdBannerLayoutTest {

    @Test
    fun `배너 폭은 화면 전체 폭이 아니라 실제 parent content 폭을 사용한다`() {
        assertEquals(328, bannerAdWidthDp(328.4f))
        assertEquals(360, bannerAdWidthDp(360.2f))
    }

    @Test
    fun `AdMob 최소 폭보다 좁으면 frame resize를 피하기 위해 광고를 숨긴다`() {
        assertNull(bannerAdWidthDp(319.9f))
    }

    @Test
    fun `inline adaptive banner는 작은 광고로 고정하지 않고 충분한 최대 높이를 요청한다`() {
        assertEquals(120, bannerAdMaxHeightDp())
    }
}
