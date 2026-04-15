package th1ngjin.fearindex.data.dto

/**
 * 물림 상태 제출 요청 DTO (Firebase Callable 페이로드)
 */
data class SubmitStuckStatusRequest(
    val deviceId: String,
    val indexType: String,   // "market" | "crypto"
    val status: String,      // "stuck" | "safe" | "none"
) {
    fun toPayload(): Map<String, Any> = mapOf(
        "deviceId" to deviceId,
        "indexType" to indexType,
        "status" to status,
    )
}

/**
 * 물림 카운터 응답 DTO
 *
 * - stuckCount/safeCount는 서버가 100명 미만이면 0으로 마스킹해 내려보낸다.
 *   그러나 percentage 필드는 **항상 실제 비율**이다.
 */
data class StuckCounterResponse(
    val stuckCount: Int,
    val safeCount: Int,
    val totalResponded: Int,
    val stuckPercentage: Double,
    val safePercentage: Double,
    val myStatus: String?,
) {
    companion object {
        fun fromMap(data: Map<String, Any?>): StuckCounterResponse {
            return StuckCounterResponse(
                stuckCount = (data["stuckCount"] as? Number)?.toInt() ?: 0,
                safeCount = (data["safeCount"] as? Number)?.toInt() ?: 0,
                totalResponded = (data["totalResponded"] as? Number)?.toInt() ?: 0,
                stuckPercentage = (data["stuckPercentage"] as? Number)?.toDouble() ?: 0.0,
                safePercentage = (data["safePercentage"] as? Number)?.toDouble() ?: 0.0,
                myStatus = data["myStatus"] as? String,
            )
        }
    }
}
