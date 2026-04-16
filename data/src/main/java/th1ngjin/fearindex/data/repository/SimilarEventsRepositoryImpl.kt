package th1ngjin.fearindex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
) : SimilarEventsRepository {

    override fun observe(indexType: FearIndexType): Flow<SimilarEventsResult> = callbackFlow {
        val docId = "similarEvents_${indexType.name.lowercase()}"
        val registration = firestore
            .collection("insights")
            .document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.w(error, "SimilarEventsRepository: observe error ($docId)")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val data = snapshot.data ?: return@addSnapshotListener
                val result = parseResult(data, indexType)
                if (result != null) {
                    trySend(result)
                } else {
                    Timber.w("SimilarEventsRepository: parse failed ($docId)")
                }
            }

        awaitClose { registration.remove() }
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
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAggregateStats(raw: Map<String, Any?>): AggregateStats? {
        val score = (raw["score"] as? Number)?.toInt() ?: return null
        val sampleCount = (raw["sampleCount"] as? Number)?.toInt() ?: return null
        if (sampleCount <= 0) return null
        val avgReturnRaw = raw["avgReturn"] as? Map<String, Any?> ?: return null
        return AggregateStats(
            score = score,
            sampleCount = sampleCount,
            avgReturn = HistoricalReturns(
                oneMonth = (avgReturnRaw["oneMonth"] as? Number)?.toDouble() ?: 0.0,
                threeMonth = (avgReturnRaw["threeMonth"] as? Number)?.toDouble() ?: 0.0,
                sixMonth = (avgReturnRaw["sixMonth"] as? Number)?.toDouble() ?: 0.0,
                oneYear = (avgReturnRaw["oneYear"] as? Number)?.toDouble() ?: 0.0,
            ),
        )
    }
}
