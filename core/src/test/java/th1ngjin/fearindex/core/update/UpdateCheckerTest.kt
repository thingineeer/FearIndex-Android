package th1ngjin.fearindex.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * iOS `RemoteConfigManager.checkForUpdate()` 와 동일한 버전 판정 로직을 검증.
 *
 * - 강제 업데이트: major.minor 가 force 기준보다 낮을 때 (1.0.x → 1.1)
 * - 선택 업데이트: 전체 버전이 minimum 보다 낮을 때 (1.1.0 → 1.1.1)
 */
class UpdateCheckerTest {

    @Test
    fun `현재 major_minor 가 force 기준보다 낮으면 강제 업데이트`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.0.1",
            forceUpdateMinimumVersion = "1.1",
            minimumAppVersion = "1.1.1",
        )
        assertEquals(UpdateStatus.FORCE_UPDATE_REQUIRED, status)
    }

    @Test
    fun `1_0_0 도 force 1_1 기준이면 강제 업데이트`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.0.0",
            forceUpdateMinimumVersion = "1.1",
            minimumAppVersion = "1.1.1",
        )
        assertEquals(UpdateStatus.FORCE_UPDATE_REQUIRED, status)
    }

    @Test
    fun `현재 major_minor 가 force 기준과 같으면 강제 아님`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.1.0",
            forceUpdateMinimumVersion = "1.1",
            minimumAppVersion = "1.1.1",
        )
        // 1.1.0 < 1.1.1 이므로 선택 업데이트
        assertEquals(UpdateStatus.UPDATE_AVAILABLE, status)
    }

    @Test
    fun `force 기준과 minimum 을 모두 만족하면 최신`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.1.1",
            forceUpdateMinimumVersion = "1.1",
            minimumAppVersion = "1.1.1",
        )
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    @Test
    fun `현재가 force 기준보다 높으면 최신`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.2.0",
            forceUpdateMinimumVersion = "1.1",
            minimumAppVersion = "1.1.1",
        )
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    @Test
    fun `major 가 낮으면 강제 업데이트`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.9.0",
            forceUpdateMinimumVersion = "2.0",
            minimumAppVersion = "2.0.0",
        )
        assertEquals(UpdateStatus.FORCE_UPDATE_REQUIRED, status)
    }

    @Test
    fun `숫자 비교는 문자열 사전순이 아니라 수치 기준이다`() {
        // 사전순이면 "1.10" < "1.9" 이지만, 수치로는 1.10 > 1.9 여야 함
        val status = UpdateChecker.evaluate(
            currentVersion = "1.9.0",
            forceUpdateMinimumVersion = "1.10",
            minimumAppVersion = "1.10.0",
        )
        assertEquals(UpdateStatus.FORCE_UPDATE_REQUIRED, status)
    }

    @Test
    fun `빈 force 기준이면 강제하지 않는다`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.0.1",
            forceUpdateMinimumVersion = "",
            minimumAppVersion = "",
        )
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    @Test
    fun `current 가 비정상 문자열이면 안전하게 최신 처리`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "",
            forceUpdateMinimumVersion = "1.1",
            minimumAppVersion = "1.1.1",
        )
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    // --- v1.2.0 배포 시나리오: force_update_minimum_version = "1.2" ---

    @Test
    fun `v1_2_0 배포 - 1_0_x 사용자는 강제 업데이트`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.0.3",
            forceUpdateMinimumVersion = "1.2",
            minimumAppVersion = "1.2.0",
        )
        assertEquals(UpdateStatus.FORCE_UPDATE_REQUIRED, status)
    }

    @Test
    fun `v1_2_0 배포 - 1_1_x 사용자도 강제 업데이트`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.1.3",
            forceUpdateMinimumVersion = "1.2",
            minimumAppVersion = "1.2.0",
        )
        assertEquals(UpdateStatus.FORCE_UPDATE_REQUIRED, status)
    }

    @Test
    fun `v1_2_0 배포 - 1_2_0 사용자는 강제 제외되고 최신`() {
        val status = UpdateChecker.evaluate(
            currentVersion = "1.2.0",
            forceUpdateMinimumVersion = "1.2",
            minimumAppVersion = "1.2.0",
        )
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }
}
