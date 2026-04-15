package th1ngjin.fearindex.domain.service

/**
 * 디바이스 고유 ID 제공자.
 *
 * 서버(Firestore, Cloud Functions)에서 사용자를 식별하는 UUID.
 * 구현체는 data 레이어의 StuckCounterStorage가 제공.
 */
interface DeviceIdProvider {
    fun loadDeviceId(): String
}
