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

    // --- 로드 전/후 컨테이너 높이 결정 (배너 미표시 버그 회귀 방지) ---

    @Test
    fun `로드 전 컨테이너 높이는 추정 높이가 0이면 표준 배너 높이로 공간을 확보한다`() {
        // 인라인 어댑티브 배너는 로드 전 estimated height가 0/음수일 수 있다.
        // 컨테이너가 0이면 로드 후에도 광고가 시각적으로 안 보이는 사고가 난다.
        assertEquals(FALLBACK_BANNER_HEIGHT_DP, resolveBannerHeightDp(estimatedHeightDp = 0, loadedHeightDp = null))
        assertEquals(FALLBACK_BANNER_HEIGHT_DP, resolveBannerHeightDp(estimatedHeightDp = -1, loadedHeightDp = null))
    }

    @Test
    fun `로드 전 추정 높이가 유효하면 그 높이로 공간을 확보한다`() {
        assertEquals(90, resolveBannerHeightDp(estimatedHeightDp = 90, loadedHeightDp = null))
    }

    @Test
    fun `로드 후에는 실제 측정 높이를 우선 사용한다`() {
        // onAdLoaded 후 실제 AdView 높이로 컨테이너를 갱신해야 클립/0높이 사고를 막는다.
        assertEquals(100, resolveBannerHeightDp(estimatedHeightDp = 50, loadedHeightDp = 100))
    }

    @Test
    fun `로드 후 측정 높이가 0이면 fallback으로 공간을 유지한다`() {
        assertEquals(FALLBACK_BANNER_HEIGHT_DP, resolveBannerHeightDp(estimatedHeightDp = 0, loadedHeightDp = 0))
    }

    @Test
    fun `로드 후 실제 높이는 클램프하지 않고 그대로 사용해 광고가 잘리지 않는다`() {
        // ★중요: AdMob "광고 프레임 크기 변경" 정책 위반 방지.
        // SDK가 정한 실제 광고 높이를 임의 상한으로 자르면 광고가 잘려 정책 위반.
        // iOS(AdBannerView.swift)도 bannerView.adSize.size 를 clamp 없이 그대로 사용.
        assertEquals(250, resolveBannerHeightDp(estimatedHeightDp = 50, loadedHeightDp = 250))
        assertEquals(300, resolveBannerHeightDp(estimatedHeightDp = 50, loadedHeightDp = 300))
    }

    @Test
    fun `로드 전 추정 높이만 maxHeight 상한으로 공간을 예약한다`() {
        // 로드 전엔 실제 크기를 모르므로 maxHeight(120)로만 예약. 로드되면 실제 높이로 교체.
        assertEquals(MAX_INLINE_BANNER_HEIGHT_DP_PUBLIC, resolveBannerHeightDp(estimatedHeightDp = 999, loadedHeightDp = null))
    }
}
