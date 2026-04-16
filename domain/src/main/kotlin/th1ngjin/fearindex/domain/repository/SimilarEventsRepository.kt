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

    /**
     * Firestore 문서가 아직 없는 경우 Callable Function 호출로 서버 캐시 생성을 트리거.
     * snapshot listener가 자동으로 결과를 받음.
     */
    suspend fun triggerCallable(indexType: FearIndexType, currentScore: Int)
}
