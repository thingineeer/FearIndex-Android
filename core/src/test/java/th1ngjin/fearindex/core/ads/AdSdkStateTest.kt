package th1ngjin.fearindex.core.ads

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GMA Next-Gen SDK 초기화 완료 게이트 — initialize 전 load 는 UninitializedPropertyAccessException 위험이 있어
 * 모든 광고 로드는 이 상태가 true 가 된 뒤에만 시작한다.
 */
class AdSdkStateTest {

    @After
    fun tearDown() = AdSdkState.resetForTest()

    @Test
    fun `초기 상태는 미초기화`() {
        assertFalse(AdSdkState.isInitialized.value)
    }

    @Test
    fun `markInitialized 후 true, 중복 호출은 무해`() {
        AdSdkState.markInitialized()
        AdSdkState.markInitialized()
        assertTrue(AdSdkState.isInitialized.value)
    }

    @Test
    fun `awaitInitialized 는 초기화 완료 시 반환`() = runBlocking {
        AdSdkState.markInitialized()
        assertEquals(true, AdSdkState.isInitialized.first { it })
    }
}
