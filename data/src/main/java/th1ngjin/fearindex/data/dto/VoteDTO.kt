package th1ngjin.fearindex.data.dto

/**
 * 투표 제출 요청 DTO (Firebase Callable 페이로드)
 */
data class SubmitVoteRequest(
    val deviceId: String,
    val indexType: String,
    val choice: String,
    val fearScore: Int,
    val date: String,
) {
    fun toPayload(): Map<String, Any> = mapOf(
        "deviceId" to deviceId,
        "indexType" to indexType,
        "choice" to choice,
        "fearScore" to fearScore,
        "date" to date,
    )
}

/**
 * 투표 결과 응답 DTO
 */
data class VoteResponse(
    val buyCount: Int,
    val holdCount: Int,
    val sellCount: Int,
    val totalCount: Int,
    val myVote: String?,
) {
    companion object {
        fun fromMap(data: Map<String, Any?>): VoteResponse {
            return VoteResponse(
                buyCount = (data["buyCount"] as? Number)?.toInt() ?: 0,
                holdCount = (data["holdCount"] as? Number)?.toInt() ?: 0,
                sellCount = (data["sellCount"] as? Number)?.toInt() ?: 0,
                totalCount = (data["totalCount"] as? Number)?.toInt() ?: 0,
                myVote = data["myVote"] as? String,
            )
        }
    }
}
