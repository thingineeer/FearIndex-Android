package th1ngjin.fearindex.core.ads

/**
 * 광고 로드 실패 시 재시도 backoff 정책 — 순수 로직.
 *
 * exponential backoff(base * 2^n)로 지연을 늘리되 [maxDelayMillis]로 상한 clamp,
 * [maxRetries] 초과 시 재시도를 중단한다. AdMob에 과도한 재요청을 던지지 않으면서
 * no-fill/네트워크 등 일시적 실패에서 회복해 "요청 대비 노출 손실"을 줄인다.
 */
class AdRetryPolicy(
    private val baseDelayMillis: Long = 2_000L,
    private val maxDelayMillis: Long = 60_000L,
    private val maxRetries: Int = 5,
) {
    /**
     * 다음 재시도까지의 지연(ms). 이미 [maxRetries]만큼 재시도했으면 null(재시도 중단).
     * @param previousRetryCount 지금까지 수행한 재시도 횟수(0 = 최초 실패 직후 첫 재시도).
     */
    fun nextDelayMillis(previousRetryCount: Int): Long? {
        if (previousRetryCount >= maxRetries) return null
        val exponential = baseDelayMillis shl previousRetryCount // base * 2^n
        return exponential.coerceAtMost(maxDelayMillis)
    }

    companion object {
        /**
         * 재시도할 가치가 있는 오류인지. INVALID_REQUEST(설정 오류)는 재요청해도 실패하므로 제외.
         * AdMob LoadAdError code: 0=INTERNAL, 1=INVALID_REQUEST, 2=NETWORK_ERROR, 3=NO_FILL.
         */
        fun isRetryable(errorCode: Int): Boolean = errorCode != CODE_INVALID_REQUEST

        private const val CODE_INVALID_REQUEST = 1
    }
}
