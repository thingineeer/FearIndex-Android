package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.core.appcheck.AppCheckTokenProbe
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.NotificationClientMetadataProvider
import th1ngjin.fearindex.data.datasource.NotificationDataSource
import th1ngjin.fearindex.data.storage.NotificationStorage
import th1ngjin.fearindex.domain.entity.FcmRegistrationRecord
import th1ngjin.fearindex.domain.entity.NotificationSettings
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.util.FcmRegistrationPolicy
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationRepository 구현체.
 *
 * - 서버 호출(Cloud Functions)과 로컬 캐시(SharedPreferences)를 조합.
 * - updateSettings: 로컬 먼저 저장 → 서버 동기화 (optimistic).
 * - getSettings: 로컬 캐시 반환 (서버 조회 없음 — 서버는 write-only).
 * - registerFCMToken: [FcmRegistrationPolicy] 로 불필요한 재등록을 건너뛰고, App Check 토큰을
 *   선취득해 확정 401 호출을 보내지 않는다(실패 사유는 프로브가 로그로 남김).
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationDataSource,
    private val storage: NotificationStorage,
    private val metadataProvider: NotificationClientMetadataProvider,
    private val appCheckProbe: AppCheckTokenProbe,
) : NotificationRepository {

    /** 테스트에서 시계를 고정할 수 있도록 분리 (Hilt 생성자는 위 4개만 주입). */
    internal var clock: () -> Long = System::currentTimeMillis

    override suspend fun registerFCMToken(deviceId: String, fcmToken: String) {
        if (ScreenshotMode.isEnabled()) return
        val settings = storage.load()
        val buildNumber = metadataProvider.current().buildNumber
        val tokenHash = sha256(fcmToken)
        val shouldRegister = FcmRegistrationPolicy.shouldRegister(
            last = storage.loadLastRegistration(),
            tokenHash = tokenHash,
            settingsHash = settings.hashCode(),
            buildNumber = buildNumber,
            nowMillis = clock(),
        )
        if (!shouldRegister) {
            Timber.d("FCM registration skipped — unchanged token/settings/build within refresh window")
            return
        }
        appCheckProbe.ensureToken()
        dataSource.registerFCMToken(deviceId, fcmToken, settings)
        // 서버 registerFCMToken은 payload에 임계값이 없으면 신규 기기의 즉시 알림 체크를
        // 보류하므로, 등록 직후 updateNotificationSettings가 즉시 체크를 트리거한다.
        // 등록 네트워크 왕복 중 초기 권한 허용이 toggle을 켰을 수 있어 최신 스냅샷을 재조회.
        val latest = storage.load()
        dataSource.updateSettings(deviceId, latest)
        storage.saveLastRegistration(
            FcmRegistrationRecord(
                tokenHash = tokenHash,
                settingsHash = latest.hashCode(),
                buildNumber = buildNumber,
                registeredAtMillis = clock(),
            ),
        )
    }

    override suspend fun updateSettings(deviceId: String, settings: NotificationSettings) {
        storage.save(settings)
        if (ScreenshotMode.isEnabled()) return
        dataSource.updateSettings(
            deviceId = deviceId,
            settings = settings,
        )
    }

    override suspend fun unregisterDevice(deviceId: String) {
        if (ScreenshotMode.isEnabled()) return
        dataSource.unregisterDevice(deviceId)
    }

    override suspend fun getSettings(deviceId: String): NotificationSettings {
        return storage.load()
    }

    override suspend fun saveSettingsLocal(settings: NotificationSettings) {
        storage.save(settings)
    }

    override suspend fun loadSettingsLocal(): NotificationSettings {
        return storage.load()
    }

    override suspend fun hasStoredNotificationPreference(): Boolean {
        return storage.hasStoredPreference()
    }

    override suspend fun hasRequestedNotificationPermission(): Boolean {
        return storage.hasRequestedPermission()
    }

    override suspend fun markNotificationPermissionRequested() {
        storage.markPermissionRequested()
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
