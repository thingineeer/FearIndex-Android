package th1ngjin.fearindex.data.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import th1ngjin.fearindex.data.dto.SubmitVoteRequest
import th1ngjin.fearindex.data.dto.VoteResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Functions + Firestore 기반 Buy/Hold/Sell 투표 DataSource.
 *
 * - submitVote / getVoteResult: Cloud Function (Callable)
 * - voteResultStream: Firestore snapshot listener
 */
@Singleton
class VoteDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
) {

    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    suspend fun submitVote(request: SubmitVoteRequest): VoteResponse {
        val result = functions
            .getHttpsCallable("submitVote")
            .call(request.toPayload())
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("submitVote: 응답 파싱 실패")
        return VoteResponse.fromMap(data)
    }

    suspend fun getVoteResult(
        deviceId: String,
        indexType: String,
        date: String,
    ): VoteResponse {
        val payload = mapOf(
            "deviceId" to deviceId,
            "indexType" to indexType,
            "date" to date,
        )
        val result = functions
            .getHttpsCallable("getVoteResult")
            .call(payload)
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("getVoteResult: 응답 파싱 실패")
        return VoteResponse.fromMap(data)
    }

    /**
     * 오늘 투표 결과를 실시간 스트리밍.
     * Firestore path: votes/{todayUTC}/results/{indexType}
     */
    fun voteResultStream(indexType: String): Flow<VoteResponse> = callbackFlow {
        listeners.remove(indexType)?.remove()

        val today = todayUTC()
        val docRef = firestore.document("votes/$today/results/$indexType")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "[VoteDataSource] Snapshot error ($indexType)")
                close(error)
                return@addSnapshotListener
            }
            val data = snapshot?.data ?: emptyMap()
            val response = VoteResponse(
                buyCount = (data["buyCount"] as? Number)?.toInt() ?: 0,
                holdCount = (data["holdCount"] as? Number)?.toInt() ?: 0,
                sellCount = (data["sellCount"] as? Number)?.toInt() ?: 0,
                totalCount = (data["totalCount"] as? Number)?.toInt() ?: 0,
                myVote = null, // snapshot에서는 myVote 알 수 없음 — 로컬 캐시로 보완
            )
            trySend(response)
        }
        listeners[indexType] = registration

        awaitClose {
            listeners.remove(indexType)?.remove()
        }
    }

    companion object {
        fun todayUTC(): String = ZonedDateTime
            .now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
}
