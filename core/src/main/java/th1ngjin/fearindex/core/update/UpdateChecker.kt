package th1ngjin.fearindex.core.update

/**
 * Remote Config 버전 기준으로 업데이트 필요 여부를 판정하는 순수 로직.
 *
 * iOS `RemoteConfigManager.checkForUpdate()` 와 동일한 규칙을 사용한다.
 * - 강제 업데이트: major.minor 가 force 기준보다 낮을 때만 (1.0.x → 1.1)
 *   → Remote Config `force_update_minimum_version` 만 수정하면 자동 트리거
 * - 선택 업데이트: 전체 버전이 minimum 보다 낮을 때 (1.1.0 → 1.1.1)
 *
 * 버전 비교는 사전순이 아니라 수치 기준(`1.10` > `1.9`)으로 한다.
 */
object UpdateChecker {

    fun evaluate(
        currentVersion: String,
        forceUpdateMinimumVersion: String,
        minimumAppVersion: String,
    ): UpdateStatus {
        val current = parse(currentVersion) ?: return UpdateStatus.UP_TO_DATE

        // major.minor 비교로 강제 업데이트 체크
        val force = parse(forceUpdateMinimumVersion)
        if (force != null && compareMajorMinor(current, force) < 0) {
            return UpdateStatus.FORCE_UPDATE_REQUIRED
        }

        // 전체 버전 비교로 선택적 업데이트 체크
        val minimum = parse(minimumAppVersion)
        if (minimum != null && compareVersions(current, minimum) < 0) {
            return UpdateStatus.UPDATE_AVAILABLE
        }

        return UpdateStatus.UP_TO_DATE
    }

    /** "1.0.1" → [1, 0, 1]. 숫자가 아닌 컴포넌트가 섞이거나 비어 있으면 null. */
    private fun parse(version: String): List<Int>? {
        if (version.isBlank()) return null
        val parts = version.trim().split(".")
        val numbers = parts.mapNotNull { it.toIntOrNull() }
        return numbers.takeIf { it.isNotEmpty() && it.size == parts.size }
    }

    /** major.minor 만 비교 (세 번째 이후 컴포넌트 무시). */
    private fun compareMajorMinor(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until 2) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    /** 전체 버전 수치 비교. */
    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }
}
