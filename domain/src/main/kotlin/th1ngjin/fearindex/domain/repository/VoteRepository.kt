package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult
import kotlinx.coroutines.flow.Flow

/**
 * Buy/Hold/Sell 투표 Repository 프로토콜
 *
 * deviceId는 구현체(Data 레이어)가 내부적으로 관리한다.
 */
interface VoteRepository {
    /** 투표 제출 */
    suspend fun submitVote(
        indexType: String,
        choice: VoteChoice,
        fearScore: Int,
    ): VoteResult

    /** 오늘 투표 결과 조회 (1회) */
    suspend fun getVoteResult(indexType: String): VoteResult

    /** 오늘 투표 결과 실시간 스트림 (Firestore snapshot listener) */
    fun voteResultStream(indexType: String): Flow<VoteResult>

    /** 오늘 이미 투표했는지 확인 (로컬 캐시) */
    fun hasVotedToday(indexType: String): Boolean

    /** 로컬에 캐시된 내 투표 선택지 */
    fun loadMyVote(indexType: String): VoteChoice?
}
