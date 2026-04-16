package th1ngjin.fearindex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import th1ngjin.fearindex.domain.entity.AggregateStats
import th1ngjin.fearindex.domain.entity.EventMatch
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.OptionalReturns
import th1ngjin.fearindex.domain.entity.Similarity
import th1ngjin.fearindex.domain.entity.SimilarEventsResult
import th1ngjin.fearindex.domain.repository.SimilarEventsRepository
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore `insights/similarEvents_{indexType}` 실시간 구독 Repository.
 *
 * iOS `SimilarEventsRepository`와 동일 구조: Firestore snapshot listener로
 * 서버가 갱신할 때마다 자동 반영.
 */
@Singleton
class SimilarEventsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : SimilarEventsRepository {

    private val triggeredScores = mutableSetOf<String>()

    override fun observe(indexType: FearIndexType): Flow<SimilarEventsResult> = callbackFlow {
        val docId = "similarEvents_${indexType.name.lowercase()}"
        Timber.i("SimilarEventsRepository: observe START ($docId)")
        val registration = firestore
            .collection("insights")
            .document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "SimilarEventsRepository: observe error ($docId)")
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Timber.w("SimilarEventsRepository: snapshot null ($docId)")
                    return@addSnapshotListener
                }
                if (!snapshot.exists()) {
                    Timber.w("SimilarEventsRepository: doc does not exist ($docId), will trigger Callable fallback")
                    return@addSnapshotListener
                }

                val data = snapshot.data
                if (data == null) {
                    Timber.w("SimilarEventsRepository: data null ($docId)")
                    return@addSnapshotListener
                }
                Timber.i("SimilarEventsRepository: snapshot received ($docId, fields=${data.keys})")
                val result = parseResult(data, indexType)
                if (result != null) {
                    Timber.i("SimilarEventsRepository: parsed OK ($docId, matches=${result.matches.size}, hasStats=${result.aggregateStats != null})")
                    trySend(result)
                } else {
                    Timber.w("SimilarEventsRepository: parse failed ($docId)")
                }
            }

        awaitClose {
            Timber.i("SimilarEventsRepository: observe END ($docId)")
            registration.remove()
        }
    }

    /**
     * Firestore 문서가 존재하지 않을 때 Callable Function `getSimilarEvents`를 호출해
     * 서버에 문서를 즉시 생성/캐시한다. 이후 snapshot listener가 자동으로 받음.
     *
     * iOS는 Firestore Trigger(`fearIndex/latest` 변경)에 의존하지만,
     * Android는 첫 진입 시 fearIndex가 아직 변경되지 않았을 수 있으므로 명시적으로 트리거.
     */
    override suspend fun triggerCallable(indexType: FearIndexType, currentScore: Int) {
        val key = "${indexType.name.lowercase()}_$currentScore"
        if (key in triggeredScores) return
        triggeredScores.add(key)

        try {
            Timber.i("SimilarEventsRepository: Callable trigger ($key)")
            val payload = mapOf(
                "indexType" to indexType.name.lowercase(),
                "currentScore" to currentScore,
            )
            functions
                .getHttpsCallable("getSimilarEvents")
                .call(payload)
                .await()
            Timber.i("SimilarEventsRepository: Callable success ($key)")
        } catch (t: Throwable) {
            Timber.w(t, "SimilarEventsRepository: Callable failed ($key)")
            triggeredScores.remove(key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseResult(
        raw: Map<String, Any?>,
        indexType: FearIndexType,
    ): SimilarEventsResult? {
        val currentScore = (raw["currentScore"] as? Number)?.toInt() ?: return null
        val updatedAt = (raw["updatedAt"] as? com.google.firebase.Timestamp)
            ?.let { Instant.ofEpochSecond(it.seconds) }
            ?: Instant.now()

        val matchesRaw = raw["matches"] as? List<*> ?: emptyList<Any>()
        val matches = matchesRaw.mapNotNull { entry ->
            (entry as? Map<String, Any?>)?.let { parseMatch(it) }
        }

        val statsRaw = raw["aggregateStats"] as? Map<String, Any?>
        val stats = statsRaw?.let { parseAggregateStats(it) }

        return SimilarEventsResult(
            indexType = indexType,
            currentScore = currentScore,
            updatedAt = updatedAt,
            matches = matches,
            aggregateStats = stats,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMatch(raw: Map<String, Any?>): EventMatch? {
        val eventId = raw["eventId"] as? String ?: return null
        val score = (raw["score"] as? Number)?.toInt() ?: return null
        val distance = (raw["distance"] as? Number)?.toInt() ?: return null
        val similarity = (raw["similarity"] as? String)?.let { Similarity.fromString(it) } ?: Similarity.FAR
        val date = raw["date"] as? String ?: ""
        val titleKey = raw["titleKey"] as? String ?: eventId
        val descriptionKey = raw["descriptionKey"] as? String ?: titleKey
        val returnAfterRaw = raw["returnAfter"] as? Map<String, Any?> ?: emptyMap()
        val isOngoing = raw["isOngoing"] as? Boolean ?: false
        val isPinned = raw["isPinned"] as? Boolean ?: false

        return EventMatch(
            eventId = eventId,
            score = score,
            distance = distance,
            similarity = similarity,
            date = date,
            titleKey = titleKey,
            descriptionKey = descriptionKey,
            returnAfter = OptionalReturns(
                oneMonth = (returnAfterRaw["oneMonth"] as? Number)?.toDouble(),
                threeMonth = (returnAfterRaw["threeMonth"] as? Number)?.toDouble(),
                sixMonth = (returnAfterRaw["sixMonth"] as? Number)?.toDouble(),
                oneYear = (returnAfterRaw["oneYear"] as? Number)?.toDouble(),
            ),
            isOngoing = isOngoing,
            isPinned = isPinned,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAggregateStats(raw: Map<String, Any?>): AggregateStats? {
        val score = (raw["score"] as? Number)?.toInt() ?: return null
        val sampleCount = (raw["sampleCount"] as? Number)?.toInt() ?: return null
        if (sampleCount <= 0) return null
        val avgReturnRaw = raw["avgReturn"] as? Map<String, Any?> ?: return null
        val maxDrawdownRaw = raw["maxDrawdown"] as? Map<String, Any?>
        val bestReturnRaw = raw["bestReturn"] as? Map<String, Any?>
        return AggregateStats(
            score = score,
            sampleCount = sampleCount,
            avgReturn = parseReturns(avgReturnRaw),
            maxDrawdown = maxDrawdownRaw?.let { parseReturns(it) },
            bestReturn = bestReturnRaw?.let { parseReturns(it) },
        )
    }

    private fun parseReturns(raw: Map<String, Any?>): HistoricalReturns = HistoricalReturns(
        oneMonth = (raw["oneMonth"] as? Number)?.toDouble() ?: 0.0,
        threeMonth = (raw["threeMonth"] as? Number)?.toDouble() ?: 0.0,
        sixMonth = (raw["sixMonth"] as? Number)?.toDouble() ?: 0.0,
        oneYear = (raw["oneYear"] as? Number)?.toDouble() ?: 0.0,
    )
}
