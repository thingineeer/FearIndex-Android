package th1ngjin.fearindex.data.datasource

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import th1ngjin.fearindex.data.dto.ReturnDataDTO
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore `returnData/{indexType}` 문서 DataSource.
 *
 * iOS `ReturnDataSource`와 동일 구조 (문서 경로 · 캐시 TTL · 실패 시 throws).
 *
 * 캐싱은 Firestore SDK의 디스크 캐시에 위임 (snapshot source `CACHE` fallback).
 * Repository 레이어가 예외 → fallback 처리하므로 여기서는 throws.
 */
@Singleton
class ReturnDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    /**
     * @param indexType "market", "kospi" 또는 "crypto"
     * @throws ReturnDataSourceException 문서 없음 / 스키마 불일치 / 네트워크 실패
     */
    suspend fun fetch(indexType: String): ReturnDataDTO {
        require(indexType == "market" || indexType == "kospi" || indexType == "crypto") {
            "지원되지 않는 indexType: $indexType"
        }

        val snapshot = try {
            firestore.collection("returnData")
                .document(indexType)
                .get()
                .await()
        } catch (t: Throwable) {
            Timber.w(t, "ReturnDataSource: Firestore fetch 실패 ($indexType)")
            throw ReturnDataSourceException.Network(t)
        }

        if (!snapshot.exists()) {
            Timber.w("ReturnDataSource: 문서 없음 (returnData/$indexType)")
            throw ReturnDataSourceException.NoData
        }

        val raw = snapshot.data ?: throw ReturnDataSourceException.NoData
        return ReturnDataDTO.fromMap(raw)
            ?: throw ReturnDataSourceException.SchemaMismatch(raw.keys.toList())
    }
}

sealed class ReturnDataSourceException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause) {
    object NoData : ReturnDataSourceException("returnData 문서 없음")
    class SchemaMismatch(val keys: List<String>) :
        ReturnDataSourceException("스키마 불일치. 발견된 필드: $keys")
    class Network(cause: Throwable) :
        ReturnDataSourceException("네트워크 실패: ${cause.message}", cause)
}
