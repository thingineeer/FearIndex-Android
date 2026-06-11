package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.ReturnDataSource
import th1ngjin.fearindex.domain.defaults.DefaultReturnData
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.repository.ReturnDataRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 수익률 데이터 Repository 구현.
 *
 * 전략 (iOS `ReturnDataRepository`와 동일):
 * 1. Firestore `returnData/{market|crypto}` fetch 시도
 * 2. 실패/문서 없음/스키마 불일치 → `DefaultReturnData` fallback
 * 3. 예외를 상위로 던지지 않음 — 항상 유효한 테이블 반환
 *
 * 프로세스 수명 내 메모리 캐시 (ConcurrentHashMap) 유지. ViewModel 재생성 시
 * Firestore 재호출 없이 즉시 반환. 데이터 갱신은 앱 재시작 시점에 반영.
 */
@Singleton
class ReturnDataRepositoryImpl @Inject constructor(
    private val dataSource: ReturnDataSource,
) : ReturnDataRepository {

    @Volatile
    private var marketCache: ReturnDataTable? = null

    @Volatile
    private var cryptoCache: ReturnDataTable? = null

    @Volatile
    private var kospiCache: ReturnDataTable? = null

    override suspend fun fetch(indexType: FearIndexType): ReturnDataTable {
        cached(indexType)?.let { return it }

        val key = indexType.serverKey()
        val table = try {
            val dto = dataSource.fetch(key)
            Timber.i("ReturnDataRepository: Firestore 로드 성공 ($key, events=${dto.historicalEvents.size})")
            dto.toDomain()
        } catch (t: Throwable) {
            Timber.w(t, "ReturnDataRepository: fallback → DefaultReturnData ($key)")
            fallback(indexType)
        }

        storeCache(indexType, table)
        return table
    }

    private fun cached(indexType: FearIndexType): ReturnDataTable? = when (indexType) {
        FearIndexType.MARKET -> marketCache
        FearIndexType.KOSPI -> kospiCache
        FearIndexType.CRYPTO -> cryptoCache
    }

    private fun storeCache(indexType: FearIndexType, table: ReturnDataTable) {
        when (indexType) {
            FearIndexType.MARKET -> marketCache = table
            FearIndexType.KOSPI -> kospiCache = table
            FearIndexType.CRYPTO -> cryptoCache = table
        }
    }

    private fun fallback(indexType: FearIndexType): ReturnDataTable = when (indexType) {
        FearIndexType.MARKET -> DefaultReturnData.market
        FearIndexType.KOSPI -> DefaultReturnData.market
        FearIndexType.CRYPTO -> DefaultReturnData.crypto
    }

    private fun FearIndexType.serverKey(): String = when (this) {
        FearIndexType.MARKET -> "market"
        FearIndexType.KOSPI -> "kospi"
        FearIndexType.CRYPTO -> "crypto"
    }
}
