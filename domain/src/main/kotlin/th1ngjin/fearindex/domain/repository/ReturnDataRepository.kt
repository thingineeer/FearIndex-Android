package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.ReturnDataTable

/**
 * 과거 수익률 + 이벤트 데이터 Repository 프로토콜.
 *
 * iOS `ReturnDataRepositoryProtocol`과 1:1 대응. 구현체는 Firestore fetch →
 * 실패 시 `DefaultReturnData` fallback 순으로 처리해 **항상 유효한 테이블**을 반환해야 함.
 */
interface ReturnDataRepository {
    /**
     * 지정된 지수 타입의 수익률 테이블을 조회.
     *
     * @param indexType MARKET 또는 CRYPTO
     * @return 101점 보간 dataPoints + historicalEvents. 네트워크 실패/문서 없음이어도
     *         fallback으로 로컬 데이터 반환. 예외를 던지지 않음.
     */
    suspend fun fetch(indexType: FearIndexType): ReturnDataTable
}
