package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.repository.FearIndexRepository

class GetFearIndexUseCase(private val repository: FearIndexRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): FearIndex =
        repository.fetchCurrent(forceRefresh)
}
