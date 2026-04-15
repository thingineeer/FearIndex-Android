package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository
import javax.inject.Inject

/**
 * 물림 상태 제출 UseCase
 */
class SubmitStuckStatusUseCase @Inject constructor(
    private val repository: StuckCounterRepository,
) {
    suspend operator fun invoke(
        indexType: FearIndexType,
        status: StuckStatus,
    ): StuckCounterResult = repository.submitStuckStatus(indexType, status)
}
