package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.repository.VoteRepository
import javax.inject.Inject

/**
 * Buy/Hold/Sell 투표 제출 UseCase
 */
class SubmitVoteUseCase @Inject constructor(
    private val repository: VoteRepository,
) {
    suspend operator fun invoke(
        indexType: String,
        choice: VoteChoice,
        fearScore: Int,
    ): VoteResult = repository.submitVote(indexType, choice, fearScore)
}
