package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 물림 카운터 조회/구독 UseCase
 */
class ObserveStuckCounterUseCase @Inject constructor(
    private val repository: StuckCounterRepository,
) {
    suspend fun fetchOnce(indexType: FearIndexType): StuckCounterResult =
        repository.fetchResult(indexType)

    fun stream(indexType: FearIndexType): Flow<StuckCounterResult> =
        repository.stuckCounterStream(indexType)

    fun loadLocalStatus(indexType: FearIndexType): StuckStatus =
        repository.loadLocalStatus(indexType)
}
