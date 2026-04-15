package th1ngjin.fearindex.data.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import th1ngjin.fearindex.data.dto.StuckCounterResponse
import th1ngjin.fearindex.data.dto.SubmitStuckStatusRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Functions + Firestore 기반 물림 카운터 DataSource.
 *
 * SRP: 물림 API 호출 + 실시간 스트림.
 *
 * - 전역 집계는 `stuckStatus/global_{indexType}` 문서를 snapshot listener로 구독.
 * - myStatus는 보안 규칙상 클라이언트가 직접 읽지 않으므로 Repository 레이어가
 *   로컬 캐시(SharedPreferences)로 보완한다.
 */
@Singleton
class StuckCounterDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
) {

    /** indexType별 listener 맵 — market/crypto 동시 구독 지원. */
    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    suspend fun submitStatus(request: SubmitStuckStatusRequest): StuckCounterResponse {
        val result = functions
            .getHttpsCallable("submitStuckStatus")
            .call(request.toPayload())
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("submitStuckStatus: 응답 파싱 실패")
        return StuckCounterResponse.fromMap(data)
    }

    suspend fun fetchResult(deviceId: String, indexType: String): StuckCounterResponse {
        val payload = mapOf(
            "deviceId" to deviceId,
            "indexType" to indexType,
        )
        val result = functions
            .getHttpsCallable("getStuckCount")
            .call(payload)
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("getStuckCount: 응답 파싱 실패")
        return StuckCounterResponse.fromMap(data)
    }

    /**
     * 전역 집계만 스트리밍한다. myStatus는 Repository 레이어가 로컬 캐시로 채운다.
     * indexType별로 독립 listener를 유지하여 market + crypto 동시 구독을 지원.
     */
    fun resultStream(indexType: String): Flow<StuckCounterResponse> = callbackFlow {
        // 이미 같은 indexType listener가 살아있으면 제거 후 재등록
        listeners.remove(indexType)?.remove()

        val docRef = firestore.document("stuckStatus/global_$indexType")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "[StuckCounterDataSource] Snapshot error ($indexType)")
                close(error)
                return@addSnapshotListener
            }
            val data = snapshot?.data ?: emptyMap()
            val response = StuckCounterResponse(
                stuckCount = (data["stuckCount"] as? Number)?.toInt() ?: 0,
                safeCount = (data["safeCount"] as? Number)?.toInt() ?: 0,
                totalResponded = (data["totalResponded"] as? Number)?.toInt() ?: 0,
                stuckPercentage = (data["stuckPercentage"] as? Number)?.toDouble() ?: 0.0,
                safePercentage = (data["safePercentage"] as? Number)?.toDouble() ?: 0.0,
                myStatus = null,
            )
            trySend(response)
        }
        listeners[indexType] = registration

        awaitClose {
            listeners.remove(indexType)?.remove()
        }
    }

    fun disconnectStream(indexType: String) {
        listeners.remove(indexType)?.remove()
    }

    fun disconnectAllStreams() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
