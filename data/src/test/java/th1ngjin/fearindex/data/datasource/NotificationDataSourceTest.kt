package th1ngjin.fearindex.data.datasource

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.NotificationSettings
import javax.inject.Provider

/**
 * NotificationDataSource — iOS Firebase Functions와의 payload 계약 검증.
 *
 * iOS `FCMService.swift` ↔ Android `NotificationDataSource.kt`는 동일 Cloud Function을 호출.
 * payload 구조가 맞지 않으면 서버에서 `INTERNAL` 반환 (bugs-fixed.md 10번 사례).
 *
 * 핵심 계약:
 * - 모든 페이로드는 `{ deviceId, settings: { ... } }` 중첩 구조.
 * - `registerFCMToken`은 `fcmToken`도 최상위에 포함.
 * - `language`는 ISO 639-1 2자리 (iOS와 동일).
 * - threshold는 클라이언트에서 0~50 / 50~100으로 클램핑.
 */
class NotificationDataSourceTest {

    private val functions = mockk<FirebaseFunctions>()
    private val callable = mockk<HttpsCallableReference>()
    private val result = mockk<HttpsCallableResult>(relaxed = true)
    private val metadataProvider = mockk<NotificationClientMetadataProvider>()
    private val dataSource = NotificationDataSource(Provider { functions }, metadataProvider)

    private val deviceId = "550e8400-e29b-41d4-a716-446655440000"

    init {
        every { metadataProvider.current() } returns NotificationClientMetadata(
            language = "ko",
            platform = "android",
            appVersion = "1.1.0",
            buildNumber = "10",
            notificationSchemaVersion = 180,
        )
    }

    private fun stubCallable(name: String): CapturingSlot<Map<String, Any>> {
        val payloadSlot = slot<Map<String, Any>>()
        every { functions.getHttpsCallable(name) } returns callable
        every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(result)
        return CapturingSlot(payloadSlot)
    }

    private class CapturingSlot<T>(val slot: io.mockk.CapturingSlot<T>) {
        val captured: T get() = slot.captured
    }

    @Test
    fun `registerFCMToken - payload는 deviceId, fcmToken, settings 전체와 metadata 포함`() = runTest {
        val captured = stubCallable("registerFCMToken")
        val token = "fake-fcm-token"
        val localSettings = NotificationSettings(
            notificationEnabled = false,
            globalNotificationEnabled = true,
            kospiNotificationEnabled = true,
            cryptoNotificationEnabled = true,
        )

        dataSource.registerFCMToken(deviceId, token, localSettings)

        val payload = captured.captured
        assertEquals(deviceId, payload["deviceId"])
        assertEquals(token, payload["fcmToken"])
        @Suppress("UNCHECKED_CAST")
        val settings = payload["settings"] as? Map<String, Any>
        assertNotNull("settings 중첩 객체가 있어야 함 (iOS 계약)", settings)
        assertEquals(false, settings?.get("notificationEnabled"))
        assertEquals(true, settings?.get("globalNotificationEnabled"))
        assertEquals(true, settings?.get("kospiNotificationEnabled"))
        assertEquals(true, settings?.get("cryptoNotificationEnabled"))
        assertEquals("ko", settings?.get("language"))
        assertEquals("android", settings?.get("platform"))
        assertEquals("1.1.0", settings?.get("appVersion"))
        assertEquals("10", settings?.get("buildNumber"))
        assertEquals(180, settings?.get("notificationSchemaVersion"))
    }

    @Test
    fun `updateSettings - settings 중첩 객체에 모든 필수 키 포함`() = runTest {
        val captured = stubCallable("updateNotificationSettings")

        val settings = NotificationSettings(
            notificationEnabled = true,
            globalNotificationEnabled = false,
            marketLowerThreshold = 25,
            marketUpperThreshold = 75,
            kospiNotificationEnabled = true,
            kospiLowerThreshold = 30,
            kospiUpperThreshold = 70,
            cryptoNotificationEnabled = false,
            cryptoLowerThreshold = 20,
            cryptoUpperThreshold = 80,
            weeklyReportNotificationEnabled = false,
        )
        dataSource.updateSettings(deviceId, settings)

        val payload = captured.captured
        assertEquals(deviceId, payload["deviceId"])
        @Suppress("UNCHECKED_CAST")
        val s = payload["settings"] as Map<String, Any>
        assertEquals(true, s["notificationEnabled"])
        assertEquals(false, s["globalNotificationEnabled"])
        assertEquals(25, s["lowerThreshold"])
        assertEquals(75, s["upperThreshold"])
        assertEquals(true, s["kospiNotificationEnabled"])
        assertEquals(30, s["kospiLowerThreshold"])
        assertEquals(70, s["kospiUpperThreshold"])
        assertEquals(false, s["cryptoNotificationEnabled"])
        assertEquals(20, s["cryptoLowerThreshold"])
        assertEquals(80, s["cryptoUpperThreshold"])
        assertEquals(false, s["weeklyReportNotificationEnabled"])
        assertEquals("ko", s["language"])
        assertEquals("android", s["platform"])
        assertEquals("1.1.0", s["appVersion"])
        assertEquals("10", s["buildNumber"])
        assertEquals(180, s["notificationSchemaVersion"])
    }

    @Test
    fun `updateSettings - lowerThreshold가 0_50 범위 밖이면 클램핑`() = runTest {
        val captured = stubCallable("updateNotificationSettings")

        dataSource.updateSettings(
            deviceId,
            NotificationSettings(
                notificationEnabled = true,
                marketLowerThreshold = 99, // 50 초과 → 50으로 클램핑
                marketUpperThreshold = 60,
                kospiLowerThreshold = -8,
                kospiUpperThreshold = 60,
                cryptoLowerThreshold = -10, // 0 미만 → 0으로 클램핑
                cryptoUpperThreshold = 60,
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val s = captured.captured["settings"] as Map<String, Any>
        assertEquals(50, s["lowerThreshold"])
        assertEquals(0, s["kospiLowerThreshold"])
        assertEquals(0, s["cryptoLowerThreshold"])
    }

    @Test
    fun `updateSettings - upperThreshold가 50_100 범위 밖이면 클램핑`() = runTest {
        val captured = stubCallable("updateNotificationSettings")

        dataSource.updateSettings(
            deviceId,
            NotificationSettings(
                notificationEnabled = true,
                marketLowerThreshold = 30,
                marketUpperThreshold = 200, // 100 초과 → 100
                kospiLowerThreshold = 30,
                kospiUpperThreshold = 10, // 51 미만 → 51
                cryptoLowerThreshold = 30,
                cryptoUpperThreshold = 10, // 51 미만 → 51
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val s = captured.captured["settings"] as Map<String, Any>
        assertEquals(100, s["upperThreshold"])
        assertEquals(51, s["kospiUpperThreshold"])
        assertEquals(51, s["cryptoUpperThreshold"])
    }

    @Test
    fun `unregisterDevice - payload는 deviceId만 포함`() = runTest {
        val captured = stubCallable("unregisterDevice")

        dataSource.unregisterDevice(deviceId)

        val payload = captured.captured
        assertEquals(deviceId, payload["deviceId"])
        assertEquals("deviceId 외 다른 키가 없어야 함", 1, payload.size)
    }

    @Test
    fun `함수 이름 - iOS 계약과 동일`() = runTest {
        // 방어적 테스트: 함수 이름이 바뀌면 서버 호출이 즉시 깨짐
        stubCallable("registerFCMToken")
        dataSource.registerFCMToken(deviceId, "t", NotificationSettings.DEFAULT)
        verify { functions.getHttpsCallable("registerFCMToken") }

        stubCallable("updateNotificationSettings")
        dataSource.updateSettings(deviceId, NotificationSettings(notificationEnabled = true))
        verify { functions.getHttpsCallable("updateNotificationSettings") }

        stubCallable("unregisterDevice")
        dataSource.unregisterDevice(deviceId)
        verify { functions.getHttpsCallable("unregisterDevice") }
    }
}
