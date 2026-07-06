package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.NotificationDataSource
import th1ngjin.fearindex.data.storage.NotificationStorage
import th1ngjin.fearindex.domain.entity.NotificationSettings

class NotificationRepositoryImplTest {

    private val dataSource = mockk<NotificationDataSource>(relaxed = true)
    private val storage = mockk<NotificationStorage>(relaxed = true)
    private val repository = NotificationRepositoryImpl(dataSource, storage)

    private val deviceId = "550e8400-e29b-41d4-a716-446655440000"

    @Test
    fun `registerFCMToken - DataSource에 deviceId+token 그대로 위임`() = runTest {
        val token = "fake-fcm-token-abc123"
        val localSettings = NotificationSettings.DEFAULT.copy(notificationEnabled = false)
        every { storage.load() } returns localSettings

        repository.registerFCMToken(deviceId, token)

        coVerify(exactly = 1) { dataSource.registerFCMToken(deviceId, token, localSettings) }
    }

    @Test
    fun `registerFCMToken - 등록 직후 로컬 설정 전체를 updateSettings로 동기화`() = runTest {
        // 서버 registerFCMToken은 payload에 임계값이 없으면 신규 기기 즉시 체크를 보류함.
        // 등록 직후 updateNotificationSettings 호출이 즉시 체크를 대신 트리거 (iOS v1.8.8 parity).
        val token = "fake-fcm-token-abc123"
        val localSettings = NotificationSettings.DEFAULT.copy(marketLowerThreshold = 20)
        every { storage.load() } returns localSettings

        repository.registerFCMToken(deviceId, token)

        io.mockk.coVerifyOrder {
            dataSource.registerFCMToken(deviceId, token, localSettings)
            dataSource.updateSettings(deviceId, localSettings)
        }
    }

    @Test
    fun `updateSettings - 로컬 저장 먼저 수행한 뒤 서버 동기화`() = runTest {
        val settings = NotificationSettings(
            notificationEnabled = true,
            globalNotificationEnabled = false,
            marketLowerThreshold = 25,
            marketUpperThreshold = 75,
            kospiNotificationEnabled = true,
            kospiLowerThreshold = 30,
            kospiUpperThreshold = 70,
            cryptoNotificationEnabled = false,
            cryptoLowerThreshold = 30,
            cryptoUpperThreshold = 70,
            weeklyReportNotificationEnabled = false,
        )

        repository.updateSettings(deviceId, settings)

        // 로컬 → 서버 순서 검증 (optimistic update)
        verify(exactly = 1) { storage.save(settings) }
        coVerify(exactly = 1) { dataSource.updateSettings(deviceId, settings) }
    }

    @Test
    fun `unregisterDevice - DataSource에 위임`() = runTest {
        repository.unregisterDevice(deviceId)

        coVerify(exactly = 1) { dataSource.unregisterDevice(deviceId) }
    }

    @Test
    fun `getSettings - 로컬 캐시 반환 (서버 조회 안함)`() = runTest {
        val cached = NotificationSettings(notificationEnabled = true, marketLowerThreshold = 15)
        every { storage.load() } returns cached

        val result = repository.getSettings(deviceId)

        assertEquals(cached, result)
        coVerify(exactly = 0) { dataSource.registerFCMToken(any(), any(), any()) }
        coVerify(exactly = 0) {
            dataSource.updateSettings(any(), any())
        }
    }

    @Test
    fun `saveSettingsLocal - 서버 호출 없이 로컬만 저장`() = runTest {
        val settings = NotificationSettings(notificationEnabled = false)

        repository.saveSettingsLocal(settings)

        verify(exactly = 1) { storage.save(settings) }
        coVerify(exactly = 0) {
            dataSource.updateSettings(any(), any())
        }
    }

    @Test
    fun `loadSettingsLocal - storage load 결과 반환`() = runTest {
        val cached = NotificationSettings.DEFAULT
        every { storage.load() } returns cached

        val result = repository.loadSettingsLocal()

        assertEquals(cached, result)
    }

    @Test
    fun `screenshot mode - 서버 동기화는 건너뛰고 로컬 저장만 수행`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)
        val settings = NotificationSettings(notificationEnabled = true)

        try {
            repository.registerFCMToken(deviceId, "token")
            repository.updateSettings(deviceId, settings)
            repository.unregisterDevice(deviceId)

            verify(exactly = 1) { storage.save(settings) }
            coVerify(exactly = 0) { dataSource.registerFCMToken(any(), any(), any()) }
            coVerify(exactly = 0) { dataSource.updateSettings(any(), any()) }
            coVerify(exactly = 0) { dataSource.unregisterDevice(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `updateSettings - DataSource 실패해도 로컬은 이미 저장됨`() = runTest {
        val settings = NotificationSettings(notificationEnabled = true)
        coEvery {
            dataSource.updateSettings(any(), any())
        } throws RuntimeException("network failure")

        val ex = runCatching { repository.updateSettings(deviceId, settings) }.exceptionOrNull()

        assertEquals("network failure", ex?.message)
        // 로컬 저장은 호출됐어야 함 (서버 실패와 무관하게 옵티미스틱)
        verify(exactly = 1) { storage.save(settings) }
    }
}
