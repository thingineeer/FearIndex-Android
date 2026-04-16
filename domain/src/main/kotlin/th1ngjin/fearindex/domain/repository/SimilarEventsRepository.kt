package th1ngjin.fearindex.domain.repository

import kotlinx.coroutines.flow.Flow
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.SimilarEventsResult

/**
 * SimilarEvents Repository 프로토콜.
 * iOS `SimilarEventsRepositoryProtocol`과 1:1 대응.
 */
interface SimilarEventsRepository {
    fun observe(indexType: FearIndexType): Flow<SimilarEventsResult>
}
