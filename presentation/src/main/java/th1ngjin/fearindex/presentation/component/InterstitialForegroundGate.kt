package th1ngjin.fearindex.presentation.component

/**
 * 포그라운드 복귀 시 인터스티셜 "세션" 리셋 판정 — 순수 로직 (iOS `InterstitialForegroundGate` 1:1).
 *
 * v1.5.3 까지 `InterstitialAdPolicy.resetSession()` 호출처가 없어 세션 cap(2)과 KOSPI 진입 1회 플래그가
 * "포그라운드 세션"이 아닌 "프로세스 수명" 기준으로 누적됐다 — 기기가 프로세스를 며칠 유지하면
 * 이후 재방문에도 인터스티셜이 영구 0. (iOS v1.9.2 에서 먼저 수정, Android v1.6.1 포팅)
 */
object InterstitialForegroundGate {

    /** 새 세션으로 판정하는 최소 백그라운드 체류 시간 (GA4 세션 만료 기준 30분). */
    const val SESSION_RESET_BACKGROUND_MILLIS: Long = 30L * 60L * 1_000L

    /** 백그라운드 체류 시간이 threshold 이상이면 새 세션 → cap/쿨다운/세션 플래그 리셋. */
    fun shouldResetSession(backgroundMillis: Long): Boolean =
        backgroundMillis >= SESSION_RESET_BACKGROUND_MILLIS
}
