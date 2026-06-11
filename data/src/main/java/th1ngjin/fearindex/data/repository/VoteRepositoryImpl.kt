package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.VoteDataSource
import th1ngjin.fearindex.data.dto.SubmitVoteRequest
import th1ngjin.fearindex.data.dto.VoteResponse
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.data.storage.VoteStorage
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Buy/Hold/Sell 투표 Repository 구현.
 * DTO <-> Entity 변환 + 로컬 캐시 + deviceId 관리.
 */
@Singleton
class VoteRepositoryImpl @Inject constructor(
    private val dataSource: VoteDataSource,
    private val voteStorage: VoteStorage,
    private val deviceStorage: StuckCounterStorage,
) : VoteRepository {

    override suspend fun submitVote(
        indexType: String,
        choice: VoteChoice,
        fearScore: Int,
    ): VoteResult {
        if (ScreenshotMode.isEnabled()) return VoteResult.EMPTY

        val deviceId = deviceStorage.loadDeviceId()
        val date = VoteDataSource.todayUTC()
        val request = SubmitVoteRequest(
            deviceId = deviceId,
            indexType = indexType,
            choice = choice.value,
            fearScore = fearScore,
            date = date,
        )
        val response = dataSource.submitVote(request)
        voteStorage.saveMyVote(indexType, choice.value)
        return response.toEntity()
    }

    override suspend fun getVoteResult(indexType: String): VoteResult {
        if (ScreenshotMode.isEnabled()) return VoteResult.EMPTY

        val deviceId = deviceStorage.loadDeviceId()
        val date = VoteDataSource.todayUTC()
        val response = dataSource.getVoteResult(deviceId, indexType, date)
        response.myVote?.let { voteStorage.saveMyVote(indexType, it) }
        return response.toEntity()
    }

    override fun voteResultStream(indexType: String): Flow<VoteResult> {
        if (ScreenshotMode.isEnabled()) return flowOf(VoteResult.EMPTY)

        return dataSource.voteResultStream(indexType).map { response ->
            val localMyVote = voteStorage.loadMyVote(indexType)
            response.copy(myVote = localMyVote).toEntity()
        }
    }

    override fun hasVotedToday(indexType: String): Boolean =
        voteStorage.hasVotedToday(indexType)

    override fun loadMyVote(indexType: String): VoteChoice? =
        VoteChoice.fromServer(voteStorage.loadMyVote(indexType))

    private fun VoteResponse.toEntity(): VoteResult = VoteResult(
        buyCount = buyCount.coerceAtLeast(0),
        holdCount = holdCount.coerceAtLeast(0),
        sellCount = sellCount.coerceAtLeast(0),
        totalCount = totalCount.coerceAtLeast(0),
        myVote = VoteChoice.fromServer(myVote),
    )
}
