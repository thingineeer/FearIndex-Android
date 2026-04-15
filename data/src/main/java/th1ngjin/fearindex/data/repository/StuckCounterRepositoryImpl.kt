package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.StuckCounterDataSource
import th1ngjin.fearindex.data.dto.StuckCounterResponse
import th1ngjin.fearindex.data.dto.SubmitStuckStatusRequest
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 물림 카운터 Repository 구현.
 * SRP: 물림 데이터 접근 + DTO ↔ Entity 변환 + 로컬 상태 캐시.
 */
@Singleton
class StuckCounterRepositoryImpl @Inject constructor(
    private val dataSource: StuckCounterDataSource,
    private val storage: StuckCounterStorage,
) : StuckCounterRepository {

    override suspend fun submitStuckStatus(
        indexType: FearIndexType,
        status: StuckStatus,
    ): StuckCounterResult {
        val deviceId = storage.loadDeviceId()
        val request = SubmitStuckStatusRequest(
            deviceId = deviceId,
            indexType = indexType.serverValue(),
            status = status.serverValue,
        )
        val response = dataSource.submitStatus(request)
        storage.saveStatus(indexType, status)
        return response.toEntity()
    }

    override suspend fun fetchResult(indexType: FearIndexType): StuckCounterResult {
        val deviceId = storage.loadDeviceId()
        val response = dataSource.fetchResult(deviceId, indexType.serverValue())
        return response.toEntity()
    }

    override fun stuckCounterStream(indexType: FearIndexType): Flow<StuckCounterResult> {
        return dataSource.resultStream(indexType.serverValue()).map { aggregate ->
            // DataSource는 집계만 스트림. 내 상태는 로컬 캐시로 주입.
            val localStatus = loadLocalStatus(indexType)
            aggregate.copy(myStatus = localStatus.serverValue).toEntity()
        }
    }

    override fun loadLocalStatus(indexType: FearIndexType): StuckStatus =
        storage.loadStatus(indexType)

    // MARK: - Helpers

    private fun StuckCounterResponse.toEntity(): StuckCounterResult = StuckCounterResult(
        stuckCount = stuckCount.coerceAtLeast(0),
        safeCount = safeCount.coerceAtLeast(0),
        totalResponded = totalResponded.coerceAtLeast(0),
        stuckPercentage = stuckPercentage.coerceIn(0.0, 100.0),
        safePercentage = safePercentage.coerceIn(0.0, 100.0),
        myStatus = StuckStatus.fromServer(myStatus),
    )

    private fun FearIndexType.serverValue(): String = name.lowercase()
}
