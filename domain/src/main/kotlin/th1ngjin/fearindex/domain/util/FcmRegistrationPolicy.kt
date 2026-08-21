package th1ngjin.fearindex.domain.util

import th1ngjin.fearindex.domain.entity.FcmRegistrationRecord

/**
 * FCM 토큰 서버 등록을 언제 다시 하는지 결정하는 순수 정책.
 *
 * 등록은 프로세스가 뜰 때마다(위젯 워커·FCM 수신 등 백그라운드 기동 포함) 시도되는데,
 * 토큰·설정·빌드가 그대로면 서버 상태도 그대로라 호출 자체가 낭비다(Play Integrity 쿼터 소모 +
 * 실패 시 401 노이즈). 단 서버 메타(appVersion/language 등) 갱신을 위해 하루 1회는 재등록한다.
 */
object FcmRegistrationPolicy {
    const val REFRESH_INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000

    fun shouldRegister(
        last: FcmRegistrationRecord?,
        tokenHash: String,
        settingsHash: Int,
        buildNumber: String,
        nowMillis: Long,
    ): Boolean {
        if (last == null) return true
        if (last.tokenHash != tokenHash) return true
        if (last.settingsHash != settingsHash) return true
        if (last.buildNumber != buildNumber) return true
        val elapsed = nowMillis - last.registeredAtMillis
        // 시계가 과거로 튀어 elapsed 가 음수면 "방금 등록"으로 취급 — 불필요 재등록 방지
        return elapsed >= REFRESH_INTERVAL_MILLIS
    }
}
