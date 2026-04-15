package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.repository.VoteRepository
import javax.inject.Inject

/**
 * Buy/Hold/Sell 투표 결과 조회 UseCase (1회)
 */
class GetVoteResultUseCase @Inject constructor(
    private val repository: VoteRepository,
) {
    suspend operator fun invoke(indexType: String): VoteResult =
        repository.getVoteResult(indexType)
}
