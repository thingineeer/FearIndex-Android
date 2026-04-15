package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.FearIndexRepository

class GetFearIndexHistoryUseCase(private val repository: FearIndexRepository) {
    suspend operator fun invoke(days: Int = 365, forceRefresh: Boolean = false): List<FearIndex> =
        repository.fetchHistory(days, forceRefresh)
}
