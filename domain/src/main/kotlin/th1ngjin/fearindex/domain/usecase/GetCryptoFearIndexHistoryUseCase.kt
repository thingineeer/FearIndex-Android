package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository

class GetCryptoFearIndexHistoryUseCase(private val repository: CryptoFearIndexRepository) {
    suspend operator fun invoke(days: Int = 31, forceRefresh: Boolean = false): List<FearIndex> =
        repository.fetchHistory(days, forceRefresh)
}
