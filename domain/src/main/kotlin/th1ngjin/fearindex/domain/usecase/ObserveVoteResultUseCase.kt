package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Buy/Hold/Sell 투표 결과 실시간 스트림 UseCase
 */
class ObserveVoteResultUseCase @Inject constructor(
    private val repository: VoteRepository,
) {
    operator fun invoke(indexType: String): Flow<VoteResult> =
        repository.voteResultStream(indexType)
}
