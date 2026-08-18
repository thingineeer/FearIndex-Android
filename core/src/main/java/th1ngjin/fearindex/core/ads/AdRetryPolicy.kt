package th1ngjin.fearindex.core.ads

/**
 * 광고 로드 실패 시 재시도 backoff 정책 — 순수 로직. iOS `AdBannerView` 스펙 1:1 포팅.
 *
 * 초기 [retryDelaysMillis]([5s, 15s, 45s])로 3회 재시도한 뒤, 그래도 실패하면
 * [finalRetryDelayMillis](300s = 5분)로 최종 1회만 더 시도하고 중단한다. AdMob에 과도한
 * 재요청을 던지지 않으면서 no-fill/네트워크 등 일시적 실패에서 회복해 노출 손실을 줄인다.
 */
class AdRetryPolicy(
    private val retryDelaysMillis: List<Long> = listOf(5_000L, 15_000L, 45_000L),
    private val finalRetryDelayMillis: Long = 300_000L,
) {
    /**
     * 다음 재시도까지의 지연(ms). 모든 재시도(초기 배열 + 최종 1회)를 소진했으면 null(중단).
     * @param previousRetryCount 지금까지 수행한 재시도 횟수(0 = 최초 실패 직후 첫 재시도).
     */
    fun nextDelayMillis(previousRetryCount: Int): Long? = when {
        previousRetryCount < retryDelaysMillis.size -> retryDelaysMillis[previousRetryCount]
        previousRetryCount == retryDelaysMillis.size -> finalRetryDelayMillis
        else -> null
    }

    companion object {
        /**
         * 재시도할 가치가 있는 오류인지 — GMA Next-Gen `LoadAdError.ErrorCode.name` 기준(SDK 비의존).
         * 설정 오류(INVALID_REQUEST: 잘못된 단위 ID / APP_ID_MISSING)와 퍼블리셔 취소(CANCELLED —
         * AdView.destroy() 등 우리가 끊은 요청)는 재요청 무의미라 제외.
         * NO_FILL / NETWORK_ERROR / TIMEOUT / INTERNAL_ERROR 등 일시적 실패와 미지 코드는 재시도.
         */
        fun isRetryable(errorCodeName: String): Boolean = errorCodeName !in NON_RETRYABLE_CODES

        private val NON_RETRYABLE_CODES = setOf("INVALID_REQUEST", "APP_ID_MISSING", "CANCELLED")
    }
}
