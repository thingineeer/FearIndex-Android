package th1ngjin.fearindex.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.core.appcheck.AppCheckFailureKind
import th1ngjin.fearindex.core.appcheck.AppCheckTokenProbe
import th1ngjin.fearindex.core.appcheck.AppCheckUnavailableException
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.NotificationClientMetadata
import th1ngjin.fearindex.data.datasource.NotificationClientMetadataProvider
import th1ngjin.fearindex.data.datasource.NotificationDataSource
import th1ngjin.fearindex.data.storage.NotificationStorage
import th1ngjin.fearindex.domain.entity.FcmRegistrationRecord
import th1ngjin.fearindex.domain.entity.NotificationSettings

class NotificationRepositoryImplTest {

    private val dataSource = mockk<NotificationDataSource>(relaxed = true)
    private val storage = mockk<NotificationStorage>(relaxed = true) {
        every { loadLastRegistration() } returns null
    }
    private val metadataProvider = mockk<NotificationClientMetadataProvider> {
        every { current() } returns NotificationClientMetadata(
            language = "ko", platform = "android", appVersion = "1.5.4",
            buildNumber = "26", notificationSchemaVersion = 180,
        )
    }
    private val appCheckProbe = mockk<AppCheckTokenProbe>(relaxed = true)
    private val now = 1_700_000_000_000L
    private val repository = NotificationRepositoryImpl(dataSource, storage, metadataProvider, appCheckProbe)
        .apply { clock = { now } }

    private val deviceId = "550e8400-e29b-41d4-a716-446655440000"

    @Test
    fun `registerFCMToken - App Check 토큰 선취득 후 서버 호출, 성공 시 등록 스냅샷 저장`() = runTest {
        val localSettings = NotificationSettings.DEFAULT
        every { storage.load() } returns localSettings

        repository.registerFCMToken(deviceId, "token-x")

        io.mockk.coVerifyOrder {
            appCheckProbe.ensureToken()
            dataSource.registerFCMToken(deviceId, "token-x", localSettings)
            dataSource.updateSettings(deviceId, localSettings)
        }
        val saved = slot<FcmRegistrationRecord>()
        verify(exactly = 1) { storage.saveLastRegistration(capture(saved)) }
        assertEquals(localSettings.hashCode(), saved.captured.settingsHash)
        assertEquals("26", saved.captured.buildNumber)
        assertEquals(now, saved.captured.registeredAtMillis)
        assertEquals(64, saved.captured.tokenHash.length) // sha-256 hex — 원문 토큰은 저장하지 않음
    }

    @Test
    fun `registerFCMToken - App Check 토큰 발급 실패면 서버 호출 없이 예외 전파(재시도 워커가 받음)`() = runTest {
        every { storage.load() } returns NotificationSettings.DEFAULT
        coEvery { appCheckProbe.ensureToken() } throws AppCheckUnavailableException(
            AppCheckFailureKind.ATTESTATION_REJECTED, IllegalStateException("code: 403"),
        )

        val thrown = runCatching { repository.registerFCMToken(deviceId, "token-x") }.exceptionOrNull()

        assertEquals(true, thrown is AppCheckUnavailableException)
        coVerify(exactly = 0) { dataSource.registerFCMToken(any(), any(), any()) }
        verify(exactly = 0) { storage.saveLastRegistration(any()) }
    }

    @Test
    fun `registerFCMToken - 토큰·설정·빌드가 같고 24시간 이내면 서버 호출을 건너뛴다`() = runTest {
        val localSettings = NotificationSettings.DEFAULT
        every { storage.load() } returns localSettings
        // 같은 토큰으로 한 번 등록해 저장된 스냅샷을 그대로 재사용
        repository.registerFCMToken(deviceId, "token-x")
        val saved = slot<FcmRegistrationRecord>()
        verify { storage.saveLastRegistration(capture(saved)) }
        every { storage.loadLastRegistration() } returns saved.captured.copy(registeredAtMillis = now - 60_000L)

        repository.registerFCMToken(deviceId, "token-x")

        coVerify(exactly = 1) { dataSource.registerFCMToken(any(), any(), any()) }
        coVerify(exactly = 1) { appCheckProbe.ensureToken() }
    }

    @Test
    fun `registerFCMToken - 서버 호출이 실패하면 등록 스냅샷을 저장하지 않는다`() = runTest {
        every { storage.load() } returns NotificationSettings.DEFAULT
        coEvery { dataSource.registerFCMToken(any(), any(), any()) } throws IllegalStateException("401")

        runCatching { repository.registerFCMToken(deviceId, "token-x") }

        verify(exactly = 0) { storage.saveLastRegistration(any()) }
    }

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
    fun `초기 권한 부트스트랩 상태 - storage에 위임`() = runTest {
        every { storage.hasStoredPreference() } returns true
        every { storage.hasRequestedPermission() } returns false

        assertEquals(true, repository.hasStoredNotificationPreference())
        assertEquals(false, repository.hasRequestedNotificationPermission())

        repository.markNotificationPermissionRequested()

        verify(exactly = 1) { storage.markPermissionRequested() }
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
