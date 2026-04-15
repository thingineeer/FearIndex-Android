package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import kotlinx.coroutines.flow.Flow

/**
 * 물림 카운터 Repository 프로토콜
 */
interface StuckCounterRepository {
    /** 물림 상태 제출 (서버 트랜잭션) */
    suspend fun submitStuckStatus(indexType: FearIndexType, status: StuckStatus): StuckCounterResult

    /** 현재 집계 조회 (1회) */
    suspend fun fetchResult(indexType: FearIndexType): StuckCounterResult

    /** 실시간 스트림 (Firestore snapshot listener) */
    fun stuckCounterStream(indexType: FearIndexType): Flow<StuckCounterResult>

    /** 로컬에 저장된 마지막 사용자 상태 조회 */
    fun loadLocalStatus(indexType: FearIndexType): StuckStatus
}
