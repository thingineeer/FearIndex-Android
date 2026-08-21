package th1ngjin.fearindex.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FcmRegistrationRecord

class FcmRegistrationPolicyTest {

    private val now = 1_700_000_000_000L
    private val fresh = FcmRegistrationRecord(
        tokenHash = "tok-a",
        settingsHash = 11,
        buildNumber = "26",
        registeredAtMillis = now - 60_000L,
    )

    @Test
    fun `기록이 없으면 등록한다`() {
        assertTrue(FcmRegistrationPolicy.shouldRegister(null, "tok-a", 11, "26", now))
    }

    @Test
    fun `토큰·설정·빌드가 같고 24시간 이내면 건너뛴다`() {
        assertFalse(FcmRegistrationPolicy.shouldRegister(fresh, "tok-a", 11, "26", now))
    }

    @Test
    fun `FCM 토큰이 바뀌면 등록한다`() {
        assertTrue(FcmRegistrationPolicy.shouldRegister(fresh, "tok-b", 11, "26", now))
    }

    @Test
    fun `알림 설정이 바뀌면 등록한다`() {
        assertTrue(FcmRegistrationPolicy.shouldRegister(fresh, "tok-a", 12, "26", now))
    }

    @Test
    fun `앱 빌드가 바뀌면 등록한다 (appVersion 메타 갱신)`() {
        assertTrue(FcmRegistrationPolicy.shouldRegister(fresh, "tok-a", 11, "27", now))
    }

    @Test
    fun `마지막 성공 후 24시간이 지나면 재등록한다`() {
        val stale = fresh.copy(registeredAtMillis = now - FcmRegistrationPolicy.REFRESH_INTERVAL_MILLIS)
        assertTrue(FcmRegistrationPolicy.shouldRegister(stale, "tok-a", 11, "26", now))
    }

    @Test
    fun `기기 시계가 과거로 튀어도(음수 경과) 24시간 규칙으로 재등록하지 않고 건너뛴다`() {
        val future = fresh.copy(registeredAtMillis = now + 3_600_000L)
        assertFalse(FcmRegistrationPolicy.shouldRegister(future, "tok-a", 11, "26", now))
    }
}
