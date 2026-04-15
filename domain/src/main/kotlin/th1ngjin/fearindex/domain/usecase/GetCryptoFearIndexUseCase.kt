package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository

class GetCryptoFearIndexUseCase(private val repository: CryptoFearIndexRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): FearIndex =
        repository.fetchCurrent(forceRefresh)
}
